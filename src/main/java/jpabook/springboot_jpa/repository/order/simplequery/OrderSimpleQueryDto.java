package jpabook.springboot_jpa.repository.order.simplequery;

import jpabook.springboot_jpa.domain.Address;
import jpabook.springboot_jpa.domain.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Read 용 DTO (v2)
 */
@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    // DTO가 파라미터를 엔티티로 받는 것은 상관없다
    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name; // LAZY 초기화 = 영속성 컨텍스트가 이 멤버 아이디를 갖고 영속성 컨텍스트를 찾아보고 없으면 DB 쿼리 날림
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address; // LAZY 초기화
    }
}
