package ru.yandex.autotests.market.partner.api.campaigns.bids;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.common.matchers.CompositeMatcher;
import ru.yandex.autotests.market.common.wiki.PartnerApiRecommendedTargetedTestDataFromWiki;
import ru.yandex.autotests.market.common.wiki.PartnerApiWikiBeanWithTags;
import ru.yandex.autotests.market.common.wiki.bids.PostRecommendedTargetedTestCasesFromWiki;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.partner.api.data.bids.RecommendedTargetedBidsDataFromWikiSteps;
import ru.yandex.autotests.market.partner.api.steps.RecommendedBidsSteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.Target;
import ru.yandex.autotests.market.partner.beans.api.bids.recommended.body.Offers;
import ru.yandex.autotests.market.report.beans.search.bidsrecommenderresponse.BidsRecommenderResponse;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.autotests.market.report.util.query.ReportRequest;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.autotests.market.common.wiki.PartnerApiRecommendedTargetedTestDataFromWiki.getTestDataFromWiki;
import static ru.yandex.autotests.market.common.wiki.bids.PostRecommendedTargetedTestCasesFromWiki.getPostRecommendedTargetedTestCasesFromWiki;

/**
 * Тесты соответсвия выдачи репорта и выдачи АПИ в ручках получения рекомендаций АПИ.
 * Проверяются варианты запросов к АПИ с разными форматами:
 * - без явного указания формата выдачи
 * - формат выдачи JSON
 * - форма выдачи XML
 *
 * Тест кейсы формируются на основе данных из wiki(ключ для связи - поле "Tags"):
 * - данные для тестов(токены,магазины,офферы) - {@link RecommendedTargetedBidsDataFromWikiSteps}
 * - кейсы которые используют данные(таргеты)  - {@link PostRecommendedTargetedTestCasesFromWiki}
 * - явки пароли лежат в секретнице, пользуются через {@link ProjectConfig}.
 *
 */
@Feature("Campaign recommended bids. METHOD=POST")
@Aqua.Test(title = "Запрос рекомендованных ставок для кампании")
@RunWith(Parameterized.class)
public class PostRecommendedTargetedTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private RecommendedBidsSteps tester = new RecommendedBidsSteps();

    private PartnerApiRequestData apiRequestData;
    private Map<String, ReportRequest> reportRequests;
    private Offers offers;
    private Target target;
    private Integer[] positions;

    @Parameterized.Parameters(name = "{index}: case={1} dataTags={0}")
    public static Collection<Object[]> parameters() {
        return combineTestDataAndTestCasesFromWiki(getPostRecommendedTargetedTestCasesFromWiki());
    }

    /**
     * Готовим список кейсов для параметризованного запуска на основе данных тестов и описании кейсов, по сути
     */
    private static Collection<Object[]> combineTestDataAndTestCasesFromWiki(Collection<? extends PartnerApiWikiBeanWithTags> testCases) {
        List<PartnerApiRecommendedTargetedTestDataFromWiki> testData = getTestDataFromWiki();
        return testCases.stream()
                .flatMap(
                        testCase -> getItemsWithTags(testData, testCase.getTags())
                                .map(partnerApiTestDataFromWiki -> new Object[]{partnerApiTestDataFromWiki, testCase})
                )
                .collect(Collectors.toList());
    }

    /**
     * Выбираем только те записи {@link PartnerApiWikiBeanWithTags}, для которых в тэгах перечисленны все элементы из tags..
     */
    private static <T extends PartnerApiWikiBeanWithTags> Stream<T> getItemsWithTags(Collection<T> taggedDataRecords, Collection<String> tags) {
        return taggedDataRecords.stream().filter(record -> record.getTags().containsAll(tags));
    }


    public PostRecommendedTargetedTest(PartnerApiRecommendedTargetedTestDataFromWiki testData, PostRecommendedTargetedTestCasesFromWiki testCase) {
        RecommendedTargetedBidsDataFromWikiSteps testDataSteps = new RecommendedTargetedBidsDataFromWikiSteps(testData, testCase);
        this.apiRequestData = testDataSteps.postRecommendedRequest();
        this.offers = testDataSteps.defaultOffersBody();
        this.target = Target.valueOf(testCase.getTarget());
        this.positions = testCase.getPositions().toArray(new Integer[0]);
        this.reportRequests = testDataSteps.recommendedReportRequests(this.target);
    }

    /**
     * Проверяем, что ответ рекомендаций в ручке АПИ корректно транслирует данные из репорта.
     * Запрос в АПИ с явным указанием формата данных ответа в виде XML.
     */
    @Title("Ответ АПИ рекомендаций в формате XML")
    @Test
    public void testApiResponseAsXML() {
        tester.isXmlApiResponseCorrect(
                offers,
                reportRequests,
                apiRequestData.withTarget(target).withPositions(positions).withFormat(Format.XML)
        );
    }

    /**
     * Проверяем, что ответ рекомендаций в ручке АПИ корректно транслирует данные из репорта.
     * Запрос в АПИ с явным указанием формата данных ответа в виде JSON.
     */
    @Title("Ответ АПИ рекомендаций в формате JSON")
    @Test
    public void testApiResponseAsJson() {
        tester.isJsonApiResponseCorrect(
                offers,
                reportRequests,
                apiRequestData.withTarget(target).withPositions(positions).withFormat(Format.JSON)
        );
    }

    /**
     * Проверяем, что ответ рекомендаций в ручке АПИ корректно трансилрует данные из репорта.
     * Запрос в ПАПИ без указания типа формата выдачи - отдает дефолтный в виде XML.
     */
    @Title("Ответ АПИ рекомендаций в формате по-умолчаниию(XML)")
    @Test
    public void testApiResponseDefault() {
        tester.isXmlApiResponseCorrect(
                offers,
                reportRequests,
                apiRequestData.withTarget(target).withPositions(positions)
        );
    }

}
