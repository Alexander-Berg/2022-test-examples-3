package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import com.google.common.collect.ImmutableList;
import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.*;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

@Slf4j
public class IrisSteps {
    private static final WrapInfor wrapInfor = new WrapInfor();
    private static final ServiceBus serviceBus = new ServiceBus();
    private static final ApiClient apiClient = new ApiClient();
    private static final DatacreatorClient dataCreator = new DatacreatorClient();
    private static final RadiatorClient radiatorClient = new RadiatorClient();
    private static final IrisClient irisClient = new IrisClient();


    protected IrisSteps() {}

    @Step("Проверяем, что данные, отправленные пушем в Iris дошли и соответствуют отправленным")
    public void checkIrisPushVGH(String length,
                          String width,
                          String height,
                          String weight,
                          long partner_id,
                          String partner_sku) {
        ValidatableResponse response = irisClient.measurementAudit(partner_id,  partner_sku);

        int maxId = response.extract().jsonPath().getInt("id.max()");
        log.info( " Max Index "+ maxId+" ");

        double widthIris = response.extract().jsonPath().getDouble(
                "find{it.id == "+maxId+"}.payload.dimensions.width"
        );
        double heightIris = response.extract().jsonPath().getDouble(
                "find{it.id == "+maxId+"}.payload.dimensions.height"
        );
        double lengthIris = response.extract().jsonPath().getDouble(
                "find{it.id == "+maxId+"}.payload.dimensions.length"
        );
        double weightIris = response.extract().jsonPath().getDouble(
                "find{it.id == "+maxId+"}.payload.dimensions.weightGross"
        );

        Assertions.assertTrue( Double.parseDouble(length)*10 == lengthIris,"Длина не совпадает"); //см в мм
        Assertions.assertTrue( Double.parseDouble(height)*10 == heightIris,"Высота не совпадает");
        Assertions.assertTrue( Double.parseDouble(width)*10 == widthIris,"Ширина не совпадает");
        Assertions.assertTrue( Double.parseDouble(weight)*1000 == weightIris,"Вес не совпадает"); //кг в г
    }

    @Step("Ждем когда в IRIS появится Trustworthy info по айтему")
    public void waitTrustworthyInfoAppear(Long vendorId,
                                          String item,
                                          String length,
                                          String width,
                                          String height,
                                          String weight) {
        Retrier.retry(() -> {
                    ValidatableResponse response = apiClient.getTrustworthyInfo(vendorId,  item);

                    List<Double> widths = extractor("result.dimensions.width", response, Double.class);
                    Assertions.assertEquals(ImmutableList.of(Double.parseDouble(width) * 10), widths, "Ширина не совпадает");

                    List<Double> heights = extractor("result.dimensions.height", response, Double.class);
                    Assertions.assertEquals(ImmutableList.of(Double.parseDouble(height) * 10), heights, "Высота не совпадает");

                    List<Double> lengths = extractor("result.dimensions.length", response, Double.class);
                    Assertions.assertEquals(ImmutableList.of(Double.parseDouble(length) * 10), lengths, "Длина не совпадает");

                    List<Double> weights = extractor("result.weight_gross", response, Double.class);
                    Assertions.assertEquals(ImmutableList.of(Double.parseDouble(weight) * 1000), weights, "Вес не совпадает");

                }
                ,
                Retrier.RETRIES_MEDIUM,
                1,
                TimeUnit.MINUTES
        );

    }


    private <T> List<T> extractor(String path, ValidatableResponse response, Class clazz) {
        return response.extract().jsonPath().getList(path, clazz);
    }

}
