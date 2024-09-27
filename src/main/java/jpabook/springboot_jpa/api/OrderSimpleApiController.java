package jpabook.springboot_jpa.api;

import jpabook.springboot_jpa.domain.Address;
import jpabook.springboot_jpa.domain.Order;
import jpabook.springboot_jpa.domain.OrderStatus;
import jpabook.springboot_jpa.repository.OrderRepository;
import jpabook.springboot_jpa.repository.OrderSearch;
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
