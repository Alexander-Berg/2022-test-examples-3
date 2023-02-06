package ru.yandex.market.ff.model.bo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

public class TailPageTest {

    @Test
    void calculateNewPageRequestTestCase0() {
        PageRequest pageRequest = new PageRequest(0, 30);
        TailPage tailPage = TailPage.calculatePage(pageRequest, 45);
        Assertions.assertNull(tailPage);
    }

    @Test
    void calculateNewPageRequestTestCase1() {
        PageRequest pageRequest = new PageRequest(1, 30);
        TailPage tailPage = TailPage.calculatePage(pageRequest, 45);
        Assertions.assertNotNull(tailPage);
        Assertions.assertEquals(0, tailPage.getOffset());
        Assertions.assertEquals(15, tailPage.getPageSize());
    }

    @Test
    void calculateNewPageRequestTestCase2() {
        PageRequest pageRequest = new PageRequest(2, 30);
        TailPage tailPage = TailPage.calculatePage(pageRequest, 45);
        Assertions.assertNotNull(tailPage);
        Assertions.assertEquals(15, tailPage.getOffset());
        Assertions.assertEquals(30, tailPage.getPageSize());
    }

    @Test
    void calculateNewPageRequestTestCase3() {
        PageRequest pageRequest = new PageRequest(1, 30);
        TailPage tailPage = TailPage.calculatePage(pageRequest, 30);
        Assertions.assertNotNull(tailPage);
        Assertions.assertEquals(0, tailPage.getOffset());
        Assertions.assertEquals(30, tailPage.getPageSize());
    }

    @Test
    void calculateNewPageRequestTestCase4() {
        PageRequest pageRequest = new PageRequest(2, 30);
        TailPage tailPage = TailPage.calculatePage(pageRequest, 30);
        Assertions.assertNotNull(tailPage);
        Assertions.assertEquals(30, tailPage.getOffset());
        Assertions.assertEquals(30, tailPage.getPageSize());
    }

}
