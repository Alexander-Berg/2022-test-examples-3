package ru.yandex.chemodan.app.psbilling.core.utils.factories;

import java.util.Collection;
import java.util.UUID;

import lombok.AllArgsConstructor;
import org.joda.time.Duration;
import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.promocodes.PromoCodeDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.SafePromoCode;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

@AllArgsConstructor
public class PsBillingPromoFactory {
    PromoTemplateDao promoTemplateDao;
    UserPromoDao userPromoDao;
    PromoCodeDao promoCodeDAO;
    JdbcTemplate3 jdbcTemplate;

    public PromoTemplateEntity createPromo(Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        PromoTemplateDao.InsertData data = customizer.apply(PromoTemplateDao.InsertData.builder()
                .description("description")
                .code(UUID.randomUUID().toString())
                .fromDate(Instant.now().minus(Duration.standardHours(1)))
                .toDate(Option.of(Instant.now().plus(Duration.standardHours(1))))
                .applicationArea(PromoApplicationArea.GLOBAL)
                .applicationType(PromoApplicationType.ONE_TIME)
                .duration(Option.of(CustomPeriod.fromDays(1)))
        ).build();
        return promoTemplateDao.create(data);
    }

    public PromoCodeEntity createPromoCode(Function<PromoCodeDao.InsertData.InsertDataBuilder,
            PromoCodeDao.InsertData.InsertDataBuilder> customizer) {
        PromoCodeDao.InsertData insertData = customizer.apply(PromoCodeDao.InsertData.builder()
                .promoCodeType(PromoCodeType.B2C)
                .numActivations(Option.of(100500))
                .remainingActivations(Option.of(100400))
                .promoCodeStatus(PromoCodeStatus.ACTIVE)
                .statusUpdatedAt(Instant.now())
                .statusReason(Option.empty())
                .fromDate(Instant.now().minus(Duration.standardDays(1)))
                .toDate(Option.of(Instant.now().plus(Duration.standardDays(365 * 100))))
        ).build();
        if (insertData.getCodes().size() != 1) {
            throw new IllegalArgumentException("need exactly one promo code to return entity");
        }
        promoCodeDAO.create(insertData);
        return promoCodeDAO.findById(insertData.getCodes().get(0));
    }

    public PromoCodeEntity createPromoCodeProductPrice(String code, UUID productPriceId,
                                                       Function<PromoCodeDao.InsertData.InsertDataBuilder,
                                                               PromoCodeDao.InsertData.InsertDataBuilder> customizer) {
        return createPromoCode(c -> c.productPriceId(Option.of(productPriceId)).codes(Cf.list(SafePromoCode.cons(code))));
    }

    public PromoCodeEntity createPromoCodePromo(String code, UUID promoId,
                                                Function<PromoCodeDao.InsertData.InsertDataBuilder,
                                                        PromoCodeDao.InsertData.InsertDataBuilder> customizer) {
        return createPromoCode(c -> c.promoTemplateId(Option.of(promoId)).codes(Cf.list(SafePromoCode.cons(code))));
    }

    public UUID createFreeSpaceFeature(
            String code,
            String description,
            String line,
            String pid,
            boolean doSetAmount,
            boolean doDelete,
            boolean doSpecifyAmountOnCreate) {
        return UUID.fromString(jdbcTemplate.queryForObject("select create_mpfs_space_feature(?,?,?,?,?,?,?)",
                String.class,
                code,
                description,
                line,
                pid,
                doSetAmount,
                doDelete,
                doSpecifyAmountOnCreate)
        );
    }

    public UUID createFreeSpaceProduct(
            String code,
            UUID titleTankerId,
            CustomPeriod period,
            Long amount,
            String spaceFeatureCode) {
        return UUID.fromString(jdbcTemplate.queryForObject("select create_free_space_product(?,?,?,?,?,?)",
                String.class,
                code,
                titleTankerId,
                period.getUnit().value(),
                period.getValue(),
                amount,
                spaceFeatureCode)
        );
    }

    public void bindToProductLines(UUID promoId, UUID...lineIds){
        bindToProductLines(promoId, Cf.list(lineIds));
    }

    public void bindToProductLines(UUID promoId, Collection<UUID> lineIds){
        promoTemplateDao.bindProductLines(promoId, lineIds);
    }


}
