package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.commasAreBounded;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.illegalComma;

public class BannerTextConstraintsCommasAreBoundedTest extends BannerTextConstraintsBaseTest {

    public BannerTextConstraintsCommasAreBoundedTest() {
        super(commasAreBounded());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"пробел - запятая - пробел", "купить , продать", null},
                {"пробел - запятая - неразрывный пробел", "купить ,\u00a0продать", null},
                {"пробел - запятая - непробельный символ", "купить ,продать", illegalComma()},
                {"непробельный символ - запятая - неразрывный пробел", "купить,\u00a0продать", null},
                {"неразрывный пробел - запятая - пробел", "купить\u00a0, продать", null},
                {"неразрывный пробел - запятая - неразрывный пробел", "купить\u00a0,\u00a0продать", null},
                {"неразрывный пробел - запятая - непробельный символ", "купить\u00a0,продать", illegalComma()},
                {"запятая посередение слова", "посе,рединке", null},
                {"запятые после слов", "яблоки, апельсины, мандарины", null},
                {"запятая перед словом в начале строки", ",купить", null},
                {"пробел-запятая перед словом в начале строки", " ,купить", illegalComma()},
                {"запятая после слова в конце строки", "купить,", null},
                {"пробел-запятая после слова в конце строки", "купить ,", null},
                {"несколько запятых подряд", ",,", null},
        });
    }
}
