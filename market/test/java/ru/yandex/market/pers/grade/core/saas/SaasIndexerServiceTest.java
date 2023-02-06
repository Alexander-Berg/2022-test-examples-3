package ru.yandex.market.pers.grade.core.saas;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.pers.grade.client.model.DeliveredBy;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteRate;
import ru.yandex.market.pers.grade.core.saas.grade.AbstractSaasGrade;
import ru.yandex.market.pers.grade.core.saas.grade.SaasModelGrade;
import ru.yandex.market.pers.grade.core.saas.grade.SaasShopGrade;
import ru.yandex.market.pers.grade.core.saas.grade.preparer.ModelOpinionProvider;
import ru.yandex.market.pers.grade.core.service.GradeRankHelperService;
import ru.yandex.market.saas.indexer.SaasIndexerAction;
import ru.yandex.market.saas.indexer.SaasIndexerRequestBody;
import ru.yandex.market.saas.indexer.document.SaasDocument;

public class SaasIndexerServiceTest extends MockedTest {

    @Autowired
    ModelOpinionProvider modelOpinionProvider;
    @Autowired
    GradeRankHelperService gradeRankHelperService;

    private ModelGrade modelGrade;
    private ShopGrade shopGrade;

    private String importPartnerType;
    private String partnerName;

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void prepareGradeParam() {
        shopGrade = GradeCreator.constructShopGradeRnd();
        modelGrade = GradeCreator.constructModelGradeRnd();

        shopGrade.setId(10_000L);
        shopGrade.setResourceId(77410495456245L);
        shopGrade.setCreated(new Date(1509011291083L));
        shopGrade.setDelivery(Delivery.DELIVERY);
        shopGrade.setOrderId("123");
        shopGrade.setAverageGrade(1);
        GradeCreator.stabilizeGrade(shopGrade);

        modelGrade.setId(10_000L);
        modelGrade.setResourceId(10495456245L);  // too big model for int field
        modelGrade.setCreated(new Date(1509011291083L));
        modelGrade.setName("Name");
        GradeCreator.stabilizeGrade(modelGrade);
    }

    @Test
    public void testSerializeNullDocument() throws IOException, JSONException {
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(null, SaasIndexerAction.REOPEN);
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_request_body_without_documents.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeShopDocument() throws IOException, JSONException {
        SaasShopGrade gr = constructShopGrade();
        gr.setDeliveredBy(DeliveredBy.SHOP);
        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_shop_document.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    public void testSerializeShopDocumentOld() throws IOException, JSONException {
        shopGrade.setResourceId(1351L);
        SaasShopGrade gr = constructShopGrade();
        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_shop_document_old.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeShop() throws IOException, JSONException {
        SaasShopGrade gr = constructShopGrade();
        SaasDocument document = gr.convertToSaasDocument();
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_request_body_shop.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeShopWithNullValues() throws IOException, JSONException {
        SaasShopGrade gr = constructShopGrade();
        gr.setGroupId(null);
        SaasDocument document = gr.convertToSaasDocument();
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_request_shop_with_null_fields_body.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeModelDocument() throws IOException, JSONException {
        SaasModelGrade gr = constructModelGrade();
        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_model_document.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeModelDocumentOld() throws IOException, JSONException {
        modelGrade.setResourceId(123456L); // small model
        SaasModelGrade gr = constructModelGrade();
        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_model_document_old.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeModelDocumentWithInformativeText() throws IOException, JSONException {
        modelGrade.setPro("Вернись");
        modelGrade.setContra("в Коноху");
        modelGrade.setText("Саске");
        SaasModelGrade gr = constructModelGrade();

        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_model_document_informative.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeImportedModelDocument() throws IOException, JSONException {
        importPartnerType = "vendor";
        partnerName = "Philips";
        SaasModelGrade gr = constructModelGrade();

        SaasDocument document = gr.convertToSaasDocument();
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_model_document_imported.json"),
            mapper.writeValueAsString(document),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeModel() throws IOException, JSONException {
        SaasModelGrade gr = constructModelGrade();
        SaasDocument document = gr.convertToSaasDocument();
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_request_body_model.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testSerializeModelWithNullValues() throws IOException, JSONException {
        SaasModelGrade gr = constructModelGrade();
        gr.setName(null);
        SaasDocument document = gr.convertToSaasDocument();
        SaasIndexerRequestBody body = new SaasIndexerRequestBody(document, SaasIndexerAction.MODIFY);
        JSONAssert.assertEquals(
            getResourceAsString("data/saas_request_model_with_null_fields_body.json"),
            mapper.writeValueAsString(body),
            JSONCompareMode.NON_EXTENSIBLE);
    }

    private String getResourceAsString(String filename) throws IOException {
        return IOUtils.readInputStream(getClass().getClassLoader().getResourceAsStream(filename));
    }

    private SaasShopGrade constructShopGrade() {
        SaasShopGrade grade = new SaasShopGrade(shopGrade.getId());
        fillGrade(grade, shopGrade);
        grade.setResourceId(shopGrade.getResourceId());
        grade.setResolved(shopGrade.getResolved());
        grade.setOrderId(shopGrade.getOrderId());
        grade.setDelivery(shopGrade.getDelivery());
        Long groupId = 41234L;
        grade.setGroupId(groupId);
        grade.setCloneIds(Set.of(shopGrade.getResourceId()));
        return grade;
    }

    private SaasModelGrade constructModelGrade() {
        SaasModelGrade grade = new SaasModelGrade(modelGrade.getId());
        fillGrade(grade, modelGrade);
        grade.setPhotoRated(0);
        grade.setResourceId(modelGrade.getResourceId());
        grade.setCloneIds(Set.of(modelGrade.getResourceId()));
        grade.setName(modelGrade.getName());
        grade.setUsageTime(modelGrade.getUsageTime().value());
        grade.setImportPartnerType(importPartnerType);
        grade.setPartnerName(partnerName);
        return grade;
    }

    private void fillGrade(AbstractSaasGrade grade, AbstractGrade param) {
        grade.setFixId(param.getId());
        grade.setAuthorUid(param.getAuthorUid());
        grade.setCreated(param.getCreated());
        grade.setCpa(false);
        grade.setRecommend(false);
        grade.setGradeValue(param.getGradeValue());
        grade.setText(param.getText());
        grade.setPro(param.getPro());
        grade.setContra(param.getContra());
        grade.setAnonymity(param.getAnonymous());
        grade.setRegionId(param.getRegionId());
        grade.setRank(0.5F);
        grade.setKarmaRank(0.5F);
        grade.setKarmaRankRated(0.5F);
        grade.setFactors(Set.of("0:1:Фактор раз", "1:3:Фактор два"));
    }

    @Test
    public void calculateRankWithGrade() {
        Map<Integer, GradeVoteRate> gradeVoteRates = new HashMap<>();
        gradeVoteRates.put(5, new GradeVoteRate(5, 2.5, 2.5));
        gradeVoteRates.put(4, new GradeVoteRate(4, 2, 2));
        gradeVoteRates.put(3, new GradeVoteRate(3, 1.5, 1.5));
        gradeVoteRates.put(2, new GradeVoteRate(2, 1.25, 1.25));
        gradeVoteRates.put(1, new GradeVoteRate(1, 1, 1));
        gradeRankHelperService.saveVoteRate(new ArrayList<>(gradeVoteRates.values()));

        modelOpinionProvider.loadTemporaryData();
        int agree = 20;
        int reject = 5;

        double prevRank = modelOpinionProvider.calculateRankWithGrade(agree, reject, 1);
        for (int i = 2; i <= 5; i++) {
            double rank = modelOpinionProvider.calculateRankWithGrade(agree, reject, 2);
            Assert.assertTrue(rank > prevRank);
        }
    }

    @Test
    public void calculatePhotoRated() {
        SaasModelGrade gr = constructModelGrade();
        gr.setPhotos(Set.of("group-id:imagename", "group-id:imagename2"));
        modelOpinionProvider.loadTemporaryData();
        modelOpinionProvider.postProcessDocument(gr);
        Assert.assertEquals(0.6, gr.getPhotoRated(), 0.0); // 0.5 + 2/10/2
    }

    @Test
    public void calculateFreshness() {
        SaasModelGrade gr = constructModelGrade();
        gr.setCreated(new Date(Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli()));
        modelOpinionProvider.loadTemporaryData();
        modelOpinionProvider.postProcessDocument(gr);
        Assert.assertEquals(0.6666, gr.getFreshness(), 0.001);

        gr = constructModelGrade();
        gr.setCreated(new Date(Instant.now().minus(20, ChronoUnit.DAYS).toEpochMilli()));
        modelOpinionProvider.loadTemporaryData();
        modelOpinionProvider.postProcessDocument(gr);
        Assert.assertEquals(0.3333, gr.getFreshness(), 0.001);

        gr = constructModelGrade();
        gr.setCreated(new Date(Instant.now().minus(31, ChronoUnit.DAYS).toEpochMilli()));
        modelOpinionProvider.loadTemporaryData();
        modelOpinionProvider.postProcessDocument(gr);
        Assert.assertEquals(0, gr.getFreshness(), 0.001);
    }

}
