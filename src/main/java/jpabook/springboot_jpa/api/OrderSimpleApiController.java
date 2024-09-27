package jpabook.springboot_jpa.api;

import jpabook.springboot_jpa.domain.Address;
import jpabook.springboot_jpa.domain.Order;
import jpabook.springboot_jpa.domain.OrderStatus;
import jpabook.springboot_jpa.repository.OrderRepository;
import jpabook.springboot_jpa.repository.OrderSearch;
import jpabook.springboot_jpa.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.springboot_jpa.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> orderV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화
        }
        return all;
    }

    /**
     * 엔티티를 DTO 로 변환하는 일반적인 방법
     * 쿼리가 총 1 + N + N번 실행된다 (v1과 쿼리수 결과는 같다)
     *    * order 조회 1번 (order 조회 결과 수가 N이 된다)
     *    * order -> member 지연 로딩 N 번
     *    * order -> delivery 지연 로딩 N 번
     *    * ex) order의 결과가 4개면 최악의 경우 1+4+4번 실행된다 (최약의 경우)
     *       * 지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> orderV2() {
        // Order 2개 조회
        // N + 1 -> 1(첫번째 쿼리) + N(회원 2 + 배송 2 = 4) = 5번의 쿼리 나감
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }
    // === v1, v2의 공통적인 문제점 : 레이지 로딩으로 인한 데이터베이스 쿼리가 너무 많이 호출되는 문제가 발생 ===

    /**
     * 페치 조인 사용해서 최적화
     * 엔티티를 페치 조인을 사용해 쿼리 1번에 조회
     * 페치 조인으로 order -> member, order -> delivery 는 이미 조회된 상태이므로 지연로딩 X
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * JPA에서 DTO로 바로 조회
     * 일반적인 SQL 을 사용할 때 처럼 원하는 값을 선택하여 조회
     * new 명령어를 사용해 JPQL의 결과를 DTO로 즉시 변환
     * SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트워크 용량 최적화(생각보다 미비)
     * 리포지토리 재사용성이 떨어짐, API 스펙에 맞춘 코드가 리포지토리에 들어가는 단점
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }
    // v3, v4 비교 : v3는 재사용성이 좋고 v4는 재사용성이 없지만, v3보다는 v4가 성능 최적화 면에서는 조금 더 좋다.

    /**
     * === 쿼리 방식 선택 권장 순서 ===
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택
     * 2. 필요하면 페치 조인으로 성능 최적화 -> 대부분의 성능 이슈 해결
     * 3. 그래도 안되면 DTO로 직접 조회하는 방법 사용
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template 사용해 SQL을 직접 사용
     */

    /**
     * Read 용 DTO (v2)
     */
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        // DTO가 파라미터를 엔티티로 받는 것은 상관없다
        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 = 영속성 컨텍스트가 이 멤버 아이디를 갖고 영속성 컨텍스트를 찾아보고 없으면 DB 쿼리 날림
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }

}
