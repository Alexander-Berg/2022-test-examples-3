package ru.yandex.market.mboc.common.erp;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;

public class AlreadySentDataFilterServiceTest extends MdmBaseDbTestClass {
    @Autowired
    private AlreadySentDataFilterService alreadySentDataFilterService;

    @Test
    public void testChangesDetection() {
        String processor = "test";
        ErpCCCodeMarkupChange change = new ErpCCCodeMarkupChange(
            "000032.32868000",
            "",
            Cis.NONE,
            "",
            false,
            List.of("TH"),
            MdmIrisPayload.CisHandleMode.NO_RESTRICTION
        );
        Map<String, Integer> sskuWithHash = Map.of(change.getShopSku(), change.hashCode());

        Assertions.assertThat(alreadySentDataFilterService.retainOnlyChangedItems(sskuWithHash, processor))
            .containsExactly(change.getShopSku());

        alreadySentDataFilterService.updateDiffHashes(sskuWithHash, processor);

        Assertions.assertThat(alreadySentDataFilterService.retainOnlyChangedItems(sskuWithHash, processor)).isEmpty();
    }
}
