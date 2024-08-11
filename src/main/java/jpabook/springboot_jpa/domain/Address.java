package jpabook.springboot_jpa.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class  Address {

    private String city;
    private String street;
    private String zipcode;

    // jpa 스펙상 만들어 놓은 것 = 사용 X
    // JPA가 이런 제약을 두는 이유는 JPA 구현 라이브러리가 객체를 생성할 대 리플랙션 같은 기술을 사용할 수 있도록 지원해야 하기 때문
    protected Address() {

    }

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }

}
