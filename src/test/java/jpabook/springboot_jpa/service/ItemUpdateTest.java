package jpabook.springboot_jpa.service;

import jakarta.persistence.EntityManager;
import jpabook.springboot_jpa.domain.item.Book;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ItemUpdateTest {

    @Autowired
    EntityManager em;

    @Test
    public void updateTest() throws Exception {
        Book book = new Book();

        // TX
        book.setName("asdfasdf");

        // 변경 감지 == dirty checking
        // TX commit


    }

}
