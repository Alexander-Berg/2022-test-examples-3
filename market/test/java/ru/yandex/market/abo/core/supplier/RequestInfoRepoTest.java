package ru.yandex.market.abo.core.supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author imelnikov
 */
public class RequestInfoRepoTest extends EmptyTest {

    @Autowired
    RequestInfoRepo requestInfoRepo;

    @Test
    public void criteria() {

        requestInfoRepo.save(new RequestInfo(1, 2));

        assertFalse(requestInfoRepo.findAll().isEmpty());
    }
}
