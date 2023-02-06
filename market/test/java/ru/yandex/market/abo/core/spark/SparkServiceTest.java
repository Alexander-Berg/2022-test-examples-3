package ru.yandex.market.abo.core.spark;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.spark.dao.SparkAffiliate;
import ru.yandex.market.abo.core.spark.dao.SparkAffiliate.AffiliateType;
import ru.yandex.market.abo.core.spark.dao.SparkFrozenAccount;
import ru.yandex.market.abo.core.spark.dao.SparkService;
import ru.yandex.market.abo.core.spark.dao.SparkShopData;
import ru.yandex.market.abo.core.spark.data.FrozenAccountDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
public class SparkServiceTest extends EmptyTest {
    protected static final String TEST_OGRN_OOO = "5555555555555";
    private static final String TEST_OGRN_IP = "123451234512345";
    private static final String FIO = UUID.randomUUID().toString();
    private static final Date DATE = new Date();

    @Autowired
    private SparkManager sparkManager;
    @Autowired
    private SparkService sparkService;
    @Autowired
    private SparkService.SparkShopDataRepository sparkShopDataRepository;
    @Autowired
    private SparkService.SparkAffiliateRepository sparkAffiliateRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        jdbcTemplate.update("UPDATE core_config SET value = '0' WHERE id = " + CoreConfig.DONT_USE_SPARK.getId());
    }

    @Test
    public void testDAO() throws Exception {
        // ----------------------------
        // Эмулируем, что в БД уже сохранены какие-то данные
        // ----------------------------
        SparkShopData d = genShopData();
        // Если магазин является физ.лицом, manager_inn для него не существует, поэтому нужно использовать ИНН самого физ.лица
        String inn = d.isCompany() ? String.valueOf(RND.nextInt()) : d.getInn();
        if (d.isCompany()) {
            d.setManagerInn(inn);
        }
        int sparkId = d.getSparkId();
        sparkService.saveAffiliateWithShopData(genLeader(FIO, inn, cloneShopData(d), AffiliateType.LEADER));

        SparkShopData whereLeader1 = genShopData();
        sparkService.saveAffiliateWithShopData(genLeader(FIO, inn, cloneShopData(whereLeader1), AffiliateType.LEADER));

        SparkShopData whereLeader2 = genShopData();
        saveCloneShopData(whereLeader2);

        // ----------------------------
        // Формируем SparkShopData и всевозможные связи SparkAffiliate
        // ----------------------------
        d.setFullInfo(true);
        List<SparkAffiliate> affiliates = new ArrayList<>();
        // руководители и совладельцы
        affiliates.add(genLeader(FIO, inn, d, AffiliateType.LEADER));
        affiliates.add(genLeader(FIO + "2", inn + "2", d, AffiliateType.LEADER_HISTORY));
        affiliates.add(genLeader(FIO + "3", inn + "3", d, AffiliateType.LEADER_HISTORY));

        affiliates.add(genLeader(FIO + "4", inn + "4", d, AffiliateType.COOWNER));
        affiliates.add(genLeader(FIO + "5", inn + "5", d, AffiliateType.COOWNER));
        affiliates.add(genLeader(FIO + "6", inn + "6", d, AffiliateType.COOWNER_HISTORY));

        d.setAffiliates(affiliates);

        List<SparkAffiliate> affiliateByManager = new ArrayList<>();
        // где текущий руководитель, является руководителем
        affiliateByManager.add(genLeader(FIO, inn, whereLeader1, AffiliateType.LEADER));
        String ogrn = "12345";
        whereLeader2.setOgrn(ogrn);
        affiliateByManager.add(genLeader(FIO, inn, whereLeader2, AffiliateType.LEADER));
        SparkShopData whereLeaderHistory1 = genShopData();
        affiliateByManager.add(genLeader(FIO, inn, whereLeaderHistory1, AffiliateType.LEADER_HISTORY));

        // где текущий руководитель, является совладельцем
        SparkShopData whereCoowner1 = genShopData();
        affiliateByManager.add(genLeader(FIO, inn, whereCoowner1, AffiliateType.COOWNER));
        affiliateByManager.add(genLeader(FIO, inn, whereLeader1, AffiliateType.COOWNER));
        SparkShopData whereCoownerHistory1 = genShopData();
        affiliateByManager.add(genLeader(FIO, inn, whereCoownerHistory1, AffiliateType.COOWNER_HISTORY));
        affiliateByManager.add(genLeader(FIO, inn, whereLeaderHistory1, AffiliateType.COOWNER_HISTORY));

        d.setAffiliateByManager(affiliateByManager);

        // ----------------------------
        // Сохраняем и проверяем
        // ----------------------------
        SparkShopData saved = sparkService.save(d);
        assertShopData(saved);

        List<SparkShopData> allData = sparkShopDataRepository.findAll();
        assertEquals(6, allData.size());
        assertEquals(affiliates.size() + affiliateByManager.size(), sparkAffiliateRepository.findAll().size());

        SparkShopData loaded = allData.stream()
                .filter(s -> s.getSparkId().equals(sparkId))
                .findFirst().orElse(null);
        assertShopData(loaded);

        SparkShopData shopData = allData.stream()
                .filter(s -> s.getSparkId().equals(whereLeader2.getSparkId()))
                .findFirst().orElse(null);
        assertNotNull(shopData);
        assertNotEquals("Не должны перетирать уже сохранённое в БД значение для SparkShopData", ogrn, shopData.getOgrn());

        List<SparkAffiliate> byManagerInn = sparkService.findByManagerInn(inn);
        assertNotNull(byManagerInn);
        assertEquals(8, byManagerInn.size());
        assertNotNull(byManagerInn.get(0).getSparkShopData());
    }

    @Test
    void testSavingLinkedEntities() {
        var shopData = genShopData();
        var frozenAccounts = genFrozenAccounts(shopData);
        shopData.setFrozenAccounts(frozenAccounts);

        var savedShopData = sparkService.save(shopData);
        assertEquals(frozenAccounts, savedShopData.getFrozenAccounts());
    }

    private void saveCloneShopData(SparkShopData d) {
        sparkShopDataRepository.save(cloneShopData(d));
    }

    private SparkShopData cloneShopData(SparkShopData d) {
        SparkShopData a = new SparkShopData();
        a.setSparkId(d.getSparkId());
        a.setManagerFio(d.getManagerFio());
        a.setActing(d.isActing());
        a.setFullName(d.getFullName());
        a.setOgrn(d.getOgrn());
        a.setInn(d.getInn());
        return a;
    }

    public SparkShopData genShopData() {
        SparkShopData d = new SparkShopData(RND.nextInt());
        d.setManagerFio(FIO);
        d.setActing(true);
        d.setFullName("Company_" + FIO);
        d.setOgrn(String.valueOf(RND.nextInt()));
        d.setInn(String.valueOf(RND.nextInt()));
        return d;
    }

    public void assertShopData(SparkShopData saved) {
        assertNotNull(saved);
        assertEquals(FIO, saved.getManagerFio());
        assertEquals(6, saved.getAffiliates().size());
        assertTrue(saved.isFullInfo());
    }

    public SparkAffiliate genLeader(String fio, String inn, SparkShopData d, AffiliateType type) {
        SparkAffiliate leader = new SparkAffiliate();
        leader.setManagerFio(fio);
        leader.setManagerInn(inn);
        leader.setSparkShopData(d);
        leader.setAffiliateType(type);
        leader.setActualDate(DATE);
        return leader;
    }

    private static Set<SparkFrozenAccount> genFrozenAccounts(SparkShopData sparkShopData) {
        var frozenAccount = mock(FrozenAccountDecision.class);

        var taxAuthority = mock(FrozenAccountDecision.TaxAuthority.class);
        when(taxAuthority.getCode()).thenReturn(Long.toString(RND.nextLong()));
        when(taxAuthority.getName()).thenReturn(RandomStringUtils.random(7));

        var bank = mock(FrozenAccountDecision.Bank.class);
        when(bank.getBIK()).thenReturn(RandomStringUtils.random(7));
        when(bank.getName()).thenReturn(RandomStringUtils.random(7));

        when(frozenAccount.getDate()).thenReturn(DateUtil.asDate(LocalDateTime.now()));
        when(frozenAccount.getNumber()).thenReturn(Long.toString(RND.nextLong()));
        when(frozenAccount.getBank()).thenReturn(bank);
        when(frozenAccount.getTaxAuthority()).thenReturn(taxAuthority);

        return Set.of(new SparkFrozenAccount(sparkShopData.getSparkId(), frozenAccount));
    }

    /**
     * Проверяем, что при записи существующего shopData
     * корректно перезаписываются связанные сущности, на примере телефонов
     */
    @Test
    public void testPhones() {
        var data = sparkManager.loadFromSparkAndUpdate(TEST_OGRN_OOO);
        assertEquals(8, data.getPhones().size());
        flushAndClear();

        data.getPhones().remove(data.getPhones().iterator().next());
        sparkService.save(data);
        flushAndClear();

        data = sparkManager.loadFromSparkAndUpdate(TEST_OGRN_OOO);
        assertEquals(7, data.getPhones().size());

        data.setUploadedDate(LocalDateTime.now().minusYears(2));
        sparkService.save(data);
        flushAndClear();

        data = sparkManager.loadFromSparkAndUpdate(TEST_OGRN_OOO);
        assertEquals(8, data.getPhones().size());
    }

    @Test
    public void testParseSaveAndDeleteCompany() throws Exception {
        String ogrn = TEST_OGRN_OOO;
        SparkShopData loadedData = sparkService.findByOgrn(ogrn);
        assertNull(loadedData);

        SparkShopData data = sparkManager.loadFromSparkAndUpdate(ogrn);
        assertCompanyData(data);

        loadedData = sparkService.findOne(data.getSparkId());
        assertCompanyData(loadedData);

        // кроме этой компании, сохранены компании, в которых является руководителем/ основателем
        // GetCompanyListByPersonINN-5555555555555.xml
        // LEADER  -> 55555, 11111, 41111
        // COOWNER -> 55555, 11111, 21111, 31111
        List<Integer> sparkIds = Arrays.asList(11111, 21111, 31111, 41111);
        List<Integer> savedSparkIds = sparkService.findBySparkIdIn(sparkIds);
        // временно изменили в рамках MARKETASSESSOR-9242
        assertEquals(0, savedSparkIds.size());

        // а для этих компаний сохранены руководители/ основатели
        for (Integer savedSparkId : savedSparkIds) {
            // sparkService.findOne(savedSparkId).getAffiliates().stream()
            assertTrue(sparkAffiliateRepository.findBySparkShopData_SparkId(savedSparkId).stream()
                    .filter(a -> a.getManagerFio().equals(data.getManagerFio()))
                    .filter(a -> a.getAffiliateType() == AffiliateType.COOWNER ||
                            a.getAffiliateType() == AffiliateType.LEADER).count() > 0);
        }

        // удаляем эту компанию
        sparkService.deleteSparkShopData(loadedData.getSparkId());
        assertNull(sparkService.findByOgrn(ogrn));

        // удаляем зависимые компании
        savedSparkIds.forEach(id -> sparkService.deleteSparkShopData(id));
    }

    private void assertCompanyData(SparkShopData data) {
        assertNotNull(data);

        assertEquals(Integer.valueOf(55555555), data.getSparkId());

        assertEquals("Иванов Иван Иванович", data.getManagerFio());
        assertEquals(8, data.getPhones().size());

        assertNotNull(data.getLists());
        assertEquals(1, data.getLists().length);

        assertNotNull(data.getOkvedCodes());
        assertEquals(19, data.getOkvedCodes().length);

        assertNotNull(data.getLeaders());
        assertEquals(5, data.getLeaders().size());
        assertEquals(data.getManagerFio(), data.getActualLeader().getManagerFio());

        assertNotNull(data.getPersonWithoutWarrant());
        assertEquals(1, data.getPersonWithoutWarrant().size());

        // временно изменили в рамках MARKETASSESSOR-9242
        assertEquals(0, data.getCntCompaniesWhereManagerCoowner().intValue());
        assertEquals(0, data.getCntCompaniesWhereManagerLeader().intValue());

        assertEquals(8, data.getCoownerHistory().size());
        assertNotNull(data.getCoownerHistory().get(0).getShortName());

        assertEquals(3, data.getCoowners().size());
        assertNotNull(data.getCoowners().get(0).getShortName());

        assertEquals(11, data.getArbitrageStat().size());
        assertEquals(7, data.getStateContractStat().size());
    }

    @Test
    public void testParseSaveAndDeleteIp() {
        String ogrn = TEST_OGRN_IP;
        SparkShopData loadedData = sparkService.findByOgrn(ogrn);
        assertNull(loadedData);

        SparkShopData data = sparkManager.loadFromSparkAndUpdate(ogrn);
        assertIpData(data);

        // временно убрали в рамках MARKETASSESSOR-9242
        // List<SparkAffiliate> byManagerInn = sparkAffiliateRepository.findByManagerInn(data.getInn());

        loadedData = sparkService.findOne(data.getSparkId());
        assertIpData(loadedData);

        sparkService.deleteSparkShopData(loadedData.getSparkId());
        assertNull(sparkService.findByOgrn(ogrn));
    }

    public void assertIpData(SparkShopData data) {
        assertNotNull(data);

        Map<AffiliateType, List<SparkAffiliate>> typeListMap = data.getAffiliates().stream()
                .collect(Collectors.groupingBy(SparkAffiliate::getAffiliateType));
        assertEquals(1, data.getAffiliates().size());
        assertEquals(1, typeListMap.get(AffiliateType.LINKED).size());
    }

}
