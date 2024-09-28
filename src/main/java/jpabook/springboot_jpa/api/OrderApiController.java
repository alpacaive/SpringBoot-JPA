package jpabook.springboot_jpa.api;

import jpabook.springboot_jpa.domain.Order;
import jpabook.springboot_jpa.domain.OrderItem;
import jpabook.springboot_jpa.repository.OrderRepository;
import jpabook.springboot_jpa.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V1 엔티티가 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 *
 * V2 엔티티를 조회해서 DTO로 변환 (fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 *
 * V3 엔티티를 조회해서 DTO로 변환 (fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야 함
 * - 대신 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능
 *
 * V4 JPA 에서 DTO 로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 *
 * V5 JPA 에서 DTO 로 바로 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 *
 * V6 JPA 에서 DTO로 바로 조회, 플랫 데이터 (1 Query) (1 Query)
 * - 페이징 불가능
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    /**
     * V1 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, Lazy = null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     *
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



}
