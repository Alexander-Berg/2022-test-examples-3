package ru.yandex.market.yt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pholser.junit.quickcheck.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtImpl;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tms.monitor.MbiTeam;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportedTablesToYTMonitoringServiceTest extends FunctionalTest {

    private static final String HOST_HAHN = "hahn.yt.yandex.net";
    private static final String HOST_ARNOLD = "arnold.yt.yandex.net";
    private static final String FULFILLMENT_TRANSACTIONS =
            "home/market/production/mbi/delivery/fulfillment_transactions";
    private static final String FULFILLMENT_CONSOLIDATED_TRANSACTIONS =
            "home/market/production/mbi/delivery/fulfillment_consolidated_transactions";
    private static final String MARKET_DELIVERY_TRANSACTIONS =
            "home/market/production/mbi/delivery/market_delivery_transactions";

    private static final String ENTITY_HISTORY = "home/market/production/mbi/barc/entity_history";

    private static final LocalDate DATE_TIME = LocalDate.of(2020, 8, 30);
    private static final String TABLE_NAME = DATE_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE);

    @Autowired
    private ExportedTablesToYTMonitoringDao exportedTablesToYTMonitoringDao;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Тест получения информации при пустом конфиге")
    @DbUnitDataSet(
            before = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationEmptyConfig.before.csv",
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationEmptyConfig.after.csv"
    )
    void getMonitoringInformationEmptyConfig() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.BILLING
        );
        YtImpl yt = mockYt(HOST_HAHN, true);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации по одной таблице")
    @DbUnitDataSet(
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationOneNewRow.after.csv"
    )
    void getMonitoringInformationOneNewRow() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(FULFILLMENT_TRANSACTIONS, List.of(HOST_HAHN)),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.BILLING
        );
        YtImpl yt = mockYt(HOST_HAHN, true);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации по одной таблице для команды SHOPS")
    @DbUnitDataSet(
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationOneNewRowShops.after.csv"
    )
    void getMonitoringInformationOneNewRowShops() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(ENTITY_HISTORY, List.of(HOST_HAHN)),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.SHOPS
        );
        YtImpl yt = mockYt(HOST_HAHN, true);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации для команды SHOPS - для биллинга ничего не должно удалиться")
    @DbUnitDataSet(
            before = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationForShops.before.csv",
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationForShops.after.csv"
    )
    void getMonitoringInformationForShops() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(ENTITY_HISTORY, List.of(HOST_HAHN)),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.SHOPS
        );
        YtImpl yt = mockYt(HOST_HAHN, true);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации по одной таблице, при этом раньше запись с ней уже была")
    @DbUnitDataSet(
            before = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationWithExistRow.before.csv",
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationWithExistRow.after.csv"
    )
    void getMonitoringInformationWithExistRow() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(FULFILLMENT_TRANSACTIONS, List.of(HOST_HAHN)),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.BILLING
        );
        YtImpl yt = mockYt(HOST_HAHN, true);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации по одной таблице, которая не существует")
    @DbUnitDataSet(
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationOneNowExistNewRow.after.csv"
    )
    void getMonitoringInformationOneNowExistNewRow() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(FULFILLMENT_TRANSACTIONS, List.of(HOST_HAHN)),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.BILLING
        );
        YtImpl yt = mockYt(HOST_HAHN, false);
        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(yt).when(spy).getYtByHost(anyString());
        spy.getMonitoringInformation(DATE_TIME);
    }

    @Test
    @DisplayName("Тест получения информации по нескольким таблицам из нескольких кластеров")
    @DbUnitDataSet(
            before = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationTwoHosts.before.csv",
            after = "ExportedTablesToYTMonitoringServiceTest.getMonitoringInformationTwoHosts.after.csv"
    )
    void getMonitoringInformationTwoHosts() {
        ExportedTablesToYTMonitoringService service = new ExportedTablesToYTMonitoringService(
                "",
                Map.of(
                        FULFILLMENT_TRANSACTIONS, List.of(HOST_ARNOLD, HOST_HAHN),
                        FULFILLMENT_CONSOLIDATED_TRANSACTIONS, List.of(HOST_ARNOLD, HOST_HAHN),
                        MARKET_DELIVERY_TRANSACTIONS, List.of(HOST_HAHN)
                ),
                exportedTablesToYTMonitoringDao,
                transactionTemplate,
                MbiTeam.BILLING
        );
        YtImpl ytHahn = mockYt(
                HOST_HAHN,
                true,
                MARKET_DELIVERY_TRANSACTIONS + "/" + TABLE_NAME,
                List.of(
                        new Pair<>(FULFILLMENT_CONSOLIDATED_TRANSACTIONS + "/" + TABLE_NAME, 4000L),
                        new Pair<>(FULFILLMENT_TRANSACTIONS + "/" + TABLE_NAME, 5000L)
                )
        );
        YtImpl ytArnold = mockYt(HOST_ARNOLD,
                true,
                null,
                List.of(
                        new Pair<>(FULFILLMENT_CONSOLIDATED_TRANSACTIONS + "/" + TABLE_NAME, 2000L),
                        new Pair<>(FULFILLMENT_TRANSACTIONS + "/" + TABLE_NAME, 3000L)
                )
        );

        ExportedTablesToYTMonitoringService spy = Mockito.spy(service);
        Mockito.doReturn(ytHahn).when(spy).getYtByHost(HOST_HAHN);
        Mockito.doReturn(ytArnold).when(spy).getYtByHost(HOST_ARNOLD);
        spy.getMonitoringInformation(DATE_TIME);
    }

    @SuppressWarnings("SameParameterValue")
    private YtImpl mockYt(String proxy, boolean isExist) {
        return mockYt(proxy, isExist, null, List.of());
    }

    private YtImpl mockYt(String proxy, boolean isExist, String noExistKey,
                          List<Pair<String, Long>> ytPathToRowCountList) {
        YtImpl yt = mock(YtImpl.class);
        Cypress cypress = mock(Cypress.class);
        when(yt.getConfiguration()).thenReturn(YtUtils.getDefaultConfiguration(proxy, "token"));
        when(yt.cypress()).thenReturn(cypress);
        when(cypress.exists(any(YPath.class))).thenReturn(isExist);
        if (noExistKey != null) {
            when(cypress.exists(Mockito.eq(YPath.cypressRoot().child(noExistKey)))).thenReturn(false);
        }
        YTreeNode yTreeNode = mock(YTreeNode.class);
        when(yTreeNode.getAttribute(anyString())).thenReturn(Optional.of(yTreeNode));
        when(yTreeNode.longValue()).thenReturn(25875L);
        when(cypress.get(any(), anyCollection())).thenReturn(yTreeNode);
        ytPathToRowCountList.forEach(pair -> {
            YTreeNode node = mock(YTreeNode.class);
            when(node.getAttribute(anyString())).thenReturn(Optional.of(node));
            when(node.longValue()).thenReturn(pair.second);
            when(cypress.get(Mockito.eq(YPath.cypressRoot().child(pair.first)), anyCollection())).thenReturn(node);
        });
        return yt;
    }
}
