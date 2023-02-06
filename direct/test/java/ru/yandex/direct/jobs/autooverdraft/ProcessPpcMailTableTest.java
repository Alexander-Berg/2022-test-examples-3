package ru.yandex.direct.jobs.autooverdraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessPpcMailTableTest {
    @Mock
    OverdraftLimitChangesMailSenderService senderService;
    @Mock
    ClientService clientService;

    @InjectMocks
    OverdraftLimitChangesMailerJob job;

    private static final ClientId alreadyPresentSentClientId = ClientId.fromLong(6L);
    private static final ClientId alreadyPresentUnsentClientId = ClientId.fromLong(8L);
    private static final List<ClientId> clientIdsToSend =
            List.of(ClientId.fromLong(2L), ClientId.fromLong(3L), ClientId.fromLong(7L), alreadyPresentUnsentClientId);
    private static final List<ClientId> initialClientIdsNotToSend =
            List.of(ClientId.fromLong(1L), alreadyPresentSentClientId);
    private static final OverdraftLimitChangesInfo dummyInfo = new OverdraftLimitChangesInfo(alreadyPresentSentClientId,
            4D, "RUB", true);

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);

        var knownClientIds = new ArrayList<>(clientIdsToSend);
        knownClientIds.add(ClientId.fromLong(4L)); // 2, 3, 4, 7
        var clientIdsWithOverdraft = new HashSet<>(clientIdsToSend);
        clientIdsWithOverdraft.add(ClientId.fromLong(5L)); // 2, 3, 5, 7
        var initialClientIdsToSend = new ArrayList<>(knownClientIds);
        initialClientIdsToSend.add(ClientId.fromLong(5L)); // 2, 3, 4, 5, 7

        when(senderService.getClientsToSendMail()).thenReturn(Map.of(
                true, initialClientIdsNotToSend,
                false, initialClientIdsToSend)
        );
        when(clientService.massGetClientsByClientIds(any())).thenReturn(knownClientIds.stream()
                .collect(Collectors.toMap(i -> i, i -> new Client().withClientId(i.asLong())
                        .withOverdraftLimit(BigDecimal.TEN).withWorkCurrency(CurrencyCode.BYN)))
        );
        job = spy(job);
        doNothing().when(job).fetchExtraDataFromDb(any(), any());
        doReturn(true).when(job)
                .isOverdraftAvailableForClient(argThat(c ->
                        clientIdsWithOverdraft.contains(ClientId.fromLong(c.getClientId()))));
        doReturn(false).when(job)
                .isOverdraftAvailableForClient(argThat(c ->
                        !clientIdsWithOverdraft.contains(ClientId.fromLong(c.getClientId()))));
        job.changesToSend.put(alreadyPresentSentClientId, dummyInfo);
        job.changesToSend.put(alreadyPresentUnsentClientId, new OverdraftLimitChangesInfo(alreadyPresentUnsentClientId,
                15D, "BYN", true));
    }

    @Test
    void processAndCheck() {
        job.processPpcMailTable();
        var clientIdsNotToSend = new ArrayList<>(initialClientIdsNotToSend);
        clientIdsNotToSend.add(ClientId.fromLong(4L));
        clientIdsNotToSend.add(ClientId.fromLong(5L)); // 1, 4, 5, 6
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(job.changesToSend).containsKeys(clientIdsToSend.toArray(new ClientId[0])); // 2, 3, 7, 8
            softly.assertThat(job.changesToSend).doesNotContainKeys(clientIdsNotToSend.toArray(new ClientId[0]));
            softly.assertThat(job.changesToSend.getOrDefault(alreadyPresentUnsentClientId, dummyInfo)
                    .getOverdraftLimit()).isEqualTo(15D);
            softly.assertThatCode(()-> verify(senderService)
                    .deleteFromMailTableByClientIds(argThat(l
                            -> (l.contains(ClientId.fromLong(4L)) && l.contains(ClientId.fromLong(5L))))))
                    .doesNotThrowAnyException();
        });
    }
}
