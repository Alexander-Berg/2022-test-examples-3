package ru.yandex.market.pers.grade.web.grade;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ru.yandex.market.pers.grade.client.dto.grade.GradeResponseDto;
import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.ugc.PhotoService;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;
import ru.yandex.market.pers.grade.ugc.MultifactorGradeService;

@Service
public class GradeCreationHelper {

    @Autowired
    GradeModeratorModificationProxy moderatorModificationProxy;
    @Autowired
    private MultifactorGradeService multifactorGradeService;
    @Autowired
    private PhotoService photoService;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @NotNull
    public static ShopGrade constructShopGrade(long shopId, long authorId) {
        ShopGrade grade = new ShopGrade().fillShopGradeCreationFields(null, Delivery.DELIVERY);
        grade.fillCommonCreationFields(authorId, ModState.UNMODERATED, shopId,
            "Отзыв должен уехать в диффе.", "Давай проверим многофакторную.", "Нет недостатков.",
            Anonymity.NONE, 213, 2);
        grade.setCreated(new Date(1525709984000L));
        grade.setGr0(0);
        grade.addAllGradeFactorValues(List.of(
            new GradeFactorValue(3, "Соответствие товара описанию", "", 4),
            new GradeFactorValue(0, "Скорость обработки заказа",  "", 2),
            new GradeFactorValue(2, "Общение",  "", 1),
            new GradeFactorValue(1, "Скорость и качество доставки",  "", 3)
        ));
        return grade;
    }

    public static ModelGrade constructModelGrade(long modelId, Long uid, Long vendorId) {
        ModelGrade result = new ModelGrade()
            .fillCommonModelGradeCreationFields(UsageTime.MORE_THAN_A_YEAR)
            .fillReportModel(GradeCreator.mockReportModel(modelId, "name",3L, "catName", vendorId));
        result.fillCommonCreationFields(uid, ModState.APPROVED, modelId, "text", "pro", "contra",
            Anonymity.NONE, 213, 213);
        result.setGradeFactorValues(Collections.singletonList(
            new GradeFactorValue(1L, "factor", "description", 1)
        ));
        result.setPhotos(constructPhotos());
        result.setAverageGrade(1);
        return result;
    }

    @NotNull
    private static List<Photo> constructPhotos() {
        return Collections.singletonList(
            Photo.buildForTest("groupId", "imageName" + UUID.randomUUID(), ModState.APPROVED)
        );
    }

    public long createApprovedGrade(AbstractGrade g) {
        GradeResponseDto createdGrade = multifactorGradeService.createGrade(g, createTestSecurityData());
        moderatorModificationProxy.moderateGradeReplies(Collections.singletonList(createdGrade.getId()),
                Collections.emptyList(), 1L, ModState.APPROVED);
        photoService.moderatePhotosByGradeIds(Collections.singletonList(createdGrade.getId()), 1L, ModState.APPROVED);
        return createdGrade.getId();
    }

    public void createGradeAndReject(ModelGrade g, Long reason) {
        GradeResponseDto createdGrade = multifactorGradeService.createGrade(g, createTestSecurityData());
        moderatorModificationProxy.moderateGradeReplies(Collections.singletonMap(createdGrade.getId(), reason), Collections.emptyMap(), 1L, ModState.REJECTED);
    }

    public void createGradeAndAutoReject(ModelGrade g) {
        GradeResponseDto createdGrade = multifactorGradeService.createGrade(g, createTestSecurityData());
        updateModState(createdGrade.getId(), ModState.AUTOMATICALLY_REJECTED);
    }

    public void updateModState(long gradeId, ModState modState) {
        pgJdbcTemplate.update("update grade set MOD_STATE = ? where id = ?", modState.value(), gradeId);
    }

    private static SecurityData createTestSecurityData() {
        return GradeCreator.defaultSecurityData();
    }

    public Long createSpamGrade(ModelGrade g) {
        GradeResponseDto createdGrade = multifactorGradeService.createGrade(g, createTestSecurityData());
        pgJdbcTemplate.update("update grade set grade_state = ? where id = ?", 0, createdGrade.getId());
        return createdGrade.getId();
    }

    public void createApprovedVerifiedGrade(ModelGrade g) {
        long id = createApprovedGrade(g);
        pgJdbcTemplate.update("update grade set verified = 1 where id = ?", id);
    }
}
