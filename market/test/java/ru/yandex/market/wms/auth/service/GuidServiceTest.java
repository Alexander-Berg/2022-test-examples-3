package ru.yandex.market.wms.auth.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class GuidServiceTest extends AuthIntegrationTest {

    @Autowired
    private GuidService guidService;

    @Test
    public void getGUIDStatic() {
        String guid = guidService.getGuid();
        assertLinesMatch(List.of("[0-9A-F]{32}"), List.of(guid));
    }
}
