package ru.yandex.market.billing.factoring.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.bcl.BclApiService;
import ru.yandex.market.billing.bcl.CreditorInfo;
import ru.yandex.market.billing.bcl.CreditorStateDto;
import ru.yandex.market.billing.bcl.CreditorStatus;
import ru.yandex.market.billing.factoring.dao.FactorRejectDao;
import ru.yandex.market.billing.person.dao.PersonInfoDao;
import ru.yandex.market.billing.person.dao.UncheckedPersonDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@ExtendWith(MockitoExtension.class)
class SendClientsToFactoringBankServiceTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Autowired
    private UncheckedPersonDao uncheckedPersonDao;

    @Autowired
    private PersonInfoDao personInfoDao;

    @Autowired
    private FactorRejectDao factorRejectDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Mock
    private BclApiService bclApiService;

    @Captor
    private ArgumentCaptor<Collection<CreditorInfo>> creditorsCaptor;


    private SendClientsToFactoringBankService service;

    @BeforeEach
    void setUp() {
        service = new SendClientsToFactoringBankService(
                uncheckedPersonDao, personInfoDao, factorRejectDao, transactionTemplate, bclApiService,
                Clock.fixed(Instant.parse("2021-10-20T06:06:33.00Z"), ZoneId.of("UTC"))
        );
    }

    @Test
    @DbUnitDataSet(
            before = "SendClientsToFactoringBankService.before.csv",
            after = "SendClientsToFactoringBankService.after.csv"
    )
    void sendCreditorsToCheck_whenSendingCreditorChanged() {
        when(bclApiService.sendCreditorsToCheck(any(), creditorsCaptor.capture())).then(invocation -> {
            // имитируем изменение юридических данных клиента с person_id = 4
            // в процессе отправки предыдущих изменений в банк
            imitateNewLegalInfoChange();

            return List.of(
                    // при отправке кредитора 2 произошла ошибка,
                    // поэтому в результат вызова sendCreditorsToCheck() он не попал

                    // успешно отправлен в банк
                    defaultCreditorStateDto("3").build(),

                    // мы отправили в банк клиента с person_id = 4 и получили подтверждение, что банк принял
                    defaultCreditorStateDto("4").build()
            );
        });

        service.execute();

        // убеждаемся, что в банк были отправлены все необработанные клиенты
        assertThat(creditorsCaptor.getValue()).hasSize(3);
    }

    @Test
    @DbUnitDataSet(
            before = "SendClientsToFactoringBankService.before.csv",
            after = "SendClientsToFactoringBankService.addToBlacklist.after.csv"
    )
    void sendCreditorsToCheck_shouldAddSentCreditorToBlackList() {
        when(bclApiService.sendCreditorsToCheck(any(), creditorsCaptor.capture())).thenReturn(List.of(
                defaultCreditorStateDto("2").build(),
                defaultCreditorStateDto("3").build(),
                defaultCreditorStateDto("4").build()
        ));

        service.execute();
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "SendClientsToFactoringBankService.before.csv",
                    "SendClientsToFactoringBankService.clientsWithSameLegalInfo.csv"
            },
            after = "SendClientsToFactoringBankService.clientsWithSameLegalInfo.after.csv"
    )
    void sendCreditorsToCheck_whenThereAreSeveralUncheckedClientsWithSameLegalInfo() {
        when(bclApiService.sendCreditorsToCheck(any(), creditorsCaptor.capture())).thenReturn(List.of(
                defaultCreditorStateDto("2").build(),
                defaultCreditorStateDto("3").build(),
                defaultCreditorStateDto("4").build(),
                // отправка реквизитов клиента с person_id=5 приведет к ошибке повторной отправки,
                // но сервис обработает её и вернет CreditorStateDto такой же, как в предыдущей строке,
                // только с другим id (который нам не интересен, поэтому пофиг на него)
                defaultCreditorStateDto("4").setId("-").build()
        ));

        service.execute();

        // убеждаемся, что сервис подхватил все 4 записи из unchecked_person, у которых processed = false
        assertThat(creditorsCaptor.getValue()).hasSize(4);
    }

    private CreditorStateDto.Builder defaultCreditorStateDto(String identity) {
        return CreditorStateDto.builder()
                .setId("")
                .setName("Billing-" + identity)
                .setInn("1111111111" + identity)
                .setOgrn("987666666666" + identity)
                .setStatus(CreditorStatus.FETCHED)
                .setStatusLastChange("");
    }

    private void imitateNewLegalInfoChange() {
        pgNamedParameterJdbcTemplate.update(
                "update market_billing.unchecked_person set updated_at = '2021-10-30' where person_id = 4",
                emptyMap()
        );
    }
}
