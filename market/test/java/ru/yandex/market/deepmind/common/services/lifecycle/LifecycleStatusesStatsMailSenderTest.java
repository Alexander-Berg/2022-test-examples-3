package ru.yandex.market.deepmind.common.services.lifecycle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.thymeleaf.spring5.SpringTemplateEngine;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.DeepmindMailSenderHelper;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditRecorder;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.services.mail.EmailService;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.users.UserRepository;

import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATDIR;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;

public class LifecycleStatusesStatsMailSenderTest extends DeepmindBaseDbTestClass {

    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource(name = "deepmindTransactionHelper")
    protected TransactionHelper transactionHelper;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private UserRepository userRepository;
    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private LifecycleStatusesStatsCalculator lifecycleStatusesStatsCalculator;
    private LifecycleStatusesStatsMailSender lifecycleStatusesStatsMailSender;

    private MboAuditServiceMock mboAuditServiceMock;
    private StorageKeyValueServiceMock keyValService;
    private DeepmindMailSenderHelper mailHelperSpy;
    private CategoryManagerTeamService categoryManagerTeamService;

    @Before
    public void setUp() {
        mboAuditServiceMock = new MboAuditServiceMock();
        var mskuStatusAuditService = new MskuStatusAuditService(mboAuditServiceMock);
        var mskuStatusAuditRecorder = new MskuStatusAuditRecorder(mboAuditServiceMock,
            Mockito.mock(MboUsersRepository.class));
        mskuStatusAuditRecorder.setAuditEnabled(true);
        mskuStatusRepository = new MskuStatusRepository(dsl);
        mskuStatusRepository.addObserver(mskuStatusAuditRecorder);

        var yqlAutoClusterMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(yqlAutoClusterMock.query(Mockito.anyString(), Mockito.anyMap(),
            Mockito.any(ResultSetExtractor.class))).thenReturn(List.of());
        categoryManagerTeamService = new CategoryManagerTeamService(deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository, new DeepmindCategoryCachingServiceMock());
        lifecycleStatusesStatsCalculator = new LifecycleStatusesStatsCalculator(namedParameterJdbcTemplate,
            deepmindMskuRepository, serviceOfferReplicaRepository, deepmindCategoryRepository,
            categoryManagerTeamService, mskuStatusAuditService, userRepository, yqlAutoClusterMock,
            YPath.simple("//tmp"), "pool");
        seasonRepository.save(new Season().setId(111L).setName("season_111"));

        keyValService = new StorageKeyValueServiceMock();
        mailHelperSpy = Mockito.spy(new DeepmindMailSenderHelper(keyValService, Mockito.mock(EmailService.class)));
        lifecycleStatusesStatsMailSender = new LifecycleStatusesStatsMailSender(lifecycleStatusesStatsCalculator,
            deepmindCategoryManagerRepository, keyValService, mailHelperSpy, new SpringTemplateEngine());
    }


    @Test
    public void mailSenderPartlySendTest() {
        // prepare data
        mboAuditServiceMock.reset();
        deepmindMskuRepository.save(msku(111L, 111L), msku(222L, 222L), msku(333L, 333L), msku(444L, 444L));
        mskuStatusRepository.save(
            mskuStatus(111L, MskuStatusValue.ARCHIVE),
            mskuStatus(222L, MskuStatusValue.REGULAR),
            mskuStatus(333L, MskuStatusValue.END_OF_LIFE),
            mskuStatus(444L, MskuStatusValue.ARCHIVE)
        );
        deepmindCategoryRepository.insertBatch(category(111L), category(222L), category(333L), category(444L));
        deepmindSupplierRepository.save(supplier(111), supplier(222), supplier(333), supplier(3333), supplier(444));
        serviceOfferReplicaRepository.save(
            offer(111, "ssku-111", 111L, 111),
            offer(222, "ssku-222", 222L, 222),
            offer(333, "ssku-333", 333L, 333),
            offer(3333, "ssku-3333", 333L, 333),
            offer(444, "ssku-444", 444L, 444)
        );
        deepmindCategoryManagerRepository.save(
            categoryManager(111, "catman_111"),
            categoryManager(333, "catman_333"),
            categoryManager(444, "catdir_444").setRole(CATDIR)
        );
        // return 2 msku to sale
        var statuses = mskuStatusRepository.findAllMap();
        mskuStatusRepository.save(
            statuses.get(111L).setMskuStatus(MskuStatusValue.REGULAR),
            statuses.get(333L).setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(111L),
            statuses.get(444L).setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(111L)
        );
        AtomicInteger count = new AtomicInteger();
        var processedLogins = new ArrayList<String>();
        Mockito.doAnswer(inv -> {
            var address = (String) inv.getArgument(0);
            var login = address.substring(0, address.indexOf("@"));
            if (count.get() <= 1) {
                processedLogins.add(login);
                count.incrementAndGet();
                return inv.callRealMethod();
            } else {
                throw new RuntimeException("Test email service error");
            }
        }).when(mailHelperSpy).sendMail(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString());
        Assertions
            .assertThatThrownBy(() -> lifecycleStatusesStatsMailSender.sendStatsMail())
            .hasMessageContaining("Test email service error");
        Assertions
            .assertThat(processedLogins)
            .containsExactly("catdir_444", "catman_111");
        Assertions
            .assertThat(keyValService.getInstant("lifecycle_statuses_stats_mail_last_sent", Instant.MIN))
            .isBefore(keyValService.getInstant("lifecycle_statuses_stats_mail_next_last_sent", Instant.MIN));
        count.set(0);
        processedLogins.clear();
        lifecycleStatusesStatsMailSender.sendStatsMail();
        Assertions
            .assertThat(processedLogins)
            .containsExactly("catman_333");
        Assertions
            .assertThat(keyValService.getInstant("lifecycle_statuses_stats_mail_last_sent", null))
            .isEqualTo(keyValService.getInstant("lifecycle_statuses_stats_mail_next_last_sent", null));
    }


    private Supplier supplier(Integer id) {
        return new Supplier().setId(id).setName(id.toString()).setSupplierType(SupplierType.FIRST_PARTY);
    }

    private CategoryManager categoryManager(long categoryId, String login) {
        return new CategoryManager()
            .setCategoryId(categoryId)
            .setStaffLogin(login)
            .setRole(CATMAN)
            .setFirstName(login)
            .setLastName(login);
    }

    private Category category(Long id) {
        return new Category()
            .setCategoryId(id)
            .setName("name_" + id);
    }

    private Msku msku(long id) {
        return new Msku()
            .setId(id)
            .setTitle("Msku #" + id)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private Msku msku(long id, long categoryId) {
        return msku(id)
            .setCategoryId(categoryId);
    }

    private MskuStatus mskuStatus(long mskuId, MskuStatusValue status) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(status)
            .setStatusStartAt(Instant.now())
            .setNpdStartDate(LocalDate.now());
        if (status == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        if (status == MskuStatusValue.SEASONAL) {
            mskuStatus.setSeasonId(111L);
        }
        if (status == MskuStatusValue.IN_OUT) {
            mskuStatus.setInoutStartDate(LocalDate.now());
            mskuStatus.setInoutFinishDate(LocalDate.now().plusDays(60));
        }
        return mskuStatus;
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, long mskuId, long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.FIRST_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
