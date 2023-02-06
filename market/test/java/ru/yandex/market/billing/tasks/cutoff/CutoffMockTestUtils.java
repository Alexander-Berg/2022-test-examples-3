package ru.yandex.market.billing.tasks.cutoff;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.mockito.ArgumentCaptor;

import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.CloseCutoffsRequest;
import ru.yandex.market.mbi.open.api.client.model.ClosingCutoff;
import ru.yandex.market.mbi.open.api.client.model.Cutoff;
import ru.yandex.market.mbi.open.api.client.model.GetCutoffsResponse;
import ru.yandex.market.mbi.open.api.client.model.OpenCutoffsRequest;
import ru.yandex.market.mbi.open.api.client.model.OpeningCutoff;
import ru.yandex.market.mbi.util.Functional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.util.Functional.mapToSet;

public class CutoffMockTestUtils {

    public static void mockGetCutoffResponseWithCutoff(
            MbiOpenApiClient mbiOpenApiClient,
            CutoffType cutoffType,
            Long datasourceId) {
        mockGetCutoffResponseWithCutoffs(mbiOpenApiClient, cutoffType, Set.of(datasourceId));
    }

    public static void mockGetCutoffResponseWithCutoffs(
            MbiOpenApiClient mbiOpenApiClient,
            CutoffType cutoffType,
            Set<Long> datasourceIds) {
        Function<Long, Cutoff> cutoffCreator =
                datasourceId -> (Cutoff) new Cutoff()
                        .datasourceId(datasourceId)
                        .cutoffType(cutoffType.getId());
        List<Cutoff> cutoffs = Functional.mapToList(datasourceIds, cutoffCreator);
        GetCutoffsResponse response = new GetCutoffsResponse().cutoffs(cutoffs);
        when(mbiOpenApiClient.getCutoffs(cutoffType.getId())).thenReturn(response);
    }

    public static CloseCutoffsRequest getActualCloseCutoffRequest(MbiOpenApiClient mbiOpenApiClient) {
        ArgumentCaptor<CloseCutoffsRequest> closeArgumentCaptor = ArgumentCaptor.forClass(CloseCutoffsRequest.class);
        verify(mbiOpenApiClient).closeCutoffs(closeArgumentCaptor.capture());
        return closeArgumentCaptor.getValue();
    }

    public static OpenCutoffsRequest getActualOpenCutoffRequest(MbiOpenApiClient mbiOpenApiClient) {
        ArgumentCaptor<OpenCutoffsRequest> openArgumentCaptor = ArgumentCaptor.forClass(OpenCutoffsRequest.class);
        verify(mbiOpenApiClient).openCutoffs(openArgumentCaptor.capture());
        return openArgumentCaptor.getValue();
    }

    public static void checkOpeningCutoff(
            OpenCutoffsRequest actualOpenCutoffsRequest,
            CutoffType expectedCutoffType,
            Long expectedDatasourceId,
            Integer expectedNotificationTemplateId) {
        checkOpeningCutoffs(actualOpenCutoffsRequest, expectedCutoffType,
                Set.of(expectedDatasourceId), expectedNotificationTemplateId);
    }

    public static void checkOpeningCutoffs(
            OpenCutoffsRequest actualOpenCutoffsRequest,
            CutoffType expectedCutoffType,
            Set<Long> expectedDatasourceIds,
            Integer expectedNotificationTemplateId) {
        assertThat(actualOpenCutoffsRequest.getCutoffs()).isNotNull();

        Set<Integer> actualTypes = mapToSet(actualOpenCutoffsRequest.getCutoffs(), OpeningCutoff::getCutoffType);
        assertThat(actualTypes).isEqualTo(Set.of(expectedCutoffType.getId()));

        Set<Long> actualDatasourceIds = mapToSet(actualOpenCutoffsRequest.getCutoffs(), OpeningCutoff::getDatasourceId);
        assertThat(actualDatasourceIds).isEqualTo(expectedDatasourceIds);

        Set<Integer> actualTemplateIds =
                mapToSet(actualOpenCutoffsRequest.getCutoffs(), OpeningCutoff::getNotificationTemplateId);
        assertThat(actualTemplateIds).isEqualTo(Set.of(expectedNotificationTemplateId));
    }

    public static void checkClosingCutoff(
            CloseCutoffsRequest actualCloseCutoffsRequest,
            CutoffType expectedCutoffType,
            Long expectedDatasourceId) {
        checkClosingCutoffs(actualCloseCutoffsRequest, expectedCutoffType, Set.of(expectedDatasourceId));
    }

    public static void checkClosingCutoffs(
            CloseCutoffsRequest actualCloseCutoffsRequest,
            CutoffType expectedCutoffType,
            Set<Long> expectedDatasourceIds) {
        assertThat(actualCloseCutoffsRequest.getCutoffs()).isNotNull();

        Set<Integer> actualTypes = mapToSet(actualCloseCutoffsRequest.getCutoffs(), ClosingCutoff::getCutoffType);
        assertThat(actualTypes).isEqualTo(Set.of(expectedCutoffType.getId()));

        Set<Long> actualDatasourceIds = mapToSet(actualCloseCutoffsRequest.getCutoffs(),
                ClosingCutoff::getDatasourceId);
        assertThat(actualDatasourceIds).isEqualTo(expectedDatasourceIds);
    }
}
