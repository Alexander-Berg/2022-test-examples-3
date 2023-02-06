package ru.yandex.chemodan.app.psbilling.core.utils;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.joda.time.Instant;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.group.GroupPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoStatusType;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.group.GroupPromoStatusType;
import ru.yandex.inside.passport.PassportUid;

@RequiredArgsConstructor
public class PromoHelper {
    private final PromoTemplateDao promoTemplateDao;
    private final UserPromoDao userPromoDao;
    private final GroupPromoDao groupPromoDao;


    public PromoTemplateEntity createGlobalPromo() {
        return createGlobalPromo(UUID.randomUUID().toString());
    }

    public PromoTemplateEntity createGlobalPromo(String code) {
        return createGlobalPromo(code, Function.identityF());
    }

    public PromoTemplateEntity createGlobalPromo(Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        return createGlobalPromo(UUID.randomUUID().toString(), customizer);
    }

    public PromoTemplateEntity createPromo(PromoApplicationArea area) {
        return createPromo(UUID.randomUUID().toString(), area, x -> x);
    }

    public PromoTemplateEntity createPromo(PromoApplicationArea area,
                                           Function<PromoTemplateDao.InsertData.InsertDataBuilder,
                                                   PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        return createPromo(UUID.randomUUID().toString(), area, customizer);
    }

    public PromoTemplateEntity createPromo(String code, PromoApplicationArea area,
                                           Function<PromoTemplateDao.InsertData.InsertDataBuilder,
                                                   PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        return promoTemplateDao.create(createSimplePromoBuilder(code,
                b -> customizer.apply(b).applicationArea(area)));
    }

    public PromoTemplateEntity createGlobalPromo(String code, Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        return promoTemplateDao.create(createSimplePromoBuilder(code, customizer));
    }

    public PromoTemplateEntity createUserPromo(Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer) {
        return createGlobalPromo(UUID.randomUUID().toString(),
                b -> customizer.apply(b).applicationArea(PromoApplicationArea.PER_USER));
    }

    public PromoTemplateEntity createUserPromo() {
        return createGlobalPromo(UUID.randomUUID().toString(), b -> b.applicationArea(PromoApplicationArea.PER_USER));
    }

    private PromoTemplateDao.InsertData createSimplePromoBuilder(
            String code, Function<PromoTemplateDao.InsertData.InsertDataBuilder,
            PromoTemplateDao.InsertData.InsertDataBuilder> customizer
    ) {
        PromoTemplateDao.InsertData.InsertDataBuilder builder = PromoTemplateDao.InsertData.builder()
                .description("description")
                .code(code)
                .fromDate(Instant.now())
                .applicationArea(PromoApplicationArea.GLOBAL)
                .applicationType(PromoApplicationType.ONE_TIME);
        return customizer.apply(builder).build();
    }

    public PromoTemplateDao.InsertData.InsertDataBuilder getPromoTemplateBuilder() {
        return PromoTemplateDao.InsertData.builder()
                .description("description")
                .code("some_code")
                .fromDate(DateUtils.pastDate())
                .toDate(Option.of(DateUtils.futureDate()))
                .applicationArea(PromoApplicationArea.GLOBAL)
                .applicationType(PromoApplicationType.ONE_TIME)
                .duration(Option.of(CustomPeriod.fromDays(1)));
    }

    public void setUserPromoStatus(PassportUid userUid, UUID promoId, Instant from, Option<Instant> to,
                                   PromoStatusType promoStatusType) {
        UserPromoDao.InsertData data = UserPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .uid(userUid)
                .fromDate(from)
                .toDate(to)
                .promoStatusType(promoStatusType)
                .build();

        userPromoDao.createOrUpdate(data);
    }

    public void setGroupPromoStatus(
            Group group,
            UUID promoId,
            Instant from,
            Option<Instant> to,
            GroupPromoStatusType status) {
        GroupPromoDao.InsertData data = GroupPromoDao.InsertData.builder()
                .promoTemplateId(promoId)
                .groupId(group.getId())
                .fromDate(from)
                .toDate(to)
                .status(status)
                .build();

        groupPromoDao.createIfNotExist(data);
    }
}
