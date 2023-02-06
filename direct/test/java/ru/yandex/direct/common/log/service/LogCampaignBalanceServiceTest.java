package ru.yandex.direct.common.log.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.direct.common.log.LogHelper;
import ru.yandex.direct.common.log.container.LogCampaignBalanceData;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class LogCampaignBalanceServiceTest {

    private LogCampaignBalanceService service;
    private LogCampaignBalanceData sendingLogData;
    private LogCampaignBalanceData expectedLogData;

    @Mock
    private LogHelper logHelper;

    @Captor
    private ArgumentCaptor<LogCampaignBalanceData> captor;

    @Before
    public void before() {
        initMocks(this);
        service = new LogCampaignBalanceService(logHelper);

        sendingLogData = new LogCampaignBalanceData()
                .withCid(RandomNumberUtils.nextPositiveLong())
                .withClientId(RandomNumberUtils.nextPositiveLong())
                .withTid(RandomNumberUtils.nextPositiveLong())
                .withType(CampaignsType.text.getLiteral())
                .withCurrency(CurrencyCode.RUB.name())
                .withSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumBalance(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumDelta(RandomNumberUtils.nextPositiveBigDecimal());

        //копия отправляемого объекта
        expectedLogData = new LogCampaignBalanceData()
                .withCid(sendingLogData.getCid())
                .withClientId(sendingLogData.getClientId())
                .withTid(sendingLogData.getTid())
                .withType(sendingLogData.getType())
                .withCurrency(sendingLogData.getCurrency())
                .withSum(sendingLogData.getSum())
                .withSumBalance(sendingLogData.getSumBalance())
                .withSumDelta(sendingLogData.getSumDelta());
    }


    @Test
    public void checkGetLogEntryArgument() {
        service.logCampaignBalance(sendingLogData);

        verify(logHelper).getLogEntry(captor.capture());
        assertThat("проверяем, что сервис вызвал logHelper.getLogEntry с ожидаемым объектом",
                captor.getValue(), beanDiffer(expectedLogData));
    }
}
