package ru.yandex.market.api.opinion;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.comment.CommentService;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.opinion.ModelOpinionV2;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.guru.BukerClient;
import ru.yandex.market.api.internal.opinion.OpinionHttpService;
import ru.yandex.market.api.internal.opinion.OpinionsSort;
import ru.yandex.market.api.internal.social.SocialApiClient;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.shop.ShopInfoService;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by apershukov on 31.03.17.
 */
@WithContext
@WithMocks
public class OpinionServiceImplTest extends BaseTest {

    private static final long MODEL_ID = 777;
    private static final String LONG_TEXT = "Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, " +
        "несмотря на название, не имеет никакого отношения к обитателям водоемов. Слово. Используется он " +
        "веб-дизайнерами для вставки на интернет-страницы и демонстрации внешнего вида контента, просмотра " +
        "шрифтов, абзацев, отступов и т.д. Так как цель применения такого текста исключительно " +
        "демонстрационная, то и смысловую нагрузку ему нести совсем необязательно.";

    private static final String ALL_OPINIONS_LINK = "<a href=\"http://market.yandex.ru/product/" + MODEL_ID +
        "/reviews?hid=91491&track=partner\">Все отзывы на Яндекс.Маркете</a>";

    private static final String LINK = "<a href=\"http://market.yandex.ru/product/" + MODEL_ID +
        "/reviews?hid=91491&firstReviewId=222&track=partner\">Подробнее на Яндекс.Маркете</a>";

    @Mock
    private BukerClient bukerClient;
    @Mock
    private SocialApiClient socialApiClient;
    @Mock
    private CommentService commentService;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private OpinionHttpService opinionHttpService;
    @Mock
    private PersGradeService persGradeService;
    @Mock
    private OpinionRejectReasonDictonarySupplier opinionRejectReasonDictonarySupplier;
    @Mock
    private ModelService modelService;
    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private UserInfoService userInfoService;

    @Inject
    private MarketUrls marketUrls;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Inject
    private BlueMobileApplicationRule blueMobileApplicationRule;

    private OpinionService opinionService;

    @Before
    public void setUp() throws Exception {
        opinionService = new OpinionServiceImpl(bukerClient, socialApiClient, commentService,
                shopInfoService, opinionHttpService, persGradeService, opinionRejectReasonDictonarySupplier, modelService, geoRegionService,
                marketUrls, urlParamsFactoryImpl, userInfoService, blueMobileApplicationRule);

        when(modelService.getModelAndCheckAccess(eq(MODEL_ID), any()))
            .thenReturn(Pipelines.startWithValue(new ModelV2() {{
            setId(MODEL_ID);
            setCategoryId(91491);
        }}));


        when(geoRegionService.getRegion(Mockito.anyInt(), Mockito.anyCollection(), Mockito.any(RegionVersion.class)))
            .thenReturn(new GeoRegion(1, null, null, null, null));

        ContextHolder.get().setClient(new Client(){{
            setOpinionLength(100);
        }});
    }

    @Test
    public void testDoNotTruncateOpinionForClientWithoutLimit() throws ExecutionException, InterruptedException {
        ContextHolder.get().setClient(new Client(){{
            setOpinionLength(0);
        }});

        prepareOpinion(LONG_TEXT, LONG_TEXT, LONG_TEXT);

        Opinion opinion = getModelOpinion();

        assertEquals(LONG_TEXT, opinion.getText());
        assertEquals(LONG_TEXT, opinion.getPros());
        assertEquals(LONG_TEXT, opinion.getCons());
    }

    @Test
    public void testAddLinkToShortOpinion() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Opinion",
            "With short",
            "text"
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Opinion", opinion.getPros());
        assertEquals("With short", opinion.getCons());

        String expectedText = "text " + ALL_OPINIONS_LINK;
        assertEquals(expectedText, opinion.getText());
    }

    @Test
    public void testAddLinkToShortOpinionWithProsOnly() throws ExecutionException, InterruptedException {
        prepareOpinion("Pros of model", null, null);

        Opinion opinion = getModelOpinion();

        assertEquals("Pros of model " + ALL_OPINIONS_LINK, opinion.getPros());
        assertNull(opinion.getCons());
        assertNull(opinion.getText());
    }

    @Test
    public void testTruncateOpinionTextForClientWithLimit() throws ExecutionException, InterruptedException {
        prepareOpinion(LONG_TEXT, LONG_TEXT, LONG_TEXT);

        Opinion opinion = getModelOpinion();

        assertEquals(LONG_TEXT, opinion.getPros());

        assertEquals("Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, несмотря на название, не " +
            "имеет никакого отношения к обитателям водоемов. Слово. Используется он веб-дизайнерами для " +
            "вставки на интернет-страницы... " + LINK, opinion.getCons());

        assertNull(opinion.getText());
    }

    @Test
    public void testSkipProsIfItIsShort() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Каждый веб-разработчик знает, что такое текст-«рыба».",
            "Текст этот, несмотря на название, не имеет никакого к обитателям водоемов.",
            "Используется он веб-дизайнерами для вставки на интернет-страницы и демонстрации внешнего вида контента"
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Каждый веб-разработчик знает, что такое текст-«рыба».", opinion.getPros());
        assertEquals("Текст этот, несмотря на название, не имеет никакого к обитателям... " + LINK,
            opinion.getCons());
        assertNull(opinion.getText());
    }

    /**
     * Проверка того что в случае если суммарная длина отзыва превышает максимальную на окончание последнего слова
     * к нему добавляется ссылка на все отзывы при этом сам отзыв не обрезается
     */
    @Test
    public void testOpinionWithOnlyProsNotLongEnough() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, несмотря на название, " +
                "не имеет никакого",
            null, null
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, несмотря на название, " +
            "не имеет никакого " + ALL_OPINIONS_LINK, opinion.getPros());
        assertNull(opinion.getCons());
        assertNull(opinion.getText());
    }

    @Test
    public void testOpinionWithOnlyConsNotLongEnough() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Каждый веб-разработчик знает, что такое текст-«рыба». ",
            "Текст этот, несмотря на название, не имеет никакого",
            null
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Каждый веб-разработчик знает, что такое текст-«рыба». ", opinion.getPros());
        assertEquals("Текст этот, несмотря на название, не имеет никакого " + ALL_OPINIONS_LINK, opinion.getCons());
        assertNull(opinion.getText());
    }

    /**
     * Тестирование того что в случае если поле не удалось обрезать многоточие не используется
     * Случается такое когда поле длиннее ограничения всего лишь на окончание последнего слова
     */
    @Test
    public void testOpinionWithOnlyTextNotLongEnough() throws ExecutionException, InterruptedException {
        prepareOpinion(
            null,
            null,
            "Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, несмотря на название, " +
                "не имеет никакого"
        );

        Opinion opinion = getModelOpinion();

        assertNull(opinion.getCons());
        assertNull(opinion.getPros());
        assertEquals("Каждый веб-разработчик знает, что такое текст-«рыба». Текст этот, несмотря на название, " +
            "не имеет никакого " + ALL_OPINIONS_LINK, opinion.getText());
    }

    @Test
    public void testTruncateTextWithNewlines() throws ExecutionException, InterruptedException {
        prepareOpinion(null, null, "Многие пишут, экран отклеивается, стекло трескается. Я пользуюсь почти год - ни " +
            "единого намека на это\r\nКуча оф прошивок: 4.4, 5.1...ожидается 6.0.\r\nМало кастомов " +
            "АОСП...кроме СМ ничего годного нет. Так что если вы любитель...это не ваш аппарат.\r\n\r\n" +
            "Порадовала водонепроницаемость. Купался в море, обронил и не заметил...спустя какое то время " +
            "мой телефон  нашли на дне моря чужие люди(ныряли) и отдали(повезло на людей).");

        Opinion opinion = getModelOpinion();
        assertEquals("Многие пишут, экран отклеивается, стекло трескается. Я пользуюсь почти год - ни единого намека на это\r\n" +
            "Куча оф прошивок: 4.4, 5.1...ожидается 6.0.\r\n" +
            "Мало кастомов АОСП...кроме СМ ничего годного нет. Так что если... " + LINK, opinion.getText());
    }

    /**
     * Тестирование обрезки текста у котрого часть, входящая в ограничение оканчивается точкой
     */
    @Test
    public void testTruncateTextWithDotEndingFittingLine() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Какой-то текст. По функционалу отлично Камера отвратительная, качество корпуса просто ужасное!" +
                "у меня. задняя панель.",
            null, null
        );

        Opinion opinion = getModelOpinion();
        assertEquals("Какой-то текст. По функционалу отлично Камера отвратительная, качество корпуса просто ужасное!" +
            "у меня... " + LINK, opinion.getPros());
    }

    /**
     * Тестирование ограничения длины отзыва у которого в ограничение входит только плюсы и минусы (в полном объеме)
     */
    @Test
    public void testTruncateOpinionWithExactTwoBlocksFitting() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Хорошо держит дорогу. Тихая даже на больших скоростях. В дождь по трассе на 4+.",
            "Пока не обнаружил. Посмотрим на сколько км хватит.",
            "Шкода Октавия. Даже в дождь со снегом ехал вполне уверенно. В поездке по серпантину при +25 - " +
                "ехал отлично в свое удовольствие."
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Хорошо держит дорогу. Тихая даже на больших скоростях. В дождь по трассе на 4+.", opinion.getPros());
        assertEquals("Пока не обнаружил. Посмотрим на сколько км хватит... " + LINK, opinion.getCons());
        assertNull(opinion.getText());
    }

    /**
     * Проверяем ошибку обрезания отзыва выявленную в проде на конкретном примере. Пример входящих данных взят из HeapDump-а
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3795">MARKETAPI-3795: Слишком медленное обрезание отзывов на модели</a>
     */
    @Test
    public void testTruncateOpinionBug3795() throws ExecutionException, InterruptedException {
        prepareOpinion(
            "Изображение и звук телевизора на высоте! И не было бы к нему претензий если бы не...",
            "Ограниченный набор поддержки звуковых форматов из-за чего многие фильмы не идут(или идут без звука)",
            "Вот такие хар-ки должны быть у фалйа что бы он читался, так что смотреть надо заранее на торрентах подходит ли он вам.\n" +
                "File Extension\tContainer\tVideo Codec\tResolution\tFrame Rate (fps)\tBit Rate (Mbps)\tAudio Codec\tUSB\tDLNA\tNet TV\tOpen Internet\tVideo Store\n" +
                "\t\tMPEG4 SP\t640x480p\t30\t3\tAAC\tYes\tYes\tNo\tNo\tNo\n" +
                "\t\tMPEG4 ASP\t720x576p\t30\t8\tMP3/MPEG1 L2/ AC3\tYes\tYes\tNo\tNo\tNo\n" +
                ".mkv\tMKV\tH.264\t1920x1080p\t24\t20\tHE-AAC, AC3, MP3, PCM\tYes\tYes\tNo\tNo\tNo\n" +
                ".asf\tASF\tWMV9/VC1 SP\t352x288\t15\t0,384\tWMA\tYes\tYes\tYes\tNo\tYes\n" +
                ".wmv\t\tWMV9/VC1 MP\t1920x1080p\t25/30\t20\tWMA\tYes\tYes\tYes\tNo\tYes\n" +
                "\t\tWMV9/VC1 AP\t1920x1080\t24/30/60\t20\tWMA\tYes\tYes\tYes\tNo\tYes\n" +
                ".mp4\tMP4\tH.264 BP\t720x576i\t15/30\t5\tAAC/HE-AAC (v1&v2)/AC3\tYes\tYes\tYes\tNo\tYes\n" +
                "\t\tH.264 MP\t720x576i\t15/30\t10\tAAC/HE-AAC (v1&v2)/AC4\tYes\tYes\tYes\tNo\tYes\n" +
                "\t\tH.264 HP\t1920x1080i\t15/30\t20\tAAC/HE-AAC (v1&v2)/AC5\tYes\tYes\tYes\tNo\tYes\n" +
                "\t\tMPEG4 SP\t640x480p\t30\t3\tAAC\tYes\tYes\tYes\tNo\tYes\n" +
                "\t\tMPEG4 ASP\t720x576i\t30\t8\tAAC\tYes\tYes\tYes\tNo\tYes\n" +
                ".ts\tTS\tMPEG2\t1920x1080p\t30\t20\tMPEG2 L2/MPEG1 L2/AC3\tYes\tYes\tNo\tNo\tNo\n" +
                ".mpg\tPS\tMPEG2\t1920x1080p\t30\t20\tMPEG2 L2/MPEG1 L2/AC3\tYes\tYes\tNo\tNo\tNo\n" +
                ".mpeg\t\t\t\t\t\tLPCM\t\t\t\t\t\n" +
                ".vob\t\tMPEG1\t352x288\t30\t1,5\tMPEG1 L2\tYes\tYes\tNo\tNo\tNo\n" +
                ".rmvb\tRMVB\tRealVideo\t848x480\t30\t2\tRA8LBR/RAAC\tYes\tNo\tNo\tNo\tNo\n" +
                ".rm\t\t8,9,10"
        );

        Opinion opinion = getModelOpinion();

        assertEquals("Изображение и звук телевизора на высоте! И не было бы к нему претензий если бы не...", opinion.getPros());
        assertEquals("Ограниченный набор поддержки звуковых форматов из-за чего многие фильмы не идут(или идут без звука)", opinion.getCons());
        assertEquals("Вот такие хар-ки должны быть у фалйа что бы он читался, так что смотреть надо заранее на торрентах подходит ли он вам.\n" +
            "File Extension\tContainer\tVideo Codec\tResolution\tFrame Rate (fps)\tBit Rate (Mbps)\tAudio Codec\tUSB\tDLNA\tNet TV\tOpen Internet\tVideo Store\n" +
            "\t\tMPEG4 SP\t640x480p\t30\t3\tAAC\tYes\tYes\tNo\tNo\tNo\n" +
            "\t\tMPEG4 ASP\t720x576p\t30\t8\tMP3/MPEG1 L2/ AC3\tYes\tYes\tNo\tNo\tNo\n" +
            ".mkv\tMKV\tH.264\t1920x1080p\t24\t20\tHE-AAC, AC3, MP3, PCM\tYes\tYes\tNo\tNo\tNo\n" +
            ".asf\tASF\tWMV9/VC1 SP\t352x288\t15\t0,384\tWMA\tYes\tYes\tYes\tNo\tYes\n" +
            ".wmv\t\tWMV9/VC1 MP\t1920x1080p... " + LINK, opinion.getText());
    }

    private Opinion getModelOpinion() throws InterruptedException, ExecutionException {
        return opinionService.getModelOpinions(MODEL_ID, null, 0, new OpinionsSort(),
            PageInfo.DEFAULT, new OauthUser(111), Collections.emptyList()).get().getOpinions().getElements().get(0);
    }

    private void prepareOpinion(String pros, String cons, String text) {
        Opinion opinion = new ModelOpinionV2() {{
            setId(222);
            setModel(new Model(MODEL_ID));
            setText(text);
            setPros(pros);
            setCons(cons);
        }};

        when(opinionHttpService.getModelOpinions(eq(MODEL_ID), anyInt(), any(PageInfo.class), any(OpinionsSort.class)))
            .thenReturn(Pipelines.startWithValue(
                new PagedResult<>(Collections.singletonList(opinion),
                    PageInfo.fromTotalElements(1, 10, 1))));
    }
}
