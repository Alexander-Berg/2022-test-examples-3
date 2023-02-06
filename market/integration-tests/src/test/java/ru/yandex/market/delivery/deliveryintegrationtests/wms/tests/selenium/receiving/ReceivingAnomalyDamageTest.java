package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.List;
import java.util.Optional;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.RegistryItemWrapper;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.common.RegistryBox;
import ru.yandex.market.logistic.api.model.common.RegistryType;
import ru.yandex.market.logistic.api.model.common.UnitCount;
import ru.yandex.market.logistic.api.model.common.UnitCountType;
import ru.yandex.market.logistic.api.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.api.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundRegistryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.PutInboundResponse;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.DEFECT;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.MISMATCHING_DESCRIPTION;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.NO_LIFE_TIME;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.NO_RUSSIAN_INFO;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.NO_TEMPERATURE_REGIME;
import static ru.yandex.market.logistic.api.model.common.NonconformityType.PART_MISSING;

@DisplayName("Selenium: Приёмка аномалий")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
@Slf4j
public class ReceivingAnomalyDamageTest extends AnomalySeleniumTest {

    private final Long VENDOR_ID = 1559L;
    private PutInboundResponse inbound;

    @Step("Создаем поставку товаров и принимаем её на паллету")
    private String createInboundAndPlaceToPallet(List<RegistryItem> items) {
        inbound = ApiSteps.Inbound().putInbound();
        log.info("Created inbound: {}", inbound.getInboundId());

        PutInboundRegistryResponse response =
                ApiSteps.Inbound().putInboundRegistry(inbound, items, RegistryType.PLANNED);
        log.info("registryId: {}", response.getRegistryId());

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inbound.getInboundId().getPartnerId(), 1);
        uiSteps.Login().PerformLogin();
        return uiSteps.Receiving().findPalletOfInbound(inbound.getInboundId().getPartnerId());
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии бракованный товар")
    @ResourceLock("Принимаем в аномалии бракованный товар")
    public void receiveAnomalyDamaged() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.DEFECT);
        registryItem1.getNonconformityTypes().add(DEFECT);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, DEFECT);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с не соответствующим описанием")
    @ResourceLock("Принимаем в аномалии товар с не соответствующим описанием")
    public void receiveAnomalyMismatchingDescription() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.MISGRADING);
        registryItem1.getNonconformityTypes().add(MISMATCHING_DESCRIPTION);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, MISMATCHING_DESCRIPTION);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар без российской этикетки")
    @ResourceLock("Принимаем в аномалии товар без российской этикетки")
    public void receiveAnomalyNoRussianInfo() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.MISGRADING);
        registryItem1.getNonconformityTypes().add(NO_RUSSIAN_INFO);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, NO_RUSSIAN_INFO);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар, у которого отсутствует часть комплекта")
    @ResourceLock("Принимаем в аномалии товар, у которого отсутствует часть комплекта")
    public void receiveAnomalyPartMissing() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.PART_MISSING);
        registryItem1.getNonconformityTypes().add(PART_MISSING);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, PART_MISSING);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар, для которого нет условий хранения")
    @ResourceLock("Принимаем в аномалии товар, для которого нет условий хранения")
    public void receiveAnomalyNoTemperatureConditions() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.MISGRADING);
        registryItem1.getNonconformityTypes().add(NO_TEMPERATURE_REGIME);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, NO_TEMPERATURE_REGIME);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии без срока годности")
    @ResourceLock("Принимаем в аномалии без срока годности")
    public void receiveAnomalyNoLifeTime() {
        RegistryItemWrapper registryItem1 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 1Name",
                "lot1", "1230918231", false);
        registryItem1.getAnomalyTypes().add(UnitCountType.MISGRADING);
        registryItem1.getNonconformityTypes().add(NO_LIFE_TIME);
        RegistryItemWrapper registryItem2 = new RegistryItemWrapper(VENDOR_ID, UniqueId.getStringUUID(), "Its 2Name",
                "lot1", "1230918232", false);

        List<RegistryItem> itemList = List.of(registryItem1.getRegistryItem(), registryItem2.getRegistryItem());
        String pallet = createInboundAndPlaceToPallet(itemList);
        log.info("Pallet: {}", pallet);

        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        log.info("good cart: {}", goodCartId);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly cart: {}", anomalyCartId);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(registryItem2, pallet, goodCartId);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveAnomaly(registryItem1, pallet, anomalyCartId, NO_LIFE_TIME);

        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        log.info("anomaly container: {}", containerId);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getInboundId().getPartnerId());
        GetInboundResponse inboundResponse = ApiSteps.Inbound().getInbound(inbound.getInboundId());
        log.info("Get inbound response: {}", inboundResponse);

        //assertions
        InboundRegistry factualRegistry = getRegistry(inboundResponse, RegistryType.FACTUAL);
        assertThat("Реестр содержит элементов", factualRegistry.getItems().size(), equalTo(itemList.size()));
        checkItem(factualRegistry, registryItem1);
        checkItem(factualRegistry, registryItem2);
        checkBoxes(factualRegistry.getBoxes(), containerId);
        InboundRegistry factualInitiallyRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_INITIAL);
        assertThat("Реестр содержит элементов", factualInitiallyRegistry.getItems(), nullValue());
        InboundRegistry factualSecondaryRegistry =
                getRegistry(inboundResponse, RegistryType.FACTUAL_ACCEPTANCE_SECONDARY);
        assertThat("Реестр содержит элементов",
                factualSecondaryRegistry.getItems().size(), equalTo(itemList.size()));
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem1);
        checkItemFactualAcceptanceSecondary(factualSecondaryRegistry, registryItem2);
        checkBoxes(factualSecondaryRegistry.getBoxes(), containerId);
    }

    private InboundRegistry getRegistry(GetInboundResponse getInboundResponse, RegistryType registryType) {
        return getInboundResponse.getRegistries().stream()
                .filter(registry ->
                        registry.getRegistryType().equals(registryType))
                .findFirst()
                .orElse(null);
    }

    private void checkItem(InboundRegistry registry, RegistryItemWrapper originalItem) {
        Optional<RegistryItemWrapper> registryItemOpt = registry.getItems().stream()
                .map(RegistryItemWrapper::new)
                .filter(item ->
                        item.getArticle().equals(originalItem.getArticle())
                ).findFirst();
        assertThat("Item is found", registryItemOpt.isPresent());
        RegistryItemWrapper registryItem = registryItemOpt.get();
        assertThat("count", registryItem.getQuantity(), equalTo(1));
        assertThat("Article", registryItem.getArticle(), equalTo(originalItem.getArticle()));
        assertThat("vendorId", registryItem.getVendorId(), equalTo(originalItem.getVendorId()));
        if (originalItem.getAnomalyTypes().isEmpty()) {
            Integer quantity = registryItem.getQuantity();
            assertThat("Количество:", quantity, equalTo(originalItem.getQuantity()));
            assertThat("item name", registryItem.getName(), equalTo(originalItem.getArticle()));
        } else {
            UnitCount unitCount = registryItem.getCount(originalItem.getAnomalyTypes().get(0));
            assertThat("Количество:", unitCount.getQuantity(), equalTo(originalItem.getQuantity()));
            assertThat("item name", registryItem.getName(), equalTo(originalItem.getArticle()));
            assertThat("count type",
                    registryItem.getRegistryItem().getUnitInfo().getCounts().get(0).getCountType(),
                    equalTo(originalItem.getAnomalyTypes().get(0)));
        }
    }

    private void checkItemFactualAcceptanceSecondary(InboundRegistry registry, RegistryItemWrapper originalItem) {
        Optional<RegistryItemWrapper> registryItemOpt = registry.getItems().stream()
                .map(RegistryItemWrapper::new)
                .filter(item ->
                        item.getArticle().equals(originalItem.getArticle())
                ).findFirst();
        assertThat("Item is found", registryItemOpt.isPresent());
        RegistryItemWrapper registryItem = registryItemOpt.get();
        assertThat("count", registryItem.getQuantity(), equalTo(1));
        assertThat("Article", registryItem.getArticle(), equalTo(originalItem.getArticle()));
        assertThat("vendorId", registryItem.getVendorId(), equalTo(originalItem.getVendorId()));
        if (originalItem.getAnomalyTypes().isEmpty()) {
            Integer quantity = registryItem.getQuantity();
            assertThat("Количество:", quantity, equalTo(originalItem.getQuantity()));
            assertThat("item name", registryItem.getName(), equalTo(originalItem.getArticle()));
        } else {
            UnitCount unitCount = registryItem.getCount(UnitCountType.NON_COMPLIENT);
            assertThat("Количество:", unitCount.getQuantity(), equalTo(originalItem.getQuantity()));
            assertThat("barcode", registryItem.getBarcode(), equalTo(originalItem.getBarcode()));
            assertThat("item name", registryItem.getName(), equalTo(originalItem.getName()));
            assertThat("count type",
                    registryItem.getRegistryItem().getUnitInfo().getCounts().get(0).getCountType(),
                    equalTo(UnitCountType.NON_COMPLIENT));
            assertThat("Nonconformity Attributes",
                    registryItem.getRegistryItem().getUnitInfo().getCounts().get(0).getNonconformityAttributes().size(),
                    equalTo(1));
            assertThat("Nonconformity Attributes",
                    registryItem.getRegistryItem().getUnitInfo().getCounts().get(0).getNonconformityAttributes().get(0),
                    equalTo(originalItem.getNonconformityTypes().get(0)));
        }
    }

    private void checkBoxes(List<RegistryBox> boxes, String anomalyCartId) {
        assertThat("Аномальная тара", boxes.size(), equalTo(1));
        RegistryBox registryBox = boxes.get(0);
        assertThat(registryBox.getUnitInfo().getCounts().get(0).getQuantity(), equalTo(1));
        assertThat(registryBox.getUnitInfo().getCounts().get(0).getCountType(), equalTo(UnitCountType.ANOMALY));
        assertThat("Box type", registryBox.getUnitInfo().getCompositeId().getPartialIds().get(0).getIdType(),
                equalTo(PartialIdType.BOX_ID));
        assertThat(registryBox.getUnitInfo().getCompositeId().getPartialIds().get(0).getValue(),
                equalTo(anomalyCartId));
    }

}
