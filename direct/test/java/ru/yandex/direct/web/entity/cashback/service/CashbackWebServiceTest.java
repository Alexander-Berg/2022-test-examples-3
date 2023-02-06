package ru.yandex.direct.web.entity.cashback.service;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.CashbackConstants;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.cashback.model.CashbackDetailsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CashbackWebServiceTest {

    @Autowired
    private CashbackWebService cashbackWebService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void testGetCashbackDetailsCsv_validationError() {
        var request = new CashbackDetailsRequest()
                .withPeriod(CashbackConstants.DETALIZATION_MAX_LENGTH + 1);
        var result = cashbackWebService.getCashbackDetailsCsv(clientInfo.getChiefUserInfo().getUser(), request);

        assertThat(result.isSuccessful())
                .isFalse();
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(field(CashbackDetailsRequest.PERIOD_FIELD)),
                                NumberDefects.inInterval(
                                        CashbackConstants.DETALIZATION_MIN_LENGTH,
                                        CashbackConstants.DETALIZATION_MAX_LENGTH)))));
    }

    @Test
    public void testGetCashbackDetailsCsv_success() throws IOException {
        var request = new CashbackDetailsRequest()
                .withPeriod(CashbackConstants.DETALIZATION_MAX_LENGTH);
        var result = cashbackWebService.getCashbackDetailsCsv(clientInfo.getChiefUserInfo().getUser(), request);

        assertThat(result.isSuccessful())
                .isTrue();
        // Истории у этого клиента нет, но файл всё равно будет и ссылку отдадим
        assertThat(result.getResult()).isNotBlank();
    }
}
