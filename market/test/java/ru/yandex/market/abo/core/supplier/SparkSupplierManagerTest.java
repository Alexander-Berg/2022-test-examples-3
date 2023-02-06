package ru.yandex.market.abo.core.supplier;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoffBuilder;
import ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.no_placement.NoPlacementManager;
import ru.yandex.market.abo.core.no_placement.NoPlacementRecord;
import ru.yandex.market.abo.core.spark.SparkManager;
import ru.yandex.market.abo.core.spark.api.SparkApiDataLoader;
import ru.yandex.market.abo.core.spark.dao.SparkService;
import ru.yandex.market.abo.core.spark.dao.SparkShopData;
import ru.yandex.market.abo.core.spark.data.Status2;
import ru.yandex.market.abo.core.spark.model.SparkCheckResult;
import ru.yandex.market.abo.core.spark.model.SparkCheckShop;
import ru.yandex.market.abo.core.spark.status.SparkStatusCheck;
import ru.yandex.market.abo.core.spark.status.SparkStatusService;
import ru.yandex.market.abo.core.spark.status.SparkStatusStCreator;
import ru.yandex.market.abo.core.spark.yt.SparkYtDataLoader;
import ru.yandex.market.abo.util.FakeUsers;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.supplier.SparkSupplierManager.buildXmlBody;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 04.08.2020
 */
class SparkSupplierManagerTest {

    private static final long SUPPLIER_ID = 123L;
    private static final String SUPPLIER_NAME = "Поставщик";
    private static final String OGRN = "1234567890123";

    private static final int SPARK_ID = 1;

    @InjectMocks
    private SparkSupplierManager sparkSupplierManager;

    @Mock
    private FeatureStatusManager featureStatusManager;
    @Mock
    private SparkApiDataLoader sparkApiDataLoader;
    @Mock
    private SparkYtDataLoader sparkYtDataLoader;
    @Mock
    private SparkManager sparkManager;
    @Mock
    private SparkService sparkService;
    @Mock
    private SparkStatusService sparkStatusService;
    @Mock
    private SparkStatusStCreator stTicketCreator;
    @Mock
    private ConfigurationService coreConfigService;
    @Mock
    private NoPlacementManager noPlacementManager;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(sparkService.findOne(SPARK_ID)).thenReturn(mock(SparkShopData.class));
        when(featureStatusManager.sendResult(any(FeatureCutoff.class))).thenReturn(GenericStatusResponse.OK_RESPONSE);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE, false", "NOT_ACTIVE, true"})
    void checkStatusTest(SparkCheckResult checkResult, boolean turnOffSupplier) {
        var checkShop = mockCheckShop(checkResult);
        when(sparkManager.checkStatus(List.of(checkShop), true))
                .thenReturn(Map.of(checkShop.getOgrn(), checkResult));

        sparkSupplierManager.checkStatusForSuppliers(List.of(checkShop));

        if (turnOffSupplier) {
            verify(sparkService).updateStatus(anyInt(), anyBoolean(), anyInt());
            verify(sparkStatusService).saveStatusCheck(any(SparkStatusCheck.class));
            verify(noPlacementManager).addRecord(new NoPlacementRecord(SUPPLIER_ID,
                    SparkManager.WRONG_JUR_INFO_REASON_ID,
                    FakeUsers.JUR_INFO_CHECKER.getId())
            );
            verify(featureStatusManager).sendResult(FeatureCutoffBuilder.create(
                    SUPPLIER_ID,
                    FeatureType.MARKETPLACE,
                    ParamCheckStatus.FAIL,
                    FeatureCutoffReason.REQUISITES
            ).withTid(Messages.MBI.SUPPLIER_WRONG_JUR_INFO).withInfo(buildXmlBody(checkShop)).build());
        } else {
            verifyNoMoreInteractions(sparkService, featureStatusManager);
        }
    }

    private static SparkCheckShop mockCheckShop(SparkCheckResult checkResult) {
        var checkShop = new SparkCheckShop(SUPPLIER_ID, OGRN, SUPPLIER_NAME);
        checkShop.setSparkId(SPARK_ID);
        if (checkResult == SparkCheckResult.NOT_ACTIVE) {
            var status = new Status2();
            status.setCode(59);
            checkShop.setStatus(status);
        }

        return checkShop;
    }
}
