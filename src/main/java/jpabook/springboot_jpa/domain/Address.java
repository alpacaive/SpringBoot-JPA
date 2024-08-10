package jpabook.springboot_jpa.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class  Address {

    private String city;
    private String street;
    private String zipcode;

}
