package ru.yandex.direct.queryrec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.queryrec.model.Language;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.queryrec.QueryrecService.ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE;
import static ru.yandex.direct.queryrec.model.Language.RUSSIAN;
import static ru.yandex.direct.queryrec.model.Language.UZBEK;
import static ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryrecServiceRecognizeLanguageUzbTest {

    private static QueryrecJni queryrecJni = new QueryrecJni(true);
    private QueryrecService queryrecService;

    private PpcProperty<Set<Long>> clientsWithEnabledUzbekLanguageProperty;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        clientsWithEnabledUzbekLanguageProperty = mock(PpcProperty.class);
        PpcProperty<Set<Long>> enableVieLanguageProperty = mock(PpcProperty.class);
        doReturn(emptySet()).when(enableVieLanguageProperty).getOrDefault(emptySet());
        PpcProperty<Set<String>> recognizedLanguagesProperty = mock(PpcProperty.class);
        doReturn(Set.of()).when(recognizedLanguagesProperty).getOrDefault(Set.of());

        UzbekLanguageThresholds thresholds = new UzbekLanguageThresholds(mock(PpcProperty.class),
                mock(PpcProperty.class), mock(PpcProperty.class), mock(PpcProperty.class));

        // так как используем Lifecycle.PER_CLASS, для тестов инициализируется один экземпляр тестового класса
        queryrecService = new QueryrecService(new LanguageRecognizer(), clientsWithEnabledUzbekLanguageProperty,
                enableVieLanguageProperty, recognizedLanguagesProperty, thresholds, queryrecJni);
    }

    @AfterAll
    static void afterAll() {
        queryrecJni.destroy();
    }

    static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // [UZBEK] : {RUSSIAN=0.290894571576185} : null
                {"Мен бўйдоқман (турмуш қурмаганман)", UZBEK},
                {"Узр, куттириб қўйдим", UZBEK},
                {"Марказгача етиб қўёлмайсизми", UZBEK},
                {"Қанча тез бўлса, шунча яхши", UZBEK},
                {"Эртага кун очиқ бўлади", UZBEK},
                {"Қизиқ, момақалдироқ бўлармикан", UZBEK},
                {"Йўқ, бўйдоқман/турмуш қурмаганман (уйланмаганман)", UZBEK},
                {"Менинг эрим ўқитувчи", UZBEK},
                {"Менинг қариндошларим кўп", UZBEK},
                {"Ёшроқ кўринасиз", UZBEK},
                {"Унинг сочлари оч/тўқ рангда", UZBEK},
                {"Бўйингиз қанча", UZBEK},
                {"Менинг ишим эрталаб соат тўққиздан бошланади", UZBEK},

                // [UZBEK] : {RUSSIAN=0.257310838816678, BELARUSIAN=0.9024432786559141} : null
                {"Яқин орада кўришгунча", UZBEK},

                // [UZBEK] : {UNKNOWN=0.9937524289485687, RUSSIAN=0.2833712650890286} : null
                {"Шакар қўшайми", UZBEK},

                // [UZBEK] : {RUSSIAN=0.4107115177356548, KAZAKH=0.9866734761607915} : null
                {"унинг кўриниши қандай", UZBEK},

                // [RUSSIAN, UKRAINIAN, BELARUSIAN, KAZAKH, UZBEK] : {UNKNOWN=0.9898974210171206, RUSSIAN=0
                // .10226352131757332} : RUSSIAN
                {"Катта рахмат", UZBEK},

                // [BELARUSIAN, UZBEK] : {RUSSIAN=0.46797846579509866} : null
                {"Хафа бўлманг", UZBEK},
                {"Кимни сўрайман", UZBEK},
                {"Сизни яна кўришимдан хурсандман", UZBEK},
                {"Кўришмаганимизга анча бўлди", UZBEK},
                {"Юринг, чўмилгани борамиз", UZBEK},
                {"Илтимос, илтифот кўрсатинг", UZBEK},
                {"Илтимос, кўтаришиб юборинг", UZBEK},
                {"Илтимос, мана бу пакетларни кўтаришиб юборинг", UZBEK},
                {"Хатимни жўнатиб юборолмайсизми", UZBEK},
                {"Манзилингизни билсам бўладими", UZBEK},
                {"Ўзингизни босинг", UZBEK},
                {"Соат неччи бўлди", UZBEK},
                {"Соат етти бўлди", UZBEK},
                {"Кеч бўлдими", UZBEK},
                {"Унинг кўзлари жигарранг/кўк", UZBEK},
                {"Мен кўп ишлашим керак", UZBEK},

                // [BELARUSIAN, UZBEK] : {} : null
                {"Кўришгунча", UZBEK},

                // [KAZAKH, UZBEK] : {RUSSIAN=0.24120970842258} : null
                {"Нечта (қанча)", UZBEK},
                {"Мархамат қилиб бу ерга", UZBEK},
                {"Мен адашиб қолдим", UZBEK},
                {"Мен уйланганман (турмуш қурганман)", UZBEK},
                {"Мана менинг ташриф қоғозим", UZBEK},
                {"Яхши қолинг", UZBEK},
                {"Кейинги учрашувимизни орзиқиб кутаман", UZBEK},
                {"Сизни хафа қилмоқчи эмасдим", UZBEK},
                {"Халақит бермайманми", UZBEK},
                {"Илтимос, яна бир қайтаринг", UZBEK},
                {"Илтимос, секинроқ гапиринг", UZBEK},
                {"Илтимос шифокорни чақиринг", UZBEK},
                {"Аниқ билмайман", UZBEK},
                {"Унинг гапига парво қилманг", UZBEK},
                {"У ерга қанча вақт пиёда юриш керак", UZBEK},
                {"Муваффақият тилайман", UZBEK},
                {"Иссиқ", UZBEK},
                {"Сиз (уйланганмисиз) турмуш қурганмисиз", UZBEK},
                {"Турмушга чиққанман (уйланганман)", UZBEK},
                {"Улар менинг яқин/узоқ қариндошларим", UZBEK},
                {"Қачон/қаерда туғилгансиз", UZBEK},
                {"Мен 1985-йил туғилганман/Тошкентда туғилганман", UZBEK},
                {"Қомати келишган", UZBEK},
                {"Ёқимли табассум", UZBEK},
                {"Қачон маош оласиз", UZBEK},

                // [KAZAKH, UZBEK] : {UNKNOWN=0.9961427716891255, RUSSIAN=0.4010008311749466} : null
                {"Қанча вақт олади", UZBEK},
                {"Ақл бовар қилмайди (Ажабо!)", UZBEK},
                {"Қанча вақт олади", UZBEK},
                {"Қанча вақт қолди", UZBEK},
                {"Сизга узоқ умр ва бахт-саодат тилайман", UZBEK},
                {"Чин қалбимдан табриклайман", UZBEK},
                {"Тезроқ соғайиб кетинг", UZBEK},
                {"Собиқ эрим", UZBEK},
                {"Собиқ хотиним", UZBEK},
                {"Вазнингиз қанча", UZBEK},

                // [RUSSIAN, UKRAINIAN, BELARUSIAN, KAZAKH, UZBEK] : {RUSSIAN=0.3872220111776733} : RUSSIAN
                {"Кечиринг, яхши эшитмадим", UZBEK},
                {"Хайрли кеч", UZBEK},
                {"Танишганимдан хурсандман", UZBEK},
                {"Ишларингиз юришяптими", UZBEK},
                {"Янгиликлар борми", UZBEK},
                {"Исмингиз нима", UZBEK},
                {"Сизни га таништиришга рухсат этинг", UZBEK},
                {"га мендан салом айтинг", UZBEK},
                {"Хабарлашиб турайлик", UZBEK},
                {"Мен билан харидга борасизми", UZBEK},
                {"Яхши таклиф", UZBEK},
                {"Бу менинг айбим", UZBEK},
                {"Кечикканим учун узр", UZBEK},
                {"Илтимос, мана бу ерга ёзинг", UZBEK},
                {"Илтимос, шошилинг", UZBEK},
                {"Бу нима", UZBEK},
                {"Бу нима дегани", UZBEK},
                {"Ручкангизни бериб турмайсизми", UZBEK},
                {"Бундай деб уйламайман", UZBEK},
                {"Менимча адашдингиз", UZBEK},
                {"Кайфиятим ёмон", UZBEK},
                {"Бу яхши эмас", UZBEK},
                {"Тушкунликка тушманг", UZBEK},
                {"Чорак кам саккиз", UZBEK},
                {"Саккиз ярим", UZBEK},
                {"Янги йилда сизга энг яхши тилаклар тилайман", UZBEK},
                {"Яхши бориб келинг", UZBEK},
                {"Келинг, яна учрашамиз", UZBEK},
                {"Мен ажрашганман", UZBEK},
                {"Фарзандларингиз борми", UZBEK},
                {"Ака (ука), опа (сингиз) ларингиз борми", UZBEK},
                {"Уларнинг фарзандлари нечта", UZBEK},
                {"Ёшингиз нечада", UZBEK},
                {"Мен йигирма бирдаман (ёшман)", UZBEK},
                {"У дилбар", UZBEK},
                {"Новча/узун", UZBEK},
                {"Баданингиз рангидан, жанубданмисиз", UZBEK},
                {"Касбингиз нима", UZBEK},
                {"Ишингизни неччида бошлайсиз", UZBEK},
                {"Иш кунингиз неча соат", UZBEK},
                {"Иш куним 7 соат", UZBEK},
                {"Бугун дам олиш куним", UZBEK},
                {"Мен таътилдаман", UZBEK},
                {"Менинг ишим катта маошли", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {ENGLISH=0.41662011139421246} : ENGLISH
                {"O'zbekchagapiramiz!", UZBEK},
                {"Uchrashuv, salomlashuv", UZBEK},
                {"Assalomu alaykum!", UZBEK},
                {"Khush kelibsiz!", UZBEK},
                {"Marhamat, otiring", UZBEK},
                {"Ismingiz nima?", UZBEK},
                {"Ruxsat eting, o'zimni tanishtiray. Men...", UZBEK},
                {"Sizga o'zimni tanishtirsam. Mening ismim...", UZBEK},
                {"Tanishganimizdan xursandman!", UZBEK},
                {"Qalaysiz? Tuzukmisiz?", UZBEK},
                {"Tanishganimizdan xursandman!", UZBEK},
                {"Men...dan keldim", UZBEK},
                {"Oq yul!", UZBEK},
                {"Iltifotingiz uchun tashakkur!", UZBEK},
                {"Rozilik. Norozilik", UZBEK},
                {"O'ylab ko'raman", UZBEK},
                {"Yo'q, albatta", UZBEK},
                {"Iloji yo'q", UZBEK},
                {"So'z berolmayman", UZBEK},
                {"Kechirasiz, bandman", UZBEK},
                {"Ishim ko'p", UZBEK},
                {"Men bora olmayman", UZBEK},
                {"Ming afsus", UZBEK},
                {"Men ayibdorman", UZBEK},
                {"Hechqisi yo'q", UZBEK},
                {"Uzr so'rashning hojati yuq", UZBEK},
                {"Zarari yo'q", UZBEK},
                {"Til va suhbat", UZBEK},
                {"Siz Ruscha gaplashasizmi?", UZBEK},
                {"Men...gapiraman(gapirmayman)", UZBEK},
                {"Men ozgina...gapiraman", UZBEK},
                {"Men sizni tushunaman (tushunmayman)", UZBEK},
                {"Sekinroq gapiring", UZBEK},
                {"Men O'zbek tilini tushunaman ammo gapirmayman", UZBEK},
                {"...Qanday topsam boladi?", UZBEK},
                {"...Qanday o'tsam boladi?", UZBEK},
                {"...Qanday borsam boladi?", UZBEK},
                {"Men to'g'ri ketyapmanmi?", UZBEK},
                {"Men (kochani, uyni) qidiryapman", UZBEK},
                {"Meni (mehmonxonaga, aeroportga, restoranga) oborib qoing", UZBEK},
                {"Qaysi mehmonxonada tuxtaymiz?", UZBEK},
                {"Xonamni kalitini bering iltimos", UZBEK},
                {"Xonamni korsam boladimi?", UZBEK},
                {"Juda soz!", UZBEK},
                {"...Sotib olmoqchiman", UZBEK},
                {"Korsating menga", UZBEK},
                {"Sizda...bormi?", UZBEK},
                {"Qayerga tolashim kerak?", UZBEK},
                {"Men universitetda o'qiyman", UZBEK},
                {"Rustam, mana bu sening kitobingmi", UZBEK},
                {"Savollarga javob bering, so'roq olmoshlariga e'tibor bering", UZBEK},
                {"Biznikiga qachon mehmonga borasiz?", UZBEK},
                {"Birga dars tayyorlaymiz, sport bilan shug'ullanamiz", UZBEK},
                {"Biz hozir ma'ruza tinglayapmiz", UZBEK},
                {"Darsdan keyin rassom Akmal Nurning ko'rgazmasiga boramizmi", UZBEK},
                {"Boramiz. Akmal Nurdan boshqa rassomlarning ham ko'rgazmasi bor", UZBEK},
                {"Ha, juda yoqdi", UZBEK},
                {"Ie, ishtahamni ochib yubording, qani ketdik", UZBEK},
                {"Senga qanday palov ko'proq yoqadi?", UZBEK},
                {"Avgustning o'ttiz birinchisida ketyapsizmi?", UZBEK},
                {"Men konferensiyaga taklifnomani e-mail orqali oldim", UZBEK},
                {"Takliflarning shakllariga e'tibor bering", UZBEK},
                {"Sizning universitetingiz qanday nomlanadi?", UZBEK},
                {"Ismingiz, familiyangiz, otangizning ismi kim?", UZBEK},
                {"Bo'sh vaqtingizni qanday o'tkazasiz?", UZBEK},
                {"Darsdan so'ng kutubxonaga bora olasizmi?", UZBEK},
                {"Ushbu gap bo'yicha savollarga javob bering.", UZBEK},
                {"O'zbekistonda qanday tantana bo'lib o'tdi?", UZBEK},
                {"Janubiy vokzalga qanday borsa bo'ladi?", UZBEK},
                {"Iltimos, meni o'tkazib yuboring.", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {ENGLISH=0.1729388703522023, UZBEK=0.9998923845664354} : UZBEK
                {"Ishlaringiz Qalay?", UZBEK},
                {"Hammasi joyidami?", UZBEK},
                {"Juda yaxshi, rahmat", UZBEK},
                {"Bunday emas", UZBEK},
                {"Kechirim so'rash va undan keyin javob beriladigan so'zlar", UZBEK},
                {"Bu o'zbekchada nima deyiladi?", UZBEK},
                {"Savollar beradigan so'zlar", UZBEK},
                {"...Qayerda joylashgan?", UZBEK},
                {"Qaysi tarafga borishim kerak?", UZBEK},
                {"Mehmonxona qayerda joylashgan?", UZBEK},
                {"Menga (Bizga) bir kishilik (ikki kishilik) xona kerak", UZBEK},
                {"Xonam nechinchi qavatda joylashgan?", UZBEK},
                {"Mening xonamda issiq (sovuq)", UZBEK},
                {"Chiqish qayerda?", UZBEK},
                {"Xavta kunlari", UZBEK},
                {"Quyidagi gaplarni rus tiliga tarjima qiling", UZBEK},
                {"Quyidagi so'zlarni o'zbek tiliga tarjima qiling", UZBEK},
                {"Quyidagi gaplarni tarjima qiling", UZBEK},
                {"Darslaringiz qachon boshlanadi?", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {UNKNOWN=0.9153442216550883, ENGLISH=0.16821944857915055,
                // UZBEK=0.9991852467242681} : UZBEK
                {"...Necha pul? or Qancha turadi?", UZBEK},
                {"Samarqandning qanday obidalarini bilasiz?", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {UNKNOWN=0.9993323382366762, ENGLISH=0.05968963246783442,
                // TURKISH=0.8675559049950755} : TURKISH
                {"Sekin-sekin", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {ENGLISH=0.21380681333715862, TURKISH=0.8554354415007712} :
                // TURKISH
                {"Men Rossiyadanman", UZBEK},
                {"Xona menga maqul kel(ma)yapti", UZBEK},
                {"Tahsin, ma'qullash", UZBEK},
                {"Necha pul tolashim kerak?", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {UNKNOWN=0.8989750062031181, ENGLISH=0.03109135617555084,
                // TURKISH=0.8919503402278582, UZBEK=0.9991306459262397} : UZBEK
                {"Mayli, yaXshi", UZBEK},
                {"Qayerda ...ni sotib olsam boladi?", UZBEK},
                {"Mustaqillik bayramining necha yilligi bayram qilindi?", UZBEK},

                // [ENGLISH, GERMAN, TURKISH, UZBEK] : {ENGLISH=0.011696000151765251, TURKISH=0.8882977177350004,
                // UZBEK=0.9999919802564252} : UZBEK
                {"Odam tanasida qanday foydali kimyoviy moddalar bor?", UZBEK},
                {"Unda birga boraylik. Sizni universitet oldida kutaman.", UZBEK},

                // Unique letters
                {"Oʻzbek tili", UZBEK},

                // Русские кейсы, которые могут быть ошибочно определены как узбекские
                {"Купите тур в Узбекистан на 2020 год Оплата по прилёту! Купите туры в Узбекистан, Самарканд, Ташкент" +
                        ". Бронируйте сейчас, платите на месте!", RUSSIAN},
                {"Туры в Самарканд на 2020 год Расписание туров на 2020 Туры в Узбекистан, Самарканд, Бухара, Ташкент" +
                        ". Бронируйте сейчас, платите на месте!", RUSSIAN},
                {"Тур Хива Самарканд Бухара! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. Организация " +
                        "экскурсий. Получите подробности!", RUSSIAN},
                {"Тур Ташкент Самарканд Бухара Хива! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. " +
                        "Организация экскурсий. Получите подробности!", RUSSIAN},
                {"Тур в Узбекистан Самарканд Бухара! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. " +
                        "Организация экскурсий. Получите подробности!", RUSSIAN},
                {"Ташкент Бухара тур! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. Организация " +
                        "экскурсий. Получите подробности!", RUSSIAN},
                {"Ташкент Бухара Самарканд тур цена! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. " +
                        "Организация экскурсий. Получите подробности!", RUSSIAN},
                {"Самарканд Бухара Хива тур! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. Организация " +
                        "экскурсий. Получите подробности!", RUSSIAN},
                {"Тур Хива Бухара Самарканд! Тур в Узбекистан! Туры в Ташкент, Бухара, Самарканд, Хива. Организация " +
                        "экскурсий. Получите подробности!", RUSSIAN},
                {"Тур в Ташкент Самарканд Бухару Хиву На 2020 год Туры в Узбекистан, Самарканд, Бухара, Ташкент. " +
                        "Бронируйте сейчас, платите на месте!", RUSSIAN},
                {"Тур в Ташкент Самарканд Бухару Хиву Оплата на месте Экскурсионные туры в Узбекистан, Самарканд, " +
                        "Ташкент на 2020 год. Лучшая подборка!", RUSSIAN},
        });
    }

    @ParameterizedTest(name = "Property is on for the client; text: {0}")
    @MethodSource("parameters")
    void recognize_PropertyIsOnForTheClient_LanguageRecognized(String text, Language language) {
        Long clientIdLong = 12345L;

        doReturn(Set.of(clientIdLong)).when(clientsWithEnabledUzbekLanguageProperty).getOrDefault(emptySet());
        assertThat(queryrecService.recognize(text, ClientId.fromLong(clientIdLong), UZBEKISTAN_REGION_ID),
                is(language));
    }

    @ParameterizedTest(name = "Property is on for all clients; text: {0}")
    @MethodSource("parameters")
    void recognize_PropertyIsOnForAllClients_LanguageRecognized(String text, Language language) {
        doReturn(ENABLE_UZBEK_LANGUAGE_FOR_ALL_CLIENTS_PROPERTY_VALUE)
                .when(clientsWithEnabledUzbekLanguageProperty).getOrDefault(emptySet());
        assertThat(queryrecService.recognize(text, ClientId.fromLong(1234L), UZBEKISTAN_REGION_ID), is(language));
    }

    @ParameterizedTest(name = "Property is on for another client; text: {0}")
    @MethodSource("parameters")
    void recognize_PropertyIsOnForAnotherClient_UzbekLanguageNotRecognized(String text, Language language) {
        doReturn(Set.of(324432L)).when(clientsWithEnabledUzbekLanguageProperty).getOrDefault(emptySet());
        assertThat(queryrecService.recognize(text, ClientId.fromLong(4321L), UZBEKISTAN_REGION_ID), not(is(UZBEK)));
    }

    @ParameterizedTest(name = "Property is off; text: {0}")
    @MethodSource("parameters")
    void recognize_PropertyIsOff_UzbekLanguageNotRecognized(String text, Language language) {
        doReturn(emptySet()).when(clientsWithEnabledUzbekLanguageProperty).getOrDefault(emptySet());
        assertThat(queryrecService.recognize(text, ClientId.fromLong(213214L), UZBEKISTAN_REGION_ID), not(is(UZBEK)));
    }
}
