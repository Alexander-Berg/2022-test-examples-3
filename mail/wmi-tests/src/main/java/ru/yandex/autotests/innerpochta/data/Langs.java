package ru.yandex.autotests.innerpochta.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.09.14
 * Time: 17:02
 * <p/>
 * Определение персидского, словацкого, португальского, венгерского в текущем рекогнайзере не поддерживается.
 * https://jira.yandex-team.ru/browse/DARIA-32952
 * https://jira.yandex-team.ru/browse/DARIA-38188
 * <p/>
 * Поддерживаемые языки:
 * азербайджанский, (баг)
 * английский,
 * белорусский,
 * болгарский,      (баг)
 * испанский,
 * итальянский,
 * казахский,  (пока не тестируем)
 * латышский,
 * литовский,
 * немецкий,
 * польский,
 * румынский,  (пока не тестируем)
 * русский,
 * татарский,  (пока не тестируем)
 * турецкий,
 * украинский,
 * французский,
 * чешский,         (баг)
 * эстонский,
 * греческий,
 * армянский,
 * грузинский,
 * иврит.
 */
public enum Langs {
    LANG_UNK("mis"), // Unknown
    LANG_RUS("ru"), // Russian
    LANG_ENG("en"), // English
    LANG_POL("pl"), // Polish
    LANG_HUN("hu"), // Hungarian
    LANG_UKR("uk"), // Ukrainian
    LANG_GER("de"), // German
    LANG_FRN("fr"), // French
    LANG_TAT("tt"), // Tatar
    LANG_BLR("be"), // Belorussian
    LANG_KAZ("kk"), // Kazakh
    LANG_ALB("sq"), // Albanian
    LANG_SPA("es"), // Spanish
    LANG_ITA("it"), // Italian
    LANG_ARM("hy"), // Armenian
    LANG_DAN("da"), // Danish
    LANG_POR("pt"), // Portuguese
    LANG_ICE("is"), // Icelandic
    LANG_SLO("sk"), // Slovak
    LANG_SLV("sl"), // Slovene
    LANG_DUT("nl"), // Dutch (Netherlandish language)
    LANG_BUL("bg"), // Bulgarian
    LANG_CAT("ca"), // Catalan
    LANG_HRV("hr"), // Croatian
    LANG_CZE("cs"), // Czech
    LANG_GRE("el"), // Greek
    LANG_HEB("he"), // Hebrew
    LANG_NOR("no"), // Norwegian
    LANG_MAC("mk"), // Macedonian
    LANG_SWE("sv"), // Swedish
    LANG_KOR("ko"), // Korean
    LANG_LAT("la"), // Latin
    LANG_BASIC_RUS("bas-ru"), // Simplified version of Russian (used at lemmer only)
    LANG_UNSET_33("bs"), // Bosnian
    LANG_UNSET_34("mt"), // Maltese

    LANG_EMPTY(""), // Indicates that document is empty
    LANG_UNK_LAT(""), // Any unrecognized latin language
    LANG_UNK_CYR(""), // Any unrecognized cyrillic language
    LANG_UNK_ALPHA(""), // Any unrecognized alphabetic language not fit into previous categories

    LANG_FIN("fi"), // Finnish
    LANG_EST("et"), // Estonian
    LANG_LAV("lv"), // Latvian
    LANG_LIT("lt"), // Lithuanian
    LANG_BAK("ba"), // Bashkir
    LANG_TUR("tr"), // Turkish
    LANG_RUM("ro"), // Romanian (also Moldavian)
    LANG_MON("mn"), // Mongolian
    LANG_UZB("uz"), // Uzbek
    LANG_KIR("ky"), // Kirghiz
    LANG_TGK("tg"), // Tajik
    LANG_TUK("tk"), // Turkmen
    LANG_SRP("sr"), // Serbian
    LANG_AZE("az"), // Azerbaijani
    LANG_BASIC_ENG("bas-en"), // Simplified version of English (used at lemmer only)
    LANG_GEO("ka"), // Georgian
    LANG_ARA("ar"), // Arabic
    LANG_PER("fa"); // Persian

    private String code;

    private Langs(String code) {
        this.code = code;
    }

    public String code() {
        return this.code;
    }

    //mid и соответсвующий ему язык
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> langs = new ArrayList<>();
        //DARIA-35737
        langs.add(new Object[]{"2550000004753710792", Langs.LANG_ITA});
        //DARIA-31855
        langs.add(new Object[]{"2550000004753710790", Langs.LANG_TUR});
        //бага, не определяем турецкий язык:
//        langs.add(new Object[]{"2550000004753710791", Langs.LANG_TUR});

//        проверяем поддерживаемые языки из LangRecongnizeTest:
        langs.add(new Object[]{"2550000004753710581", Langs.LANG_UKR});
        langs.add(new Object[]{"2550000004753710547", Langs.LANG_HEB});
        langs.add(new Object[]{"2550000004753710588", Langs.LANG_ENG});
        langs.add(new Object[]{"2550000004753710572", Langs.LANG_TUR});

        langs.add(new Object[]{"2550000004753710589", Langs.LANG_RUS});

        langs.add(new Object[]{"2550000004753710545", Langs.LANG_GRE});
        langs.add(new Object[]{"2550000004753710573", Langs.LANG_ARM});
        langs.add(new Object[]{"2550000004753710593", Langs.LANG_HEB});
        langs.add(new Object[]{"2550000004753710620", Langs.LANG_FRN});
        langs.add(new Object[]{"2550000004753710598", Langs.LANG_TUR});

        langs.add(new Object[]{"2550000004753710586", Langs.LANG_ENG});

        langs.add(new Object[]{"2550000004753710612", Langs.LANG_BLR});
        langs.add(new Object[]{"2550000004753710595", Langs.LANG_GRE});

        langs.add(new Object[]{"2550000004753710537", Langs.LANG_EST});
        langs.add(new Object[]{"2550000004753710610", Langs.LANG_SPA});

        langs.add(new Object[]{"2550000004753710536", Langs.LANG_LAV});
        langs.add(new Object[]{"2550000004753710563", Langs.LANG_ITA});

        langs.add(new Object[]{"2550000004753710578", Langs.LANG_FRN});

        langs.add(new Object[]{"2550000004753710569", Langs.LANG_BLR});
        langs.add(new Object[]{"2550000004753710624", Langs.LANG_ENG});
        langs.add(new Object[]{"2550000004753710640", Langs.LANG_TUR});
        langs.add(new Object[]{"2550000004753710544", Langs.LANG_GRE});
        langs.add(new Object[]{"2550000004753710582", Langs.LANG_UKR});
        langs.add(new Object[]{"2550000004753710577", Langs.LANG_FRN});

        langs.add(new Object[]{"2550000004753710616", Langs.LANG_GEO});
        langs.add(new Object[]{"2550000004753710621", Langs.LANG_GEO});

        langs.add(new Object[]{"2550000004753710617", Langs.LANG_UKR});
        langs.add(new Object[]{"2550000004753710580", Langs.LANG_GER});
        langs.add(new Object[]{"2550000004753710535", Langs.LANG_LIT});

        langs.add(new Object[]{"2550000004753710579", Langs.LANG_GER});
        langs.add(new Object[]{"2550000004753710587", Langs.LANG_ENG});

        langs.add(new Object[]{"2550000004753710614", Langs.LANG_ITA});
        langs.add(new Object[]{"2550000004753710611", Langs.LANG_ARM});
        langs.add(new Object[]{"2550000004753710597", Langs.LANG_TUR});
        langs.add(new Object[]{"2550000004753710591", Langs.LANG_RUS});
        langs.add(new Object[]{"2550000004753710641", Langs.LANG_TUR});

        langs.add(new Object[]{"2550000004753710583", Langs.LANG_POL});
        langs.add(new Object[]{"2550000004753710564", Langs.LANG_ITA});
        langs.add(new Object[]{"2550000004753710585", Langs.LANG_POL});

//        Определяем чешский как словацкий: MAILDEV-413
//        langs.add(new Object[]{"2550000004753710549", Langs.LANG_CZE});
//        langs.add(new Object[]{"2550000004753710596", Langs.LANG_CZE});
//        langs.add(new Object[]{"2550000004753710548", Langs.LANG_CZE});

        langs.add(new Object[]{"2550000004753710590", Langs.LANG_RUS});
        langs.add(new Object[]{"2550000004753710619", Langs.LANG_GER});

        langs.add(new Object[]{"2550000004753710575", Langs.LANG_SPA});
        langs.add(new Object[]{"2550000004753710546", Langs.LANG_HEB});
        langs.add(new Object[]{"2550000004753710574", Langs.LANG_ARM});
        langs.add(new Object[]{"2550000004753710622", Langs.LANG_POL});
        langs.add(new Object[]{"2550000004753710570", Langs.LANG_BLR});

        langs.add(new Object[]{"2550000004753710576", Langs.LANG_SPA});
        langs.add(new Object[]{"2550000004753710584", Langs.LANG_POL});
        langs.add(new Object[]{"2550000004753710627", Langs.LANG_RUS});

        //в коротких документах, язык должен определяться как unknown: DARIA-32691
        langs.add(new Object[]{"2550000004753710789", Langs.LANG_UNK});

        return langs;
    }

    public static List<Object[]> languages() {
        List<Object[]> dataLangs = new ArrayList<Object[]>();
        dataLangs.add(new Object[]{"az"});
        dataLangs.add(new Object[]{"ka"});
        dataLangs.add(new Object[]{"ru"});
        dataLangs.add(new Object[]{"en"});
        dataLangs.add(new Object[]{"be"});
        dataLangs.add(new Object[]{"kk"});
        dataLangs.add(new Object[]{"tt"});
        dataLangs.add(new Object[]{"tr"});
        dataLangs.add(new Object[]{"hy"});
        dataLangs.add(new Object[]{"uk"});
        dataLangs.add(new Object[]{"ro"});
        return dataLangs;
    }

}
