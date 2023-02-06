package ru.yandex.market.pers.grade.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ClusterGrade;
import ru.yandex.market.pers.grade.core.model.core.GradeSource;
import ru.yandex.market.pers.grade.core.model.core.GradeValue;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.ugc.model.GradeFactorValue;
import ru.yandex.market.pers.grade.core.ugc.model.Photo;
import ru.yandex.market.pers.service.common.util.AbstractSecurityData;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Vendor;

/**
 * @author varvara
 * 02.09.2019
 */
public class GradeCreator {
    // генераторы для данных к тестам
    public static AtomicLong LONG_GEN = new AtomicLong(100100000);
    public static AtomicLong UID_GEN = new AtomicLong(600600000);
    public static AtomicLong MODEL_ID_GEN = new AtomicLong(700700000);
    public static AtomicLong SHOP_ID_GEN = new AtomicLong(800800000);

    public static final String DEFAULT_YANDEX_UID = "5217148121158766543";

    @Autowired
    private DbGradeService gradeService;

    private static final long ORDER_ID = 1234L;

    public static long rndLong() {
        return LONG_GEN.getAndIncrement();
    }

    public static long rndUid() {
        return UID_GEN.getAndIncrement();
    }

    public static long rndModel() {
        return MODEL_ID_GEN.getAndIncrement();
    }

    public static long rndShop() {
        return SHOP_ID_GEN.getAndIncrement();
    }


    public static SecurityData defaultSecurityData() {
        long uid = 123L; // any uid - just to inform that this is not unlogin
        return SecurityData.build(null, Map.of(
            AbstractSecurityData.HEADER_X_REAL_IP, "231.231.231.231",
            "other", "TEST_HTTPHEADERS"
        ), DEFAULT_YANDEX_UID, uid, "4ed86");
    }

    public static SecurityData unloginSecurityData(String yandexUid) {
        return SecurityData.build(null, Map.of(
            AbstractSecurityData.HEADER_X_REAL_IP, "231.231.231.231",
            "other", "TEST_HTTPHEADERS"
        ), yandexUid, null, "4ed86");
    }

    public static ShopGrade constructShopGradeRnd() {
        return constructShopGrade(rndShop(), rndUid());
    }

    public static ShopGrade constructShopGrade(long shopId, Long userId) {
        ShopGrade shopGrade = new ShopGrade().fillShopGradeCreationFields(String.valueOf(ORDER_ID), Delivery.PICKUP);
        shopGrade.fillCommonCreationFields(userId, ModState.APPROVED, shopId,
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            Anonymity.NONE, null, null
        );
        shopGrade.addGradeFactorValues(new GradeFactorValue(3, "Соответствие товара описанию", "", 4));
        shopGrade.addGradeFactorValues(new GradeFactorValue(0, "Скорость обработки заказа", "", 2));
        shopGrade.addGradeFactorValues(new GradeFactorValue(2, "Общение", "", 1));
        shopGrade.addGradeFactorValues(new GradeFactorValue(1, "Скорость и качество доставки", "", 3));
        shopGrade.setAverageGrade(2);
        return shopGrade;
    }

    public static ModelGrade constructModelGradeRnd() {
        return constructModelGrade(rndModel(), rndUid());
    }

    public static ClusterGrade constructClusterGrade(Long clusterId, Long authorId) {
        ClusterGrade clusterGrade = (ClusterGrade) new ClusterGrade()
                .fillCommonModelGradeCreationFields(UsageTime.SEVERAL_DAYS)
                .fillReportModel(GradeCreator.mockReportModel(clusterId, "моделька", 1L, "категория", 1L));

        clusterGrade.fillCommonCreationFields(authorId, ModState.APPROVED, clusterId,
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                Anonymity.NONE, 123, 123
        );
        clusterGrade.setGradeFactorValues(Collections.singletonList(
                new GradeFactorValue(1L, "factor", "description", 1)
        ));
        clusterGrade.setPhotos(constructPhotos());
        clusterGrade.setAverageGrade(1);
        return clusterGrade;
    }

    public static ModelGrade constructModelGrade(Long modelId, Long authorId) {
        ModelGrade modelGrade = new ModelGrade()
            .fillCommonModelGradeCreationFields(UsageTime.SEVERAL_DAYS)
            .fillReportModel(GradeCreator.mockReportModel(modelId, "моделька", 1L, "категория", 1L));

        modelGrade.fillCommonCreationFields(authorId, ModState.APPROVED, modelId,
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            Anonymity.NONE, 123, 123
        );
        modelGrade.setGradeFactorValues(Collections.singletonList(
            new GradeFactorValue(1L, "factor", "description", 1)
        ));
        modelGrade.setPhotos(constructPhotos());
        modelGrade.setAverageGrade(1);
        return modelGrade;
    }

    public static ModelGrade constructModelGradeNoText(Long modelId, Long authorId, ModState modState) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, authorId);
        grade.setModState(modState);
        grade.setText(null);
        grade.setPro(null);
        grade.setContra(null);
        grade.setPhotos(List.of());
        return grade;
    }

    public static ShopGrade constructShopGradeNoText(Long shopId, Long authorId, ModState modState) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, authorId);
        grade.setModState(modState);
        grade.setText(null);
        grade.setPro(null);
        grade.setContra(null);
        grade.setPhotos(List.of());
        return grade;
    }

    @NotNull
    public static List<Photo> constructPhotos() {
        return Collections.singletonList(
            Photo.buildForTest("groupId", "imageName" + UUID.randomUUID().toString(), ModState.APPROVED)
        );
    }

    public long createShopGrade(long userId, long shopId) {
        return createShopGrade(
            userId,
            shopId,
            GradeValue.GOOD.toAvgGrade()
        );
    }

    /**
     * @param userId
     * @param shopId
     * @param gradeValue : 1..5
     * @return
     */
    public long createShopGrade(long userId, long shopId, int gradeValue) {
        ShopGrade shopGrade = constructShopGrade(shopId, userId);
        shopGrade.setAverageGrade(gradeValue);
        return createGrade(shopGrade);
    }

    public long createShopGrade(long userId, long shopId, int gradeValue, ModState modState) {
        ShopGrade shopGrade = constructShopGrade(shopId, userId);
        shopGrade.setAverageGrade(gradeValue);
        shopGrade.setModState(modState);
        return createGrade(shopGrade);
    }

    public AbstractGrade createAndReturnGrade(AbstractGrade grade) {
        return gradeService.getGrade(createGrade(grade));
    }

    public long createClusterGrade(Long clusterId, Long authorId) {
        ClusterGrade clusterGrade = constructClusterGrade(clusterId, authorId);
        return createGrade(clusterGrade);
    }

    public long createModelGrade(Long modelId, Long authorId) {
        ModelGrade modelGrade = constructModelGrade(modelId, authorId);
        return createGrade(modelGrade);
    }

    public long createModelGradeUnmoderated(Long modelId, Long authorId) {
        return createModelGrade(modelId, authorId, ModState.UNMODERATED);
    }

    public long createModelGrade(Long modelId, Long authorId, ModState modState) {
        ModelGrade modelGrade = constructModelGrade(modelId, authorId);
        modelGrade.setModState(modState);
        return createGrade(modelGrade);
    }

    public long createModelGrade(Long modelId, Long authorId, ModState modState, String text) {
        ModelGrade modelGrade = constructModelGrade(modelId, authorId);
        modelGrade.setModState(modState);
        modelGrade.setText(text);
        modelGrade.setPro("");
        modelGrade.setContra("");
        return createGrade(modelGrade);
    }

    public long createFeedbackGrade(long shopId, long uid, String orderId) {
        ShopGrade grade = GradeCreator.constructShopGrade(shopId, uid);
        grade.setSource(GradeSource.FEEDBACK.value());
        grade.setOrderId(orderId);
        return gradeService.createGrade(grade, defaultSecurityData());
    }

    public long createFeedbackGradeRnd() {
        ShopGrade grade = GradeCreator.constructShopGradeRnd();
        grade.setOrderId(String.valueOf(ORDER_ID));
        grade.setSource(GradeSource.FEEDBACK.value());
        return gradeService.createGrade(grade, defaultSecurityData());
    }

    public long createGrade(AbstractGrade grade) {
        return gradeService.createGrade(grade, defaultSecurityData());
    }

    public long createGradeUnlogin(AbstractGrade grade, String yandexUid) {
        return gradeService.createGrade(grade, unloginSecurityData(yandexUid));
    }

    public static Model mockReportModel(long modelId, Long catId, String catName, Long vendorId) {
        return mockReportModel(modelId, null, catId, catName, vendorId);
    }

    public static Model mockReportModel(long modelId, String modelName, Long catId, String catName, Long vendorId) {
        Model model = new Model();
        model.setId(modelId);
        model.setName(modelName);
        if (catId != null) {
            model.setCategory(new Category(catId, catName));
        }
        if (vendorId != null) {
            model.setVendor(new Vendor(vendorId, "some vendor"));
        }
        return model;
    }

    public static Model mockReportModelCat(long modelId, Long catId, String catName) {
        Model model = new Model();
        model.setId(modelId);
        if (catId != null) {
            model.setCategory(new Category(catId, catName));
        }
        return model;
    }

    public static void stabilizeGrade(AbstractGrade grade) {
        grade.setText("shortText");
        grade.setContra("contra");
        grade.setPro("pro");
        grade.setAuthorUid(161658075L);
        grade.setRegionId(213);
    }
}
