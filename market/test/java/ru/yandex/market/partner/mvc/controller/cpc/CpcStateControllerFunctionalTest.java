package ru.yandex.market.partner.mvc.controller.cpc;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.core.cpc.CPC;
import ru.yandex.market.core.cpc.CpcState;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
public class CpcStateControllerFunctionalTest extends FunctionalTest {

    /**
     * Простой тест GET-ручки.
     * <p>
     * Так как в базе нет никаких записей про магазин 774,
     * значит у него нет cutoff-ов, а
     * значит CPC у него включён (REAL).
     */
    @Test
    public void testCpcState() {
        ResponseEntity<CpcStateResponse> entity =
                FunctionalTestHelper.get(baseUrl + "/cpcState?datasourceId=774&_user_id=1", CpcStateResponse.class);
        Assertions.assertEquals(200, entity.getStatusCode().value());
        Assertions.assertEquals(CPC.REAL, entity.getBody().cpcState.getCpc());

    }

    @XmlRootElement(name = "data")
    static class CpcStateResponse {
        @XmlElement(name = "result")
        public CpcState cpcState;

        @Override
        public String toString() {
            return "CpcStateResponse{" +
                    "cpcState=" + cpcState +
                    '}';
        }
    }

}
