package jpabook.springboot_jpa.repository;

import jpabook.springboot_jpa.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // select m from Member m where m.name = :name -> 이렇게 자동으로 만들어버림
    List<Member> findByName(String name);

}
