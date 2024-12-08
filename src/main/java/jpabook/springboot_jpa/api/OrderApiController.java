package jpabook.springboot_jpa.api;

import jpabook.springboot_jpa.domain.Address;
import jpabook.springboot_jpa.domain.Order;
import jpabook.springboot_jpa.domain.OrderItem;
import jpabook.springboot_jpa.domain.OrderStatus;
import jpabook.springboot_jpa.repository.OrderRepository;
import jpabook.springboot_jpa.repository.OrderSearch;
import jpabook.springboot_jpa.repository.order.query.OrderFlatDto;
import jpabook.springboot_jpa.repository.order.query.OrderItemQueryDto;
import jpabook.springboot_jpa.repository.order.query.OrderQueryDto;
import jpabook.springboot_jpa.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * V1 엔티티가 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 * <p>
 * V2 엔티티를 조회해서 DTO로 변환 (fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 * <p>
 * V3 엔티티를 조회해서 DTO로 변환 (fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야 함
 * - 대신 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능
 * <p>
 * V4 JPA 에서 DTO 로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 * <p>
 * V5 JPA 에서 DTO 로 바로 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 * <p>
 * V6 JPA 에서 DTO로 바로 조회, 플랫 데이터 (1 Query) (1 Query)
 * - 페이징 불가능
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, Lazy = null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     * <p>
     * - orderItem, item 관계를 직접 초기화하면 Hibernate5Module 설정에 의해 엔티티를 JSON으로 생성한다
     * - 양방향 연관관계면 무한 루프에 걸리지 않게 한곳에 @JsonIgnore 를 추가해야 한다
     * - 엔티티를 직접 노출하므로 좋은 방법은 아니다
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName()); // Lazy 강제 초기화
        }

        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        for (Order order : orders) {
            System.out.println("order ref = " + order + " id = " + order.getId());
        }

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     * V3.1 엔티티를 조회해서 DTO로 변환 고려
     * - ToOne 관계만 우선 모두 페치 조인으로 최적화
     * - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize 로 최적화
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        // Order 를 기준으로ToOne 관계 걸린 애는 다 패치조인으로 한방에 가져옴

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     * Query : 루트 1번, 컬렉션 N번 실행
     * ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1 : N) 관계는 각각 별도로 처리한다
     * - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다
     * - ToMany(1:N) 관계는 조인하면 row 수가 증가한다
     * row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고
     * , ToMany 관계는 최적화하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * Query : 루트 1번, 컬렉션 1번 실행
     * ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을 한꺼번에 조회
     * MAP 을 사용해서 매칭 성능 상향
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * Query : 1번
     * 단점
     *    - 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되므로 상황에 따라 V5 보다 느릴 수 있다
     *    - 애플리케이션에서 추가 작업이 크다
     *    - 페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> orderV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                ))
                .entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }


    /**
     * V2 용 DTO
     */
    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }

    }

    @Getter
    static class OrderItemDto {
        private String itemName; // 상품명
        private int orderPrice; // 주문 가격
        private int count; // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

}
