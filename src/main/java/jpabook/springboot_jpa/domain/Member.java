package jpabook.springboot_jpa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded
    private Address address;

//    @JsonIgnore -> 이거 넣으면 주문 정보는 빠지고 단순 회원 정보만 나옴 BUT 안좋음 = 엔티티는 노출하지 말자.
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

}
