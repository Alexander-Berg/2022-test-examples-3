package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

@RunWith(Parameterized.class)
@Description("Проверка валидатора отсутствия некорректной escape-последовательности в uri")
public class ConstraintsInvalidEscapeSeq {

    @Parameterized.Parameter(0)
    public String description;
    @Parameterized.Parameter(1)
    public String uri;
    @Parameterized.Parameter(2)
    public Boolean isUriValid;

    @Parameterized.Parameters(name = "uri=\"{1}\" description=\"{0}\"")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"uri без escape-последовательности", "ttt&a=b", true},
                {"корректная escape-последовательность в имени параметра", "%aa=b", true},
                {"корректная escape-последовательность в значении параметра", "a=%bb", true},
                {"корректная escape-последовательность, содержащая все возможные символы", "%00%bb%1a%b2%3c%4d%e5%6f%77%88%99", true},
                {"обрыв escape-последовательности на один символ", "ttt%b", false},
                {"пустая escape-последовательность на  символ", "ttt%", false},
                {"uri, состоящий только из %", "%", false},
                {"некорректный первый символ в последовательности", "ttt%gf", false},
                {"некорректный второй символ в последовательности", "ttt%fh", false},
                {"микс корректных и некорректных последовательностей", "ttt%aa%nn%bbooo", false}
        });
    }

    @Test
    public void validTelegram() {
        assertThat(BannerUriConstraints.hasNotInvalidEscapeSeq().apply(uri))
                .isEqualTo(isUriValid ? null : invalidValue());
    }
}
