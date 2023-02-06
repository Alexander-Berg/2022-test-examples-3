package ru.yandex.market.billing.report.payment;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.report.payment.dao.PaymentReportDao;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PaymentReportYtExportServiceTest extends FunctionalTest {
    private static final LocalDate EXPORT_DATE = LocalDate.of(2022, 2, 14);

    @Autowired
    public PaymentReportDao paymentReportDao;

    @Autowired
    public EnvironmentService environmentService;

    @Value("${market.billing.payment-report.export.yt.path}")
    private String ytTablePath;

    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> ytWritenEntitiesCaptor;
    @Mock
    private Yt ytMock;
    @Mock
    private YtTransactions ytTransactions;
    @Mock
    private Transaction transaction;
    @Mock
    private YtTables ytTablesMock;
    @Mock
    private Cypress cypressMock;
    @Mock
    private GUID transactionId;

    @BeforeEach
    void setup() {
        when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);
        when(transaction.getId())
                .thenReturn(transactionId);
        setUpMock(ytMock);
    }

    private void setUpMock(Yt yt) {
        when(yt.tables()).thenReturn(ytTablesMock);
        when(yt.cypress()).thenReturn(cypressMock);
        when(yt.transactions()).thenReturn(ytTransactions);
    }

    @Test
    @DbUnitDataSet(before = "PaymentReportYtExportServiceTest.testExport.csv")
    void testExport() throws JsonProcessingException {
        PaymentReportYtExportService ytExportService =
                new PaymentReportYtExportService(
                        ytMock,
                        ytTablePath,
                        paymentReportDao,
                        environmentService
                );

        ytExportService.process(EXPORT_DATE);

        verify(ytTablesMock)
                .write(
                        any(),
                        eq(true),
                        argThat(yPath -> yPath.toString().equals(ytTablePath)),
                        eq(YTableEntryTypes.JACKSON_UTF8),
                        ytWritenEntitiesCaptor.capture()
                );

        ArgumentCaptor<CreateNode> createNodeArgumentCaptor = ArgumentCaptor.forClass(CreateNode.class);
        verify(cypressMock)
                .create(createNodeArgumentCaptor.capture());

        verifyNoMoreInteractions(ytTablesMock);

        List<String> actual = ImmutableList.copyOf(ytWritenEntitiesCaptor.getValue())
                .stream()
                .map(JsonNode::toString)
                .collect(Collectors.toList());

        String expectedJson = StringTestUtil.getString(
                getClass(),
                "PaymentReportYtExportServiceTest.testExport.json"
        );

        String[] expected = Arrays.stream(new ObjectMapper().readValue(expectedJson, JsonNode[].class))
                .map(JsonNode::toString)
                .toArray(String[]::new);

        assertThat(actual, Matchers.containsInAnyOrder(expected));
    }
}
