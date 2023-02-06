package ru.yandex.autotests.innerpochta.utils.matchers;

import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.autotests.innerpochta.utils.beans.SignBean;
import ru.yandex.autotests.innerpochta.utils.oper.GetProfile;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * User: lanwen
 * Date: 06.12.13
 * Time: 20:37
 */
public class SignMatchers {

    /**
     * Делает запрос на получение самостоятельно. Удобно использовать вместе со ждущим матчером.
     *
     * @param matcher должен быть матчером на Iterable,
     *                т.к. возможно несколько равнозначных подписей, к которым разом и применится переданный матчер
     * @return матчер для списка подписей в выдаче.
     */
    public static FeatureMatcher<GetProfile, List<SignBean>> hasSigns(final Matcher matcher) {
        return new FeatureMatcher<GetProfile, List<SignBean>>(matcher, "signs that", "signs was") {
            @Override
            protected List<SignBean> featureValueOf(GetProfile oper) {
                    return oper.get().via(new DefaultHttpClient()).statusCodeShouldBe(HttpStatus.OK_200).signs();
            }
        };
    }

    public static FeatureMatcher<SignBean, String> signWithText(final Matcher<String> matcher) {
        return new FeatureMatcher<SignBean, String>(matcher, "sign with text", "sign with text") {
            @Override
            protected String featureValueOf(SignBean signBean) {
                return signBean.text();
            }
        };
    }


    public static Matcher<SignBean> signWithTextTraitsCode(final Matcher<String> matcher) {
        return signWithTextTraits("code", matcher);
    }

    public static Matcher<SignBean> signWithTextTraitsLang(final Matcher<String> matcher) {
        return signWithTextTraits("lang", matcher);
    }


    public static FeatureMatcher<SignBean, String> signWithTextTraits(final String key, final Matcher<String> matcher) {
        return new FeatureMatcher<SignBean, String>(
                matcher,
                "sign with text_traits:" + key,
                "sign with text_traits:" + key) {
            @Override
            protected String featureValueOf(SignBean signBean) {
                return signBean.textTraits().get(key);
            }
        };
    }

    public static FeatureMatcher<SignBean, Boolean> signIsDefault(boolean value) {
        return new FeatureMatcher<SignBean, Boolean>(is(value), "sign is default", "sign") {
            @Override
            protected Boolean featureValueOf(SignBean signBean) {
                return signBean.isDefault();
            }
        };
    }

    /**
     * @param matcher должен быть матчером на Iterable,
     *                т.к. структура предполагает наличие нескольких ассоциированных мыл
     * @return матчер на емейлы у конкретного бина
     */
    public static FeatureMatcher<SignBean, List<String>> signWithEmails(final Matcher matcher) {
        return new FeatureMatcher<SignBean, List<String>>(matcher, "sign with emails", "sign with emails") {
            @Override
            protected List<String> featureValueOf(SignBean signBean) {
                return signBean.associatedEmailsInBean();
            }
        };
    }

    public static FeatureMatcher<SignBean, Boolean> signIsSanitize(boolean value) {
        return new FeatureMatcher<SignBean, Boolean>(is(value), "sign is sanitize", "sign") {
            @Override
            protected Boolean featureValueOf(SignBean signBean) {
                return signBean.isSanitize();
            }
        };
    }
}
