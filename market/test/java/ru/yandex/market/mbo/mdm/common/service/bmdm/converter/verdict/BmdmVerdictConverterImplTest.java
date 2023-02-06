package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.verdict;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.MdmVerdictVersionWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.CpaVerdictGenerator;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.SskuImpersonalGoldenVerdictPreparationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.SskuPartnerVerdictPreparationUtil;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MetadataProviderMock;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class BmdmVerdictConverterImplTest {
    private BmdmVerdictConverter converter;
    private EnhancedRandom random;

    @Before
    public void setup() {
        MetadataProviderMock metadataProvider = new MetadataProviderMock();
        metadataProvider.addEntityType(TestBmdmUtils.VERDICT_ET);
        metadataProvider.addEntityType(TestBmdmUtils.SILVER_SSKU_RESOLUTION_ET);
        metadataProvider.addEntityType(TestBmdmUtils.GOLDEN_SSKU_RESOLUTION_ET);

        converter = new BmdmVerdictConverterImpl(metadataProvider);
        random = TestDataUtils.defaultRandom(54367654777L);
    }

    @Test
    public void testIdempotency() {
        for (int i = 0; i < 100; i++) {
            var goldenResolution = goldenResolution();
            var silverResolution = silverResolution();

            Assertions.assertThat(converter.entity2gold(converter.gold2entity(goldenResolution)))
                .isEqualTo(goldenResolution);
            Assertions.assertThat(converter.entity2silver(converter.silver2entity(silverResolution)))
                .isEqualTo(silverResolution);
        }
    }

    private SskuVerdictResult goldenResolution() {
        ShopSkuKey key = random.nextObject(ShopSkuKey.class);
        List<ErrorInfo> errors = errors(random.nextInt(10));
        SskuVerdictResult sskuVerdictResult = SskuImpersonalGoldenVerdictPreparationUtil.prepareVerdictResult(key,
            errors,
            new MdmVerdictVersionWrapper()
                .setMdmVersionTs(random.nextObject(Instant.class))
                .setContentVersionId(random.nextLong()));

        sskuVerdictResult.setMdmId(random.nextLong());
        sskuVerdictResult.setUpdatedTs(random.nextObject(Instant.class));
        return sskuVerdictResult;
    }

    private SskuPartnerVerdictResult silverResolution() {
        ShopSkuKey key = random.nextObject(ShopSkuKey.class);
        List<ErrorInfo> errors = errors(random.nextInt(10));
        SskuPartnerVerdictResult sskuPartnerVerdictResult =
            SskuPartnerVerdictPreparationUtil.prepareVerdictResult(key, errors,
            new MdmVerdictVersionWrapper()
                .setMdmVersionTs(random.nextObject(Instant.class))
                .setContentVersionId(random.nextLong()));
        sskuPartnerVerdictResult.setMdmId(random.nextLong());
        sskuPartnerVerdictResult.setUpdatedTs(random.nextObject(Instant.class));
        return sskuPartnerVerdictResult;
    }

    private List<ErrorInfo> errors(int amount) {
        int border = CpaVerdictGenerator.ERROR_TYPES_FOR_VERDICT_GENERATION.length;
        List<ErrorInfo> errors = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            int idx = random.nextInt(border);
            String errorCode = CpaVerdictGenerator.ERROR_TYPES_FOR_VERDICT_GENERATION[idx].getErrorCode();
            String messageTemplate = random.nextObject(String.class);
            ErrorInfo.Level level = random.nextObject(ErrorInfo.Level.class);
            Map<String, Object> params = map(random.nextInt(10));
            errors.add(new ErrorInfo(errorCode, messageTemplate, level, params));
        }
        return errors;
    }

    private Map<String, Object> map(int amount) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < amount; i++) {
            switch (random.nextObject(RandomType.class)) {
                case BOOL:
                    result.put(random.nextObject(String.class), random.nextBoolean());
                    break;
                case STRING:
                    result.put(random.nextObject(String.class), random.nextObject(String.class));
                    break;
                case INT:
                    result.put(random.nextObject(String.class), random.nextInt());
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private enum RandomType {
        STRING, BOOL, INT
    }
}
