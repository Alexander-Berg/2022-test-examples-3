package ru.yandex.market.loyalty.core.test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.core.dao.accounting.AccountDao;
import ru.yandex.market.loyalty.core.model.DataVersion;
import ru.yandex.market.loyalty.core.model.accounting.Account;
import ru.yandex.market.loyalty.core.model.accounting.AccountMatter;
import ru.yandex.market.loyalty.core.model.delivery.AddressType;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.service.BudgetService;
import ru.yandex.market.loyalty.core.service.CategoryTreeService;
import ru.yandex.market.loyalty.core.service.FreeDeliveryAddressService;
import ru.yandex.market.loyalty.core.utils.CoreCollectionUtils;

@Component
public class SupplementaryDataLoader {
    public static final int PARENT_CATEGORY_ID = 1000;
    public static final int FIRST_CHILD_CATEGORY_ID = 1001;
    public static final int SECOND_CHILD_CATEGORY_ID = 1002;
    public static final int PHARMA_ROOT_CATEGORY_ID = 8475840;
    public static final int PHARMA_BUD_CATEGORY_ID = 15754673;
    public static final int PHARMA_LIST_CATEGORY_ID = 16089018;
    public static final int PHARMA_VITAMINS_AND_MINERALS_CATEGORY_ID = 90521;
    public static final int PHARMA_CHILD_CATEGORY = 15756357;
    public static final int SUPPLIER_EXCLUSION_ID = 1095644;
    public static final int STICK_CATEGORY = 16440100;
    public static final int ALCOHOL_CATEGORY = 16155381;
    public static final int ALCOHOL_CHILD_CATEGORY = 16155647;
    public static final int WARMER_CATEGORY = 16440108;
    public static final int VAPORIZER_CATEGORY = 16761723;
    public static final int VAPORIZER_LIQUID_CATEGORY = 16761766;
    private static final int ROOT_CATEGORY_ID = 0;

    private final AccountDao accountDao;
    private final BudgetService budgetService;
    private final JdbcTemplate jdbcTemplate;
    private final CategoryTreeService categoryTreeService;
    private final FreeDeliveryAddressService freeDeliveryAddressService;

    public SupplementaryDataLoader(
            AccountDao accountDao, BudgetService budgetService, JdbcTemplate jdbcTemplate,
            CategoryTreeService categoryTreeService, FreeDeliveryAddressService freeDeliveryAddressService
    ) {
        this.accountDao = accountDao;
        this.budgetService = budgetService;
        this.jdbcTemplate = jdbcTemplate;
        this.categoryTreeService = categoryTreeService;
        this.freeDeliveryAddressService = freeDeliveryAddressService;
    }

    public void createTechnicalIfNotExists() {
        Arrays.stream(AccountMatter.values()).forEach(matter -> {
            try {
                accountDao.getTechnicalAccountId(matter);
            } catch (Exception e) {
                accountDao.createTechnicalAccount(matter, BigDecimal.valueOf(Integer.MAX_VALUE));
            }
        });
    }

    public void createEmptyOperationContext() {
        jdbcTemplate.update("INSERT INTO operation_context (id) VALUES (0) ON CONFLICT (id) DO NOTHING ");
    }

    public void createReserveIfNotExists(BigDecimal reserveAmount) {
        Account reserveAccount;
        long technicalAccountId = accountDao.getTechnicalAccountId(AccountMatter.MONEY);
        try {
            reserveAccount = accountDao.getReserveAccount();
            if (reserveAccount.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                budgetService.performSingleTransaction(reserveAccount.getBalance(),
                        reserveAccount.getId(), technicalAccountId, BudgetMode.SYNC,
                        MarketLoyaltyErrorCode.BUDGET_EXCEEDED
                );
            }
        } catch (Exception e) {
            accountDao.createReserveAccount();
            reserveAccount = accountDao.getReserveAccount();
        }
        budgetService.performSingleTransaction(reserveAmount,
                technicalAccountId, reserveAccount.getId(), BudgetMode.SYNC,
                MarketLoyaltyErrorCode.BUDGET_EXCEEDED
        );
    }

    public void populateCategoryTree() {
        jdbcTemplate.update("DELETE FROM data_version WHERE id='CATEGORY_TREE'");
        jdbcTemplate.update("DELETE FROM category_tree");

        Long version = jdbcTemplate.queryForObject(
                "" +
                        "INSERT INTO data_version(id, num) " +
                        "VALUES ('" + DataVersion.CATEGORY_TREE.getCode() + "', nextval('data_version_num_seq'))" +
                        "RETURNING num",
                Long.class
        );

        List<CategoryNode> baseCategories = Stream.of(
                STICK_CATEGORY,
                WARMER_CATEGORY,
                ALCOHOL_CATEGORY,
                VAPORIZER_CATEGORY,
                VAPORIZER_LIQUID_CATEGORY,
                123, 1123, 12312, 879465459, 100, 400, 123141412, 12312131, 200, 500,
                600, 12644434, 12312, 456,
                1, 2, 123213, 879465459, 12312131
        )
                .distinct()
                .map(cat -> new CategoryNode(cat, ROOT_CATEGORY_ID))
                .collect(Collectors.toList());
        CategoryNode rootCategory = new CategoryNode(ROOT_CATEGORY_ID, null);
        List<CategoryNode> treeSample = Arrays.asList(
                new CategoryNode(PARENT_CATEGORY_ID, ROOT_CATEGORY_ID),
                new CategoryNode(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID),
                new CategoryNode(SECOND_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID)
        );
        List<CategoryNode> pharmaCategories = Arrays.asList(
                new CategoryNode(PHARMA_ROOT_CATEGORY_ID, ROOT_CATEGORY_ID),
                new CategoryNode(PHARMA_LIST_CATEGORY_ID, PHARMA_ROOT_CATEGORY_ID),
                new CategoryNode(PHARMA_CHILD_CATEGORY, PHARMA_ROOT_CATEGORY_ID),
                new CategoryNode(PHARMA_BUD_CATEGORY_ID, PHARMA_ROOT_CATEGORY_ID),
                new CategoryNode(PHARMA_VITAMINS_AND_MINERALS_CATEGORY_ID, PHARMA_ROOT_CATEGORY_ID)
        );
        List<CategoryNode> alcoCategories = Arrays.asList(
                new CategoryNode(ALCOHOL_CHILD_CATEGORY, ALCOHOL_CATEGORY)
        );
        jdbcTemplate.batchUpdate(
                "" +
                        "INSERT INTO category_tree(version_num, hid, parent_hid, name) VALUES " +
                        "(?, ?, ?, ?)",
                CoreCollectionUtils.union(
                        baseCategories,
                        Collections.singletonList(rootCategory),
                        treeSample,
                        pharmaCategories,
                        alcoCategories
                )
                        .stream()
                        .map(node -> new Object[]{version, node.getHid(), node.getParentHid(),
                                "category_" + node.getHid()})
                        .collect(Collectors.toList())
        );

        categoryTreeService.refreshCategoryTree();
    }

    public void populateFreeDeliveryAddresses() {
        jdbcTemplate.update("DELETE FROM free_delivery_address");

        jdbcTemplate.update("" +
                "INSERT INTO free_delivery_address(address, longitude, latitude, address_type) VALUES " +
                "('г. Москва, ул. Садовническая, 82с2','37.642474','55.735520','" + AddressType.YANDEX.getCode() + "');"
        );

        freeDeliveryAddressService.refreshFreeDeliveryAddresses();
    }

    public void createDefaultCashbackDetailsGroup() {
        // ON CONFLICT для MarketLoyaltyBackProdDataMockedDbTest который накатывает продовые данные
        jdbcTemplate.update("INSERT INTO cashback_details_group(id, name, title, status) VALUES (0, 'default', " +
                "'Стандартный кешбэк', 'ACTIVE') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO cashback_details_group(id, name, title, status) VALUES (1, 'extra', " +
                "'Повышенный кешбэк', 'ACTIVE') ON CONFLICT DO NOTHING");
        jdbcTemplate.update("INSERT INTO cashback_details_group(id, name, title, status) VALUES (2, 'partner_extra', 'Повышенный кешбэк от продавца', 'ACTIVE') ON CONFLICT DO NOTHING");
    }

    private static class CategoryNode {
        private final int hid;
        private final Integer parentHid;

        private CategoryNode(int hid, Integer parentHid) {
            this.hid = hid;
            this.parentHid = parentHid;
        }

        public int getHid() {
            return hid;
        }

        public Integer getParentHid() {
            return parentHid;
        }
    }
}
