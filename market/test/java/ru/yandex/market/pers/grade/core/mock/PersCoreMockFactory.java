package ru.yandex.market.pers.grade.core.mock;

import java.math.BigDecimal;
import java.util.UUID;

import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.pers.grade.core.PersCoreMockConfig;
import ru.yandex.market.pers.grade.core.article.service.PreviewCreatorServiceImpl;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Prices;
import ru.yandex.market.report.model.ProductType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 26.12.16
 */
public class PersCoreMockFactory {

    public static Model generateModel() {
        Model result = new Model();
        result.setId(Math.abs(PersCoreMockConfig.RND.nextLong()));
        result.setName(UUID.randomUUID().toString());
        result.setType(ProductType.MODEL);
        Category category = new Category(1, "category");
        result.setCategory(category);
        Prices prices = new Prices(100.0, 1.0, 199.0, BigDecimal.ZERO, Currency.RUR);
        result.setPrices(prices);
        return result;
    }

    protected void doNoting() {
    }

    public static void okRestTemplate(RestTemplate restTemplate) {
        when(restTemplate.postForEntity(any(), any(), any(), anyMap())).thenReturn(ResponseEntity.ok().build());
        when(restTemplate.getForEntity(any(), any(), anyMap())).thenReturn(ResponseEntity.ok().build());
    }

    public static void brokenRestTemplate(RestTemplate restTemplate) {
        when(restTemplate.postForEntity(any(),
            any(),
            any(),
            anyMap())).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        when(restTemplate.getForEntity(any(),
            any(),
            anyMap())).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public static void goodPreviewPublishRestTemplate(RestTemplate restTemplate) {
        whenPreviewBukerGet(restTemplate)
            .thenReturn(ResponseEntity.ok(PreviewCreatorServiceImpl.BUKER_OK_ANSWER_SUBSTRING));
        whenPreviewSaasPost(restTemplate)
            .thenReturn(ResponseEntity.ok(new PreviewCreatorServiceImpl.SaasPreviewObject[0]));
    }

    public static void goodBukerAndBrokenSaasRestTemplate(RestTemplate restTemplate) {
        whenPreviewBukerGet(restTemplate)
            .thenReturn(ResponseEntity.ok(PreviewCreatorServiceImpl.BUKER_OK_ANSWER_SUBSTRING));
        whenPreviewSaasPost(restTemplate)
            .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    public static void goodSaasAndBrokenBukerRestTemplate(RestTemplate restTemplate) {
        whenPreviewBukerGet(restTemplate)
            .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        whenPreviewSaasPost(restTemplate)
            .thenReturn(ResponseEntity.ok(new PreviewCreatorServiceImpl.SaasPreviewObject[0]));
    }

    private static OngoingStubbing<ResponseEntity<String>> whenPreviewBukerGet(RestTemplate restTemplate) {
        return when(restTemplate.getForEntity(any(), eq(String.class), anyMap()));
    }

    private static OngoingStubbing<ResponseEntity<PreviewCreatorServiceImpl.SaasPreviewObject[]>> whenPreviewSaasPost(
        RestTemplate restTemplate
    ) {
        return when(restTemplate.postForEntity(
            any(),
            any(),
            eq(PreviewCreatorServiceImpl.SaasPreviewObject[].class),
            anyMap()));
    }

}
