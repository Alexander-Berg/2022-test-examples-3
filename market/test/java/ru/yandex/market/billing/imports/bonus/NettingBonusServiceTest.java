package ru.yandex.market.billing.imports.bonus;

import java.time.ZoneId;
import java.util.Iterator;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тестирует импорт начисленных по программам бонусов из YT в оракл, для обиливания.
 */
@ExtendWith(MockitoExtension.class)
public class NettingBonusServiceTest extends FunctionalTest {

    private static final ZoneId LOCAL_ZONE_ID = TimeZone.getDefault().toZoneId();
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Europe/Moscow");

    @Value("${netting.bonus.table://home/netting_bonus}")
    private String table;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("pgNamedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Mock
    private Yt yt;
    @Mock
    private Cypress cypress;
    @Mock
    private YtTables ytTables;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeAll
    static void beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
    }

    @AfterAll
    static void afterAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(LOCAL_ZONE_ID));
    }

    @DisplayName("Тестирует что будут импортированы и начислены новые бонусы, хранящиеся в YT")
    @Test
    @DbUnitDataSet(before = "NettingBonusServiceTest.before.csv", after = "NettingBonusServiceTest.after.csv")
    public void testImportNewBonuses() {
        YPath yPath = getPath();
        Assertions.assertThat(yPath).isNotNull();

        NettingBonusService service = getNettingBonusService();
        service.doImport();

        verify(yt).cypress();
        verify(cypress).exists(yPath);
        verify(yt).tables();
        verify(ytTables).read(argThat(yPath::equals), argThat(arg -> arg.equals(YTableEntryTypes.YSON)),
                any(Consumer.class));
    }

    @DisplayName("Тестирует полного реимпорта бонусов из YT")
    @Test
    @DbUnitDataSet(before = "NettingBonusServiceTest.before.csv",
            after = "NettingBonusServiceTest.fullImport.after.csv")
    public void testReimportAllBonuses() {
        YPath yPath = getPath();
        Assertions.assertThat(yPath).isNotNull();
        environmentService.setValue(NettingBonusService.IS_FULL_IMPORT_ENV_KEY, "true");

        NettingBonusService service = getNettingBonusService();
        service.doImport();
    }

    @DisplayName("Тестирует проставление bonus_account_id для импортированных из оракла исторических бонусов")
    @Test
    @DbUnitDataSet(before = "NettingBonusServiceTest.updateAcc.before.csv",
            after = "NettingBonusServiceTest.updateAcc.after.csv")
    public void testImportBonusesAccounts() {
        YPath yPath = getPath();
        Assertions.assertThat(yPath).isNotNull();
        NettingBonusService service = getNettingBonusService();

        service.updatePartnersBonusAccountIds();
    }

    @NotNull
    private NettingBonusService getNettingBonusService() {
        NettingBonusDao nettingBonusDao = new NettingBonusDao(pgNamedParameterJdbcTemplate);
        return new NettingBonusService(yt, table, nettingBonusDao, transactionTemplate, environmentService);
    }

    private YPath getPath() {
        when(yt.cypress()).thenReturn(cypress);
        YPath yPath = YPath.simple(table);
        when(cypress.exists(yPath)).thenReturn(true);
        when(yt.tables()).thenReturn(ytTables);

        Iterator<YTreeMapNode> ytRows = getYtRows();

        doAnswer((Answer<Void>) invocation -> {
            Consumer<YTreeMapNode> mapper = invocation.getArgument(2);
            while (ytRows.hasNext()) {
                mapper.accept(ytRows.next());
            }
            return null;
        }).when(ytTables).read(argThat(yPath::equals), argThat(arg -> arg.equals(YTableEntryTypes.YSON)),
                any(Consumer.class));
        return yPath;
    }

    private Iterator<YTreeMapNode> getYtRows() {
        return Stream.of(
                getRow(111, "NEWBIE", 123, 10_000, true, 1636146000000L, 1638738000000L),
                getRow(222, "NEWBIE", 124, 20_000, true, 1636146000000L, 0L),
                getRow(333, "NEWBIE", 125, 30_000, true, 1636146000000L, 1638738000000L),
                getRow(444, "INVOLVE_ALL", 126, 40_000, true, 1636146000000L, 1638738000000L),
                getRow(555, "NEWBIE", 127, 50_000, true, 1636146000000L, 1638738000000L)
        ).collect(Collectors.toList()).iterator();
    }

    private YTreeMapNode getRow(long partnerId, String programType, long bonusId, int bonusSum, boolean enabled,
                                long issuedAt, long expiredAt) {
        YTreeMapNode row1 = mock(YTreeMapNode.class);
        when(row1.getLongO("partner_id")).thenReturn(Optional.of(partnerId));
        when(row1.getStringO("program_type")).thenReturn(Optional.of(programType));
        when(row1.getLongO("bonus_id")).thenReturn(Optional.of(bonusId));
        when(row1.getIntO("bonus_sum")).thenReturn(Optional.of(bonusSum));
        when(row1.getBoolO("enabled")).thenReturn(Optional.of(enabled));
        when(row1.getLongO("timestamp")).thenReturn(Optional.of(issuedAt));
        when(row1.getLongO("expired_at")).thenReturn(Optional.of(expiredAt));
        return row1;
    }
}
