package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgramCategory;
import ru.yandex.direct.core.entity.cashback.repository.CashbackCategoriesRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsCashbackDetails.CLIENTS_CASHBACK_DETAILS;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsCashbackHistory.CLIENTS_CASHBACK_HISTORY;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsCashbackPrograms.CLIENTS_CASHBACK_PROGRAMS;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsOptions.CLIENTS_OPTIONS;
import static ru.yandex.direct.dbschema.ppcdict.tables.CashbackCategories.CASHBACK_CATEGORIES;
import static ru.yandex.direct.dbschema.ppcdict.tables.CashbackPrograms.CASHBACK_PROGRAMS;
import static ru.yandex.direct.dbschema.ppcdict.tables.CashbackProgramsCategories.CASHBACK_PROGRAMS_CATEGORIES;

@Component
public class CashbackSteps {
    private final ShardHelper shardHelper;
    private final DslContextProvider dslContextProvider;
    private final CashbackCategoriesRepository cashbackCategoriesRepository;

    @Autowired
    public CashbackSteps(ShardHelper shardHelper,
                         DslContextProvider dslContextProvider,
                         CashbackCategoriesRepository cashbackCategoriesRepository) {
        this.shardHelper = shardHelper;
        this.dslContextProvider = dslContextProvider;
        this.cashbackCategoriesRepository = cashbackCategoriesRepository;
    }

    public CashbackCategory getCategory(Long id) {
        return cashbackCategoriesRepository.get(id);
    }

    public void createCategory(CashbackCategory category) {
        var id = dslContextProvider.ppcdict()
                .insertInto(CASHBACK_CATEGORIES)
                .columns(CASHBACK_CATEGORIES.NAME_RU, CASHBACK_CATEGORIES.NAME_EN,
                        CASHBACK_CATEGORIES.DESCRIPTION_RU, CASHBACK_CATEGORIES.DESCRIPTION_EN,
                        CASHBACK_CATEGORIES.BUTTON_LINK, CASHBACK_CATEGORIES.BUTTON_TEXT_RU,
                        CASHBACK_CATEGORIES.BUTTON_TEXT_EN)
                .values(category.getNameRu(), category.getNameEn(),
                        category.getDescriptionRu(), category.getDescriptionEn(), category.getButtonLink(),
                        category.getButtonTextRu(), category.getButtonTextEn())
                .returningResult(CASHBACK_CATEGORIES.CASHBACK_CATEGORY_ID)
                .fetchOne();
        category.setId(id.value1());
    }

    public void createTechnicalCategory() {
        var category = getTechnicalCategory();
        dslContextProvider.ppcdict()
                .insertInto(CASHBACK_CATEGORIES)
                .columns(CASHBACK_CATEGORIES.CASHBACK_CATEGORY_ID, CASHBACK_CATEGORIES.NAME_RU, CASHBACK_CATEGORIES.NAME_EN,
                        CASHBACK_CATEGORIES.DESCRIPTION_RU, CASHBACK_CATEGORIES.DESCRIPTION_EN)
                .values(category.getId(), category.getNameRu(), category.getNameEn(),
                        category.getDescriptionRu(), category.getDescriptionEn())
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void createProgram(CashbackProgram program) {
        var id = dslContextProvider.ppcdict()
                .insertInto(CASHBACK_PROGRAMS)
                .columns(CASHBACK_PROGRAMS.CASHBACK_CATEGORY_ID,
                        CASHBACK_PROGRAMS.PERCENT, CASHBACK_PROGRAMS.IS_ENABLED, CASHBACK_PROGRAMS.IS_GENERAL)
                .values(program.getCategoryId(), program.getPercent(),
                        booleanToLong(program.getIsEnabled()), booleanToLong(program.getIsPublic()))
                .returningResult(CASHBACK_PROGRAMS.CASHBACK_PROGRAM_ID)
                .fetchOne();
        program.setId(id.value1());
    }

    public void createProgramCategoryLink(CashbackProgramCategory link) {
        dslContextProvider.ppcdict()
                .insertInto(CASHBACK_PROGRAMS_CATEGORIES)
                .columns(CASHBACK_PROGRAMS_CATEGORIES.LINK_ID, CASHBACK_PROGRAMS_CATEGORIES.CASHBACK_CATEGORY_ID,
                        CASHBACK_PROGRAMS_CATEGORIES.CASHBACK_PROGRAM_ID, CASHBACK_PROGRAMS_CATEGORIES.ORDER)
                .values(link.getId(), link.getCategoryId(), link.getProgramId(), link.getOrder())
                .execute();
    }

    public void createTechnicalProgram() {
        var program = getTechnicalProgram();
        dslContextProvider.ppcdict()
                .insertInto(CASHBACK_PROGRAMS)
                .columns(CASHBACK_PROGRAMS.CASHBACK_PROGRAM_ID, CASHBACK_PROGRAMS.CASHBACK_CATEGORY_ID,
                        CASHBACK_PROGRAMS.PERCENT, CASHBACK_PROGRAMS.IS_ENABLED, CASHBACK_PROGRAMS.IS_GENERAL)
                .values(program.getId(), program.getCategoryId(), program.getPercent(),
                        booleanToLong(program.getIsEnabled()), booleanToLong(program.getIsPublic()))
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void updateConsumedCashback(ClientId clientId, BigDecimal bonus) {
        int shard = shardHelper.getShardByClientIdStrictly(clientId);
        dslContextProvider.ppc(shard)
                .update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CASHBACK_BONUS, bonus)
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    public void updateAwaitingCashback(ClientId clientId, BigDecimal bonus) {
        int shard = shardHelper.getShardByClientIdStrictly(clientId);
        dslContextProvider.ppc(shard)
                .update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CASHBACK_AWAITING_BONUS, bonus)
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    public void addRewardDetails(ClientId clientId,
                                 Long programId,
                                 BigDecimal reward,
                                 BigDecimal rewardWithoutNds,
                                 LocalDate date) {
        var entryId = shardHelper.generateClientCashbackDetailsIds(1).get(0);
        int shard = shardHelper.getShardByClientIdStrictly(clientId);

        dslContextProvider.ppc(shard)
                .insertInto(CLIENTS_CASHBACK_DETAILS)
                .columns(CLIENTS_CASHBACK_DETAILS.CLIENT_CASHBACK_DETAILS_ID,
                        CLIENTS_CASHBACK_DETAILS.CASHBACK_PROGRAM_ID,
                        CLIENTS_CASHBACK_DETAILS.CLIENT_ID,
                        CLIENTS_CASHBACK_DETAILS.REWARD,
                        CLIENTS_CASHBACK_DETAILS.REWARD_WO_NDS,
                        CLIENTS_CASHBACK_DETAILS.REWARD_DATE)
                .values(entryId, programId, clientId.asLong(), reward, rewardWithoutNds, date.atStartOfDay())
                .execute();
    }

    public void clear() {
        dslContextProvider.ppcdict().truncate(CASHBACK_PROGRAMS).execute();
        dslContextProvider.ppcdict().truncate(CASHBACK_CATEGORIES).execute();
        dslContextProvider.ppcdict().truncate(CASHBACK_PROGRAMS_CATEGORIES).execute();

        shardHelper.forEachShard(shard -> {
            dslContextProvider.ppc(shard).truncate(CLIENTS_CASHBACK_PROGRAMS).execute();
            dslContextProvider.ppc(shard).truncate(CLIENTS_CASHBACK_HISTORY).execute();
        });
    }

    public void createTechnicalEntities() {
        createTechnicalCategory();
        createTechnicalProgram();
    }

    public static CashbackCategory getTechnicalCategory() {
        return new CashbackCategory()
                .withId(1L)
                .withNameRu("Программа лояльности")
                .withNameEn("Loyalty program")
                .withDescriptionRu("Определяет участие клиента в программе лояльности")
                .withDescriptionEn("Defines clients participation in loyalty program");
    }

    public static CashbackProgram getTechnicalProgram() {
        var technicalCategory = getTechnicalCategory();
        return new CashbackProgram()
                .withId(1L)
                .withPercent(BigDecimal.ZERO)
                .withIsPublic(true)
                .withIsEnabled(true)
                .withCategoryId(technicalCategory.getId())
                .withCategoryNameRu(technicalCategory.getNameRu())
                .withCategoryNameEn(technicalCategory.getNameEn())
                .withCategoryDescriptionRu(technicalCategory.getDescriptionRu())
                .withCategoryDescriptionEn(technicalCategory.getDescriptionEn());
    }
}
