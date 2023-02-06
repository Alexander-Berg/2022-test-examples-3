package ru.yandex.market.logistics.management.controller.partner;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.PartnerController;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PartnerControllerSyncPickupPointsTest extends AbstractContextualTest {

    @Autowired
    private PartnerController partnerController;

    @ParameterizedTest
    @MethodSource("externalHashResetValues")
    void testPartnerSyncPickupPoints(Boolean externalHashReset) throws Exception {
        doNothing().when(partnerController).syncPickupPoints(1L, externalHashReset);

        MockHttpServletRequestBuilder requestBuilder = patch("/externalApi/partners/1/syncPickupPoints");
        if (externalHashReset != null) {
            requestBuilder.param("externalHashReset", String.valueOf(externalHashReset));
        }

        mockMvc.perform(requestBuilder).andExpect(status().isOk());
        verify(partnerController).syncPickupPoints(1L, externalHashReset);
    }

    @Nonnull
    private static Stream<Arguments> externalHashResetValues() {
        return Stream.of(null, true, false).map(Arguments::of);
    }
}
