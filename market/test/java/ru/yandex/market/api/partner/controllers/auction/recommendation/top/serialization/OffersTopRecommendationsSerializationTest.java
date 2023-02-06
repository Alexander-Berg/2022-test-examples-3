package ru.yandex.market.api.partner.controllers.auction.recommendation.top.serialization;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.api.partner.controllers.auction.model.ImpossibleRecommendation;
import ru.yandex.market.api.partner.controllers.auction.model.OfferTopRecommendations;
import ru.yandex.market.api.partner.controllers.auction.model.OffersTopRecommendations;
import ru.yandex.market.api.partner.controllers.auction.model.TopRecommendationType;
import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;
import ru.yandex.market.api.partner.response.ApiResponseV2;

import static ru.yandex.market.mbi.util.MoneyValuesHelper.centsToUE;

/**
 * Проверка сериализации ТОПов рекомендаций
 *
 * @author chmilevfa@yandex-team.ru
 * @since 08.06.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class OffersTopRecommendationsSerializationTest extends BaseJaxbSerializationTest {

    /**
     * Проверка сериализации корректно сформированных рекомендаций для офферов
     * с несколькими рекомендациями и рекомендуемыми позициями
     */
    @Test
    public void SerializeCorrectOffersTopRecommendationsTest() throws Exception {
        OffersTopRecommendations expected = new OffersTopRecommendations();

        //Заполняем информацию по оферу
        OfferTopRecommendations offerTopRecommendations = new OfferTopRecommendations("tst1234567", 1234L);
        offerTopRecommendations.setName("Телефон Apple iPhone SE 16GB 1723 (Розовое Золото) RU/A");
        offerTopRecommendations.setBid(centsToUE(15));
        offerTopRecommendations.setMinBid(centsToUE(16));

        //Заполняем данные по рекомендациям
        OfferTopRecommendations.TopQueries topQueries1 = new OfferTopRecommendations.TopQueries();
        topQueries1.setQuery("Apple iPhone SE 16");
        topQueries1.setType(TopRecommendationType.TOP_ALL);
        topQueries1.setAverageOfferPosition(59);
        topQueries1.setOfferShowCount(3192);
        topQueries1.setQueryShowCount(31);
        topQueries1.setModelCount(0);
        topQueries1.setRecommendationsList(new ArrayList<>(Arrays.asList(
                new OfferTopRecommendations.TopQueries.Recommendations(1, centsToUE(403)),
                new OfferTopRecommendations.TopQueries.Recommendations(2, centsToUE(363)))
        ));

        OfferTopRecommendations.TopQueries topQueries2 = new OfferTopRecommendations.TopQueries();
        topQueries2.setQuery("iphone se");
        topQueries2.setType(TopRecommendationType.TOP_OFFER);
        topQueries2.setAverageOfferPosition(31);
        topQueries2.setOfferShowCount(30675);
        topQueries2.setQueryShowCount(20);
        topQueries2.setModelCount(2);
        topQueries2.setRecommendationsList(new ArrayList<>(Arrays.asList(
                new OfferTopRecommendations.TopQueries.Recommendations(1, centsToUE(296)),
                new OfferTopRecommendations.TopQueries.Recommendations(2, centsToUE(196)))
        ));

        offerTopRecommendations.setTopQueries(new ArrayList<>(Arrays.asList(topQueries1, topQueries2)));
        expected.addOfferRecommendations(offerTopRecommendations);

        ApiResponseV2 apiResponseV2 = ApiResponseV2.ok(expected);

        try (InputStream jsonInputStream = this.getClass().getResourceAsStream("OffersTopRecommendations.json");
             InputStream xmlInputStream = this.getClass().getResourceAsStream("OffersTopRecommendations.xml")) {
            testSerialization(apiResponseV2,
                    IOUtils.toString(jsonInputStream, "UTF-8"),
                    IOUtils.toString(xmlInputStream, "UTF-8"));
        }
    }

    /**
     * Проверка сериализации объекта с данными оффера но без рекомендаций
     */
    @Test
    public void SerializeOffersWithoutRecommendationsTest() throws Exception {
        OffersTopRecommendations expected = new OffersTopRecommendations();

        //Заполняем информацию по оферу
        OfferTopRecommendations offerTopRecommendations = new OfferTopRecommendations("tst1234567", 1234L);
        offerTopRecommendations.setName("Телефон Apple iPhone SE 16GB 1723 (Розовое Золото) RU/A");
        offerTopRecommendations.setBid(centsToUE(15));
        offerTopRecommendations.setMinBid(centsToUE(16));

        expected.addOfferRecommendations(offerTopRecommendations);

        ApiResponseV2 apiResponseV2 = ApiResponseV2.ok(expected);

        try (InputStream jsonInputStream = this.getClass().getResourceAsStream("OffersWithoutRecommendations.json");
             InputStream xmlInputStream = this.getClass().getResourceAsStream("OffersWithoutRecommendations.xml")) {
            testSerialization(apiResponseV2,
                    IOUtils.toString(jsonInputStream, "UTF-8"),
                    IOUtils.toString(xmlInputStream, "UTF-8"));
        }
    }

    /**
     * Проверка сериализации сформированных рекомендаций для офферов
     * с рекомендацией и ошибками в рекомендуемых позициях
     */
    @Test
    public void SerializeOffersWithUnreachableTopRecommendationsTest() throws Exception {
        OffersTopRecommendations expected = new OffersTopRecommendations();

        //Заполняем информацию по оферу
        OfferTopRecommendations offerTopRecommendations = new OfferTopRecommendations("tst1234567", 1234L);
        offerTopRecommendations.setName("Телефон Apple iPhone SE 16GB 1723 (Розовое Золото) RU/A");
        offerTopRecommendations.setBid(centsToUE(150));
        offerTopRecommendations.setMinBid(centsToUE(15));

        //Заполняем данные по рекомендациям
        OfferTopRecommendations.TopQueries topQueries1 = new OfferTopRecommendations.TopQueries();
        topQueries1.setQuery("Apple iPhone SE 16");
        topQueries1.setType(TopRecommendationType.TOP_ALL);
        topQueries1.setAverageOfferPosition(59);
        topQueries1.setOfferShowCount(3192);
        topQueries1.setQueryShowCount(31);
        topQueries1.setModelCount(0);
        topQueries1.setRecommendationsList(new ArrayList<>(Arrays.asList(
                new OfferTopRecommendations.TopQueries.Recommendations(1, centsToUE(1)),
                new OfferTopRecommendations.TopQueries.Recommendations(2, ImpossibleRecommendation.UNKNOWN),
                new OfferTopRecommendations.TopQueries.Recommendations(3, ImpossibleRecommendation.UNREACHABLE))
        ));

        offerTopRecommendations.setTopQueries(new ArrayList<>(Collections.singletonList(topQueries1)));
        expected.addOfferRecommendations(offerTopRecommendations);

        ApiResponseV2 apiResponseV2 = ApiResponseV2.ok(expected);

        try (InputStream jsonInputStream =
                     this.getClass().getResourceAsStream("OffersWithUnreachableTopRecommendations.json");
             InputStream xmlInputStream =
                     this.getClass().getResourceAsStream("OffersWithUnreachableTopRecommendations.xml")) {
            testSerialization(apiResponseV2,
                    IOUtils.toString(jsonInputStream, "UTF-8"),
                    IOUtils.toString(xmlInputStream, "UTF-8"));
        }
    }

    /**
     * Проверка сериализации пустого списка рекомендаций
     */
    @Test
    public void SerializeEmptyTopRecommendationsTest() throws Exception {
        OffersTopRecommendations expected = new OffersTopRecommendations();

        ApiResponseV2 apiResponseV2 = ApiResponseV2.ok(expected);

        try (InputStream jsonInputStream = this.getClass().getResourceAsStream("EmptyTopRecommendations.json");
             InputStream xmlInputStream = this.getClass().getResourceAsStream("EmptyTopRecommendations.xml")) {
            testSerialization(apiResponseV2,
                    IOUtils.toString(jsonInputStream, "UTF-8"),
                    IOUtils.toString(xmlInputStream, "UTF-8"));
        }
    }
}
