package ru.yandex.market.cpabackoffice;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.mj.generated.client.test_service.api.IdmApiClient;
import ru.yandex.mj.generated.client.test_service.model.IdmAddOrRemoveRoleResponse;
import ru.yandex.mj.generated.client.test_service.model.IdmInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdmApiTest extends AbstractFunctionalTest {

    @Autowired
    private IdmApiClient idmApiClient;

    @Test
    public void getInfoTest() throws ExecutionException, InterruptedException {
        IdmInfo idmInfo = idmApiClient.idmInfoGet().schedule().get();

        assertEquals(0, idmInfo.getCode());

        assertEquals("role", idmInfo.getRoles().getSlug());

        assertEquals("Роль", idmInfo.getRoles().getName());
        assertEquals(1, idmInfo.getRoles().getValues().size());
    }

    @Test
    @Disabled
    public void addRoleTest() throws ExecutionException, InterruptedException {
        IdmAddOrRemoveRoleResponse idmInfo =
                idmApiClient.idmAddRolePost("test", "123", "{\"role\":\"admin\"").schedule().get();

        assertEquals(0, idmInfo.getCode());
    }
}

