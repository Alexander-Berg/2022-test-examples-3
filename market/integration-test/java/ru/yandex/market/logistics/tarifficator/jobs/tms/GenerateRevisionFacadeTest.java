package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.exception.RevisionGenerationException;
import ru.yandex.market.logistics.tarifficator.jobs.model.GenerateRevisionPayload;
import ru.yandex.market.logistics.tarifficator.jobs.processor.GenerateRevisionFacade;
import ru.yandex.market.logistics.tarifficator.service.export.DeliveryCalculatorDatasetGenerator;
import ru.yandex.market.logistics.tarifficator.service.revision.RevisionItemService;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.tarifficator.mds.MdsFactory.buildDatasetFilename;
import static ru.yandex.market.logistics.tarifficator.mds.MdsFactory.buildDatasetLocation;
import static ru.yandex.market.logistics.tarifficator.mds.MdsFactory.buildDatasetUrl;

@DisplayName("Интеграционный тест GenerateRevisionService")
@DatabaseSetup("/tags/tags.xml")
class GenerateRevisionFacadeTest extends AbstractContextualTest {

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private RevisionItemService revisionItemService;
    @Autowired
    private GenerateRevisionFacade generateRevisionFacade;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private DeliveryCalculatorDatasetGenerator deliveryCalculatorDatasetGenerator;
    @Autowired
    private LMSClient lmsClient;

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Генерация пустого поколения")
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-empty-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateEmptySuccess() {
        generateRevisionFacade.processPayload(createPayload(Set.of(1L)));
    }

    @Test
    @DisplayName("Не генерировать выгрузки тарифов без программ/тегов")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-empty-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void doNotGenerateForTariffWithoutProgram() {
        generateRevisionFacade.processPayload(createPayload(Set.of(1L)));
    }


    @Test
    @DisplayName("Генерация нового поколения c DAAS тарифами")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateDaasSuccess() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);
        mockGetPartner();

        generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Генерация нового поколения c MARKET_DELIVERY тарифами")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/market-delivery-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-market-delivery-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateMarketDeliverySuccess() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L, 3L);
        mockMdsGetUrl(newMdsFileIds);
        mockGetPartner();

        generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Генерация нового поколения c BERU_CROSSDOCK тарифами")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/beru-crossdock-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-beru-crossdock-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateBeruCrossdockSuccess() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);
        mockGetPartner();

        generateRevisionFacade.processPayload(createPayload(Set.of()));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Генерация поколения с добавлением нового прайс-листа")
    @DatabaseSetup({
        "/tms/revision/before/generate-success.xml",
        "/tms/revision/before/regenerate-success.xml",
    })
    @DatabaseSetup(value = "/tms/revision/before/daas-tags.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void regenerateWithInsertSuccess() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);

        generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 301L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Генерация поколения с завершением прайс-листа")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @DatabaseSetup(value = "/tms/revision/before/archive-10-20-price-lists.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup(value = "/tms/revision/before/regenerate-with-delete.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-with-delete.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void regenerateWithDeleteSuccess() {
        generateRevisionFacade.processPayload(createPayload(Set.of(100L, 201L)));
    }

    @Test
    @DisplayName("Генерация поколения с завершением всех прайс-листов")
    @DatabaseSetup("/tms/revision/before/regenerate-with-delete-all.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-with-delete-all.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void regenerateWithDeleteAllSuccess() {
        mockGetPartner();
        generateRevisionFacade.processPayload(createPayload(Set.of(201L)));
    }


    @Test
    @DisplayName("Генерация поколения с изменением контента выгрузки без изменений в прайс-листах")
    @DatabaseSetup("/tms/revision/before/regenerate-without-any-changes.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-with-change-dataset.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void regenerateWithChangeDatasetContentSuccess() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);

        doReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tariff/>".getBytes(StandardCharsets.UTF_8))
            .when(deliveryCalculatorDatasetGenerator).generate(anyLong());

        generateRevisionFacade.processPayload(createPayload(Set.of(2L, 100L, 201L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Частичная перегенерация поколения с изменениями контента выгрузки")
    @DatabaseSetup("/tms/revision/before/regenerate-without-any-changes.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-partially.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void partiallyRegenerateWithoutAnyChangesUpdateSuccess() {
        Set<Long> newMdsFileIds = Set.of(1L);
        mockMdsGetUrl(newMdsFileIds);

        doReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?><tariff/>".getBytes(StandardCharsets.UTF_8))
            .when(deliveryCalculatorDatasetGenerator).generate(eq(100L));

        generateRevisionFacade.processPayload(createPayload(Set.of(100L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }


    @Test
    @DisplayName("Не создаем новое поколение, если оно не отличается от старого")
    @DatabaseSetup("/tms/revision/before/regenerate-without-any-changes.xml")
    @ExpectedDatabase(
        value = "/tms/revision/before/regenerate-without-any-changes.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void regenerateWithoutAnyChangesUpdateSuccess() {
        generateRevisionFacade.processPayload(createPayload(Set.of(2L, 100L, 201L)));
    }

    @Test
    @DisplayName("Ошибка генерации выгрузки")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @ExpectedDatabase(value = "/tms/revision/after/generate-error.xml", assertionMode = NON_STRICT_UNORDERED)
    void datasetGenerationError() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);
        doThrow(new RuntimeException("Cannot generate dataset"))
            .when(deliveryCalculatorDatasetGenerator).generate(anyLong());

        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Revision created with some errors: " +
                "priceListId: 100, error: Cannot generate dataset; priceListId: 20, error: Cannot generate dataset");
    }

    @Test
    @DisplayName("Ошибка отправки файлов в MDS хранилище")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/upload-error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void mdsUploadError() {
        Set<Long> newMdsFiles = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFiles);
        mockGetPartner();
        doThrow(new MdsS3Exception("Cannot upload file"))
            .when(mdsS3Client).upload(any(), any(ContentProvider.class));

        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Revision created with some errors:" +
                " priceListId: 100, error: Cannot upload file; priceListId: 20, error: Cannot upload file");

        verify(mdsS3Client, times(2)).upload(any(), any(ContentProvider.class));
        verifyMdsGetUrl(newMdsFiles);
    }

    @Test
    @DisplayName("Ошибка генерации одного из элементов поколения")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/one-revision-item-failed.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateDatasetError() {
        long wrongPriceListId = 2L;
        Set<Long> goodMdsFileIds = Set.of(1L);
        mockMdsGetUrl(goodMdsFileIds);

        doThrow(new RuntimeException("Fail"))
            .when(revisionItemService).generateAndSaveDatasetForItem(eq(wrongPriceListId), any());

        // проверяем, что каждая генерация шла в своей транзакции
        // goodPriceListIds revision item'а должны были загрузиться как положено, а wrongPriceListId останется без файла
        doNothing().when(revisionItemService).delete(any());

        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Cannot create revision: An concurrent error occurred during executing tasks");

        verifyMdsGetUrl(goodMdsFileIds);
        verifyMdsUpload(goodMdsFileIds);
    }

    @Test
    @DisplayName("Ошибка отправки одного файла в MDS хранилище")
    @DatabaseSetup({
        "/tms/revision/before/generate-success.xml",
        "/tms/revision/before/regenerate-success.xml",
    })
    @DatabaseSetup(value = "/tms/revision/before/daas-tags.xml", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/tms/revision/after/partial-failure.xml", assertionMode = NON_STRICT_UNORDERED)
    void mdsUploadOneError() {
        List<Long> successMdsFileIds = List.of(1L);
        List<Long> failMdsFileIds = List.of(2L);
        mockMdsGetUrl(successMdsFileIds);
        mockMdsUploadException(failMdsFileIds);

        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 301L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Revision created with some errors: priceListId: 20, error: Cannot upload file");

        verify(mdsS3Client).upload(eq(buildDatasetLocation(failMdsFileIds.get(0))), any(ContentProvider.class));
        verifyMdsGetUrl(ListUtils.union(successMdsFileIds, failMdsFileIds));
        verifyMdsUpload(successMdsFileIds);
    }

    @Test
    @DisplayName("Генерация нового поколения для ПВЗ-тарифа")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @DatabaseSetup("/pickup-points/pickup-points.xml")
    @DatabaseSetup(
        value = "/tms/revision/before/generate-pickup-tariff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/tms/revision/after/generate-pickup-tariff-success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateSuccessForPickupDeliveryMethod() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);
        mockGetPartner();

        generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L)));

        verifyMdsGetUrl(newMdsFileIds);
        verifyMdsUpload(newMdsFileIds);
    }

    @Test
    @DisplayName("Ошибка генерации нового поколения для ПВЗ-тарифа")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @DatabaseSetup(
        value = "/tms/revision/before/generate-pickup-tariff.xml",
        type = DatabaseOperation.REFRESH
    )
    void generateFailedForPickupDeliveryMethod() {
        Set<Long> newMdsFileIds = Set.of(1L, 2L);
        mockMdsGetUrl(newMdsFileIds);

        softly.assertThatCode(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L))))
            .doesNotThrowAnyException();

        assertThat(backLogCaptor.getResults())
            .anyMatch(line -> line.contains("level=WARN"
                + "\tformat=plain"
                + "\tcode=GENERATE_REVISION"
                + "\tpayload=Failed to generate revision for new item, skipping:\\n"
                + "Cannot generate dataset for price-list 100 (Directions are empty)"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=revisionItem"
                + "\tentity_values=revisionItem:1"
            ))
            .anyMatch(line -> line.contains("level=WARN"
                + "\tformat=plain"
                + "\tcode=GENERATE_REVISION"
                + "\tpayload=Failed to generate revision for new item, skipping:\\n"
                + "Cannot generate dataset for price-list 20 (Directions are empty)"
                + "\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd"
                + "\tentity_types=revisionItem"
                + "\tentity_values=revisionItem:2"
            ));
    }

    @Test
    @DisplayName("Ошибка генерации одного из элементов поколения — предыдущее поколение было")
    @DatabaseSetup({
        "/pickup-points/pickup-points.xml",
        "/tms/revision/before/generate-success.xml",
        "/tms/revision/before/generate-pickup-tariff-success.xml"
    })
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @DatabaseSetup(
        value = "/tms/revision/before/generate-pickup-tariff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/tms/revision/after/regenerate-pickup-tariff-one-error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void generateFailedForPickupDeliveryMethodWithPreviousRevision() {
        Set<Long> successMdsFileIds = Set.of(1L);
        mockMdsGetUrl(successMdsFileIds);
        mockGetPartner();

        doThrow(new RuntimeException("Cannot generate dataset for price-list 100"))
            .when(deliveryCalculatorDatasetGenerator).generate(eq(100L));

        // проверяем, что каждая генерация шла в своей транзакции
        // goodPriceListIds revision item'а должны были загрузиться как положено, а wrongPriceListId останется без файла
        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Revision created with some errors: priceListId: 100, error: " +
                "Cannot generate dataset for price-list 100"
            );

        verifyMdsUpload(successMdsFileIds);
        verifyMdsGetUrl(successMdsFileIds);
        verify(revisionItemService, never()).delete(any());
    }

    @Test
    @DisplayName("Ошибка отправки одного файла в MDS хранилище — прайс-лист ранее не принадлежал ревизии")
    @DatabaseSetup("/tms/revision/before/generate-success.xml")
    @DatabaseSetup("/tms/revision/before/daas-tags.xml")
    @ExpectedDatabase(
        value = "/tms/revision/after/partial-failure-file-was-not-in-revision.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void mdsUploadOneErrorPriceListWasNotInRevision() {
        List<Long> successMdsFileIds = List.of(2L);
        List<Long> failMdsFileIds = List.of(1L);
        mockMdsGetUrl(successMdsFileIds);
        mockMdsUploadException(failMdsFileIds);
        mockGetPartner();

        softly.assertThatThrownBy(() -> generateRevisionFacade.processPayload(createPayload(Set.of(20L, 100L, 201L))))
            .isInstanceOf(RevisionGenerationException.class)
            .hasMessage("Revision created with some errors: priceListId: 100, error: Cannot upload file");

        verify(mdsS3Client).upload(eq(buildDatasetLocation(failMdsFileIds.get(0))), any(ContentProvider.class));
        verifyMdsGetUrl(ListUtils.union(successMdsFileIds, failMdsFileIds));
        verifyMdsUpload(successMdsFileIds);
    }

    private void mockMdsGetUrl(Collection<Long> ids) {
        ids.forEach(id -> {
            String filename = buildDatasetFilename(id);
            ResourceLocation location = buildDatasetLocation(id);
            when(resourceLocationFactory.createLocation(eq(filename))).thenReturn(location);
            when(mdsS3Client.getUrl(eq(location))).thenReturn(buildDatasetUrl(id));
        });
    }

    private void mockMdsUploadException(Collection<Long> ids) {
        ids.forEach(id -> {
            String filename = buildDatasetFilename(id);
            ResourceLocation location = buildDatasetLocation(id);
            when(resourceLocationFactory.createLocation(eq(filename))).thenReturn(location);
            when(mdsS3Client.getUrl(eq(location))).thenReturn(buildDatasetUrl(id));
            doThrow(new MdsS3Exception("Cannot upload file"))
                .when(mdsS3Client).upload(eq(buildDatasetLocation(id)), any(ContentProvider.class));
        });
    }

    private void mockGetPartner() {
        when(lmsClient.getPartner(any()))
            .thenAnswer(invocation -> {
                Long partnerId = invocation.getArgument(0);
                return Optional.of(
                    PartnerResponse.newBuilder()
                        .id(partnerId)
                        .name("partner_" + partnerId)
                        .readableName("partner_" + partnerId)
                        .build()
                );
            });
    }

    private void verifyMdsUpload(Collection<Long> ids) {
        ids.forEach(id -> {
            ResourceLocation location = buildDatasetLocation(id);
            verify(mdsS3Client).upload(eq(location), any());
        });
    }

    private void verifyMdsGetUrl(Collection<Long> ids) {
        ids.forEach(id -> {
            ResourceLocation location = buildDatasetLocation(id);
            verify(mdsS3Client).getUrl(eq(location));
        });
    }

    @Nonnull
    private GenerateRevisionPayload createPayload(Set<Long> priceListIds) {
        return new GenerateRevisionPayload("1", Set.of(), priceListIds);
    }
}
