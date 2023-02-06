package ru.yandex.market.pers.tms.logbroker.executor.antifraud;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.logbroker.producer.GradeForAntifraudProducer;
import ru.yandex.market.pers.tms.yt.model.GradeForAntifraudYt;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GradeForAntifraudProducerExecutorTest extends MockedPersTmsTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final long SHOP_ID = 304L;
    @Autowired
    GradeForAntifraudProducerExecutor gradeForAntifraudProducerExecutor;
    @Autowired
    GradeForAntifraudProducer gradeForAntifraudProducer;
    @Autowired
    GradeCreator gradeCreator;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    @Qualifier("logbrokerClientFactory")
    LogbrokerClientFactory logbrokerClientFactory;
    @Captor
    ArgumentCaptor<byte[]> messageCaptor;

    private AsyncProducer mockLogbrokerClientFactory() throws InterruptedException {
        AsyncProducer asyncProducer = mock(AsyncProducer.class);
        when(logbrokerClientFactory.asyncProducer(any())).thenReturn(asyncProducer);
        when(asyncProducer.init()).thenReturn(CompletableFuture.completedFuture(new ProducerInitResponse(1, "", 1, "")));
        return asyncProducer;
    }

    @Test
    public void testGradeProducingWithError() throws Exception {
        long modelGrade = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        long shopGradeBad = gradeCreator.createGrade(GradeCreator.constructShopGradeRnd());
        gradeForAntifraudProducer.setLastProcessedId(modelGrade - 1);
        AsyncProducer asyncProducer = mockLogbrokerClientFactory();

        when(asyncProducer.write(messageCaptor.capture())).thenAnswer(invocation -> {
            if (invocation == null) {
                return null;
            }
            byte[] argument = invocation.getArgument(0);
            if (mapper.readValue(argument, GradeForAntifraudYt.class).getId() == modelGrade) {
                return CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true));
            }
            CompletableFuture completableFuture = new CompletableFuture();
            completableFuture.completeExceptionally(new RuntimeException("bad"));
            return completableFuture;
        });

        gradeForAntifraudProducerExecutor.runTmsJob();
        Assert.assertEquals(modelGrade, gradeForAntifraudProducer.getLastProcessedGradeId());
    }

    @Test
    public void testGradeProducing() throws Exception {
        long modelGrade = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        long shopGrade = gradeCreator.createGrade(GradeCreator.constructShopGradeRnd());
        gradeForAntifraudProducer.setLastProcessedId(modelGrade - 1);
        AsyncProducer asyncProducer = mockLogbrokerClientFactory();

        when(asyncProducer.write(messageCaptor.capture())).thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true)));

        gradeForAntifraudProducerExecutor.runTmsJob();
        List<String> grades = messageCaptor.getAllValues().stream().map(String::new).collect(Collectors.toList());

        Assert.assertEquals(shopGrade, gradeForAntifraudProducer.getLastProcessedGradeId());
    }

    @Test
    public void testEmptyGradeProducing() throws Exception {
        long modelGrade = gradeCreator.createGrade(GradeCreator.constructModelGradeRnd());
        long shopGrade = gradeCreator.createGrade(GradeCreator.constructShopGradeRnd());
        gradeForAntifraudProducer.setLastProcessedId(shopGrade); // already processed
        AsyncProducer asyncProducer = mockLogbrokerClientFactory();
        verifyZeroInteractions(asyncProducer);
        gradeForAntifraudProducerExecutor.runTmsJob();
        Assert.assertEquals(shopGrade, gradeForAntifraudProducer.getLastProcessedGradeId());
    }

    @Test
    public void testModelGradeSerialization() throws Exception {
        ModelGrade gradeToCreate = GradeCreator.constructModelGradeRnd();
        gradeToCreate.setResourceId(10495456L);
        GradeCreator.stabilizeGrade(gradeToCreate);
        long modelGrade = gradeCreator.createGrade(gradeToCreate);
        gradeForAntifraudProducer.setLastProcessedId(modelGrade - 1);
        AsyncProducer asyncProducer = mockLogbrokerClientFactory();

        when(asyncProducer.write(messageCaptor.capture())).thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true)));

        gradeForAntifraudProducerExecutor.runTmsJob();
        List<String> grades = messageCaptor.getAllValues().stream().map(String::new).collect(Collectors.toList());
        Assert.assertEquals(1, grades.size());

        String json = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/logbroker/logbroker_model_grade.json"),
            StandardCharsets.UTF_8
        );
        JSONAssert.assertEquals(json, grades.get(0),
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("http_headers", (o1, o2) -> true),
                new Customization("cr_time", (o1, o2) -> true),
                new Customization("id", (o1, o2) -> true)));
    }

    @Test
    public void testShopGradeSerialization() throws Exception {
        ShopGrade shopGradeToCreate = GradeCreator.constructShopGradeRnd();
        GradeCreator.stabilizeGrade(shopGradeToCreate);
        shopGradeToCreate.setResourceId(SHOP_ID);
        long shopGrade = gradeCreator.createGrade(shopGradeToCreate);
        gradeForAntifraudProducer.setLastProcessedId(shopGrade - 1);
        AsyncProducer asyncProducer = mockLogbrokerClientFactory();

        when(asyncProducer.write(messageCaptor.capture())).thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, true)));

        gradeForAntifraudProducerExecutor.runTmsJob();
        List<String> grades = messageCaptor.getAllValues().stream().map(String::new).collect(Collectors.toList());
        Assert.assertEquals(1, grades.size());

        String json = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/logbroker/logbroker_shop_grade.json"),
            StandardCharsets.UTF_8
        );
        JSONAssert.assertEquals(json, grades.get(0),
            new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
                new Customization("http_headers", (o1, o2) -> true),
                new Customization("cr_time", (o1, o2) -> true),
                new Customization("id", (o1, o2) -> true)));
    }


}
