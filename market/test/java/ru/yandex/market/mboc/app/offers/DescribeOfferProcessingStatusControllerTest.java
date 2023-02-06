package ru.yandex.market.mboc.app.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.expression.DescribeExprHandler;
import ru.yandex.market.mboc.common.utils.expression.EvaluateAndDescribeExprHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.app.offers.DescribeOfferProcessingStatusController.Format.JSON;

public class DescribeOfferProcessingStatusControllerTest extends BaseMbocAppTest {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private SupplierRepository supplierRepository;

    private DescribeOfferProcessingStatusController controller;

    @Before
    public void setup() {
        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier()
        );

        categoryInfoRepository = new CategoryInfoRepositoryMock(Mockito.mock(MboUsersRepository.class));
        categoryInfoRepository.insertBatch(Arrays.asList(
            new CategoryInfo(1).setModerationInYang(true),
            new CategoryInfo(2),
            new CategoryInfo(3),
            new CategoryInfo(4)));
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService
            .addCategory(1)
            .addCategory(2)
            .addCategory(4);
        var categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategories(
            OfferTestUtils.defaultCategory()
        );

        var supplierService = new SupplierService(supplierRepository);
        NeedContentStatusService needContentStatusService = new NeedContentStatusService(
            categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet())
        );

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);


        OffersProcessingStatusService offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false, false, 3, categoryInfoCache);

        controller = new DescribeOfferProcessingStatusController(offerRepository, offersProcessingStatusService);
    }

    @Test
    public void describeStatusCalculation() {
        var response = controller.describeStatusCalculation(JSON.name());
        var expTree = (DescribeExprHandler.Node) response.getBody();

        assertThat(expTree).isNotNull();
        assertThat(expTree.getChildren()).isNotEmpty();

        walkTree(expTree, node ->
            assertThat(node)
                .as("node %s should have no marks", node.getDescription())
                .matches(n -> n.getMarks().isEmpty())
        );
    }

    @Test
    public void describeOfferStatusCalculation() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);
        var response = controller.describeOfferStatusCalculation(offer.getId(), false, JSON.name());
        var expTree = (DescribeExprHandler.Node) response.getBody();

        assertThat(expTree).isNotNull();

        walkTree(expTree, node -> {
            if (node.getChildren().isEmpty()) {
                assertThat(node.getMarks())
                    .as("leaf node %s should have EvaluatedMark", node.getDescription())
                    .extracting(o -> o.getClass().getName())
                    .containsExactly(EvaluateAndDescribeExprHandler.EvaluatedMark.class.getName());
            }
        });
    }

    @Test
    public void describeOfferStatusCalculationFull() {
        Offer offer = OfferTestUtils.simpleOffer();
        offerRepository.insertOffer(offer);
        var response = controller.describeOfferStatusCalculation(offer.getId(), true, JSON.name());
        var expTree = (DescribeExprHandler.Node) response.getBody();

        assertThat(expTree).isNotNull();

        Map<String, AtomicInteger> foundMarks = new HashMap<>();
        AtomicInteger nodesCount = new AtomicInteger();
        walkTree(expTree, node -> {
            nodesCount.incrementAndGet();
            node.getMarks()
                .forEach(mark -> foundMarks.computeIfAbsent(mark.getClass().getName(), m -> new AtomicInteger())
                    .incrementAndGet());
        });

        assertThat(foundMarks.keySet())
            .containsExactly(EvaluateAndDescribeExprHandler.EvaluatedMark.class.getName());

        int markedNodesCount = foundMarks.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        assertThat(markedNodesCount)
            .as("not all nodes marked as EVALUATED")
            .isLessThan(nodesCount.get());
    }

    private void walkTree(DescribeExprHandler.Node descriptionTree,
                          Consumer<DescribeExprHandler.Node> nodeConsumer) {
        nodeConsumer.accept(descriptionTree);
        descriptionTree.getChildren().forEach(node -> walkTree(node, nodeConsumer));
    }
}
