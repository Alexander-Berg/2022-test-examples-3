package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import steps.ordersteps.OrderSteps;

import ru.yandex.market.delivery.entities.common.Order;
import ru.yandex.market.delivery.entities.request.ds.DsCreateOrderRequest;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.sender.SenderBrandCreator;
import ru.yandex.market.delivery.rupostintegrationapp.service.exception.ServiceInternalException;
import ru.yandex.market.delivery.russianpostapiclient.bean.createorder.CreateOrderRequest;
import ru.yandex.market.delivery.russianpostapiclient.bean.declaredenum.AddressType;
import ru.yandex.market.delivery.russianpostapiclient.bean.declaredenum.MailCategory;
import ru.yandex.market.delivery.russianpostapiclient.bean.declaredenum.MailType;

class CreateOrderRequestConverterTest extends BaseTest {
    private static final int CORRECT_PICKUPPOINT_CODE = 123456;
    private static final long CORRECT_COST = 100L;

    private CreateOrderRequestConverter createOrderRequestConverter = new CreateOrderRequestConverter();
    private DsCreateOrderRequest dsCreateOrderRequest = new DsCreateOrderRequest();
    private DsCreateOrderRequest.RequestContent requestContent = dsCreateOrderRequest.new RequestContent();
    private Order order = OrderSteps.getOrder();

    @BeforeEach
    void before() {
        requestContent.setOrder(order);
        dsCreateOrderRequest.setRequestContent(requestContent);
    }

    @Test
    void parsePickupPointCodePositiveTest() {
        requestContent.getOrder().setPickupPointCode(Integer.toString(CORRECT_PICKUPPOINT_CODE));

        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getIndexTo())
            .as("Not correct result of pickuppoint code parse")
            .isEqualTo(CORRECT_PICKUPPOINT_CODE);
    }

    @Test
    void parsePickupPointCodeNegativeTest() {
        requestContent.getOrder().setPickupPointCode("adsfg");

        softly.assertThatThrownBy(() -> createOrderRequestConverter.convert(dsCreateOrderRequest))
            .isInstanceOf(ServiceInternalException.class);
    }

    @Test
    void setDimensionTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getDimension()).as("Not Empty order dimension").isNull();
    }

    @Test
    void setCostPositiveTest() {
        requestContent.getOrder().setAssessedCost(BigDecimal.valueOf(CORRECT_COST));
        long correctCostInKopecks = CORRECT_COST * 100L;

        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getInsuranceValue())
            .as("Cost in createOrderRequest not equals cost in order")
            .isEqualTo(correctCostInKopecks);
    }

    @Test
    void setMailCategoryWithCostTest() {
        requestContent.getOrder().setAssessedCost(BigDecimal.valueOf(CORRECT_COST));

        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getMailCategory())
            .as("Not set MailCategory.WITH_DECLARED_VALUE if set cost")
            .isEqualTo(MailCategory.WITH_DECLARED_VALUE);
    }

    @Test
    void setCostNegativeTest() {
        requestContent.getOrder().setAssessedCost(BigDecimal.valueOf(0, 5));

        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getInsuranceValue()).isEqualTo(0L);
    }

    @Test
    void setMailCategoryWithoutCostTest() {
        requestContent.getOrder().setAssessedCost(BigDecimal.valueOf(0, 5));

        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest.getMailCategory())
            .as("Not set MailCategory.ORDINARY if set cost")
            .isEqualTo(MailCategory.ORDINARY);
    }

    @Test
    void addressTypeTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getAddressToType())
            .as("Not correct AddressType in createOrderRequest")
            .isEqualTo(AddressType.DEMAND);
    }

    @Test
    void mailTypeTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getMailType())
            .as("Not correct MailType in createOrderRequest")
            .isEqualTo(MailType.EMS_OPTIMAL);
    }

    @Test
    void shipmentPointCodeTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getPostOfficeCode())
            .as("Not correct ShipmentPointCode in createOrderRequest")
            .isEqualTo(order.getShipmentPointCode());
    }

    @Test
    void placeToTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getPlaceTo())
            .as("placeTo in createOrderRequest not equal localityTo in order")
            .isEqualTo(order.getLocationTo().getLocality());
    }

    @Test
    void regionToTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getRegionTo())
            .as("regionTo in createOrderRequest not equal region in order")
            .isEqualTo(order.getLocationTo().getRegion());
    }

    @Test
    void mailDirectionCodeTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getMailDirectionCode())
            .as("Not correct mailDirectionCode. Warning! in class use 'magic' number for mailDirectionCode")
            .isEqualTo(643); //TODO: исправить после того как из конвертера выпилят магическую константу
    }

    @Test
    void wightTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(BigDecimal.valueOf(createOrderRequest.getWeight()))
            .as("Not correct weight in gramm")
            .isEqualTo(order.getWeight().multiply(BigDecimal.valueOf(1000)));
    }

    @Test
    void orderNumberTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getOrderNumber())
            .as("orderNumber in createOrderRequest not equal yandexId")
            .isEqualTo(order.getOrderId().getYandexId());
    }

    @Test
    void recipientNameTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getRecipientName())
            .as("Not correct recipientName in createOrderRequest")
            .isEqualTo(order.getRecipient().getFio().getName());
    }

    @Test
    void recipientSurnameTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getSurname())
            .as("Not correct recipientSurname in createOrderRequest")
            .isEqualTo(order.getRecipient().getFio().getSurname());
    }

    @Test
    void recipientMiddleNameTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getMiddleName())
            .as("Not correct recipientMiddleName in createOrderRequest")
            .isEqualTo(order.getRecipient().getFio().getPatronymic());
    }

    @Test
    void brandNameTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getBrandName())
            .as("brandName in createOrderRequest not equal sender in order")
            .isEqualTo(SenderBrandCreator.buildBrand(order.getSender()));
    }

    @Test
    void commentTest() {
        CreateOrderRequest createOrderRequest = createOrderRequestConverter.convert(dsCreateOrderRequest);

        softly.assertThat(createOrderRequest).as("createOrderRequest is empty").isNotNull();
        softly.assertThat(createOrderRequest.getComment())
            .as("Not correct comment in createOrderRequest")
            .isEqualTo(order.getComment());
    }
}
