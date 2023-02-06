package ru.yandex.market.crm.operatorwindow.services.fraud;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.AttemptResult;
import ru.yandex.market.crm.operatorwindow.external.smartcalls.ConfirmFraudAttemptResult;
import ru.yandex.market.crm.operatorwindow.util.DateParsers;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.utils.serialize.CustomJsonDeserializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

public class SmartcallsResultsTest {

    private static final Logger LOG = LoggerFactory.getLogger(SmartcallsResultsTest.class);

    private static final CustomJsonDeserializer deserializer
            = new CustomJsonDeserializer(new ObjectMapperFactory(Optional.empty()));

    private static final SmartcallsResults smartcallsResults = new SmartcallsResults(null, null, deserializer);

    private static final String ATTEMPTS_PATH = "two-attempts.json";
    private static final String THREE_ATTEMPTS_PATH = "three-attempts.json";

    private Collection<ConfirmFraudAttemptResult> selectAttempts(String resourcePath) {
        byte[] resource = ResourceHelpers.getResource(resourcePath);
        LOG.trace("resource {}: {}", resourcePath, CrmStrings.valueOf(resource));
        List<AttemptResult> res = deserializer.readObject(
                new TypeReference<List<AttemptResult>>() {
                }, resource);

        return res.stream()
                .map(smartcallsResults::parseCallData)
                .filter(SmartcallsResults::filterAttemptsWithOrderId)
                .collect(SmartcallsResults.createCollectorSelectingMaximum(
                        attempt -> attempt.getCallData().getOrderId(),
                        SmartcallsResults.MORE_SUCCESS_ATTEMPT_COMPARATOR))
                .values();
    }


    @Test
    public void compareAttempts() {
        Collection<ConfirmFraudAttemptResult> selectedAttempts = selectAttempts(ATTEMPTS_PATH);

        Assertions.assertEquals(1, selectedAttempts.size());
        ConfirmFraudAttemptResult attempt = selectedAttempts.iterator().next();
        Assertions.assertTrue(attempt.getAttemptResult().isAttemptSuccessful());
        Assertions.assertTrue(attempt.getCallData().isOrderConfirmed());
    }

    @Test
    // Проверяем на данных с прода, с проблемы с тикета OCRM-4058. Оличается от twoAttempts только количеством попыток.
    public void compareThreeAttempts() {
        Collection<ConfirmFraudAttemptResult> selectedAttempts = selectAttempts(THREE_ATTEMPTS_PATH);

        Assertions.assertEquals(1, selectedAttempts.size());
        ConfirmFraudAttemptResult attempt = selectedAttempts.iterator().next();
        Assertions.assertTrue(attempt.getAttemptResult().isAttemptSuccessful());
        Assertions.assertTrue(attempt.getCallData().isOrderConfirmed());
    }

    @Test
    public void formatOldestDateTimeYekt() {
        checkFormatOldestDateTime("2020-01-27 06:41:01", "2020-01-27T11:41:01+05:00");
    }

    @Test
    public void formatOldestDateTimeMsk() {
        checkFormatOldestDateTime("2020-01-27 08:41:01", "2020-01-27T11:41:01+03:00");
    }

    @Test
    public void formatOldestDateTimeIso() {
        checkFormatOldestDateTime("2020-01-27 11:41:01", "2020-01-27T11:41:01+00:00");
    }

    public void checkFormatOldestDateTime(String expected, String value) {
        Assertions.assertEquals(expected, SmartcallsResults.formatOldestDateTime(DateParsers.parseIso(value)));
    }

}
