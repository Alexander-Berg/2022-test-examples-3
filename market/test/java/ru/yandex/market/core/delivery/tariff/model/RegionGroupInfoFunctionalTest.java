package ru.yandex.market.core.delivery.tariff.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.test.context.FunctionalTest;

// FunctionalTest потому что хотим проверить сериализацию с ObjectMapper'ом, который реально используется в проде.
// Так как в проде падала ручка из-за того, что маппер ругался на лишние свойства, а
// у нас распространены мапперы, которые не ругаются.
@ParametersAreNonnullByDefault
public class RegionGroupInfoFunctionalTest extends FunctionalTest {
    @Autowired
    private ObjectMapper jacksonMapper;

    @Test
    void testSerialization() throws IOException {
        String frontendActualRequest =
                "{\"datasourceId\":311291," +
                        "\"groupName\":\"ЛО\"," +
                        "\"regions\":[165888,165992,165993,166063,166156,166158,166160,166163,166202,166320,166321,166323,166409,166690]," +
                        "\"removeRegions\":{}}";
        RegionGroupInfo groupInfo = jacksonMapper.readValue(frontendActualRequest, RegionGroupInfo.class);
        HashSet<Long> expectedRegions = new HashSet<>(Arrays.asList(
                165888L,
                165992L,
                165993L,
                166063L,
                166156L,
                166158L,
                166160L,
                166163L,
                166202L,
                166320L,
                166321L,
                166323L,
                166409L,
                166690L));
        Assertions.assertEquals(expectedRegions, groupInfo.getRegions());
        Assertions.assertEquals("ЛО", groupInfo.getGroupName());
        Assertions.assertEquals(Collections.emptyMap(), groupInfo.getRemoveRegions());
    }
}