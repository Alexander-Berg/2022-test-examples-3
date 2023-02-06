package ru.yandex.autotests.innerpochta.wmi.uatraits;

import ch.lambdaj.function.convert.Converter;
import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Collection;

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.asCharSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.uatraits.MobileGetInfoB2B.LOGIN_GROUP;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.15
 * Time: 17:36
 */
@Title("[Uatraits][B2B] Проверка uatraits. Ручка mobile_get_info")
@Description("Ходим на почту с разными юзер агентами, проверяем выдачу ручки mobile_get_info")
@Aqua.Test
@Features(MyFeatures.WMI)
@Stories({MyStories.UATRAITS, MyStories.B2B})
@RunWith(Parameterized.class)
@Issue("DARIA-45808")
@Credentials(loginGroup = LOGIN_GROUP)
public class MobileGetInfoB2B extends BaseTest {

    public static final String LOGIN_GROUP = "Touchb2b";

    @Parameterized.Parameter(0)
    public String userAgent;
    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "user_agent = {0}")
    public static Collection<Object[]> data() throws Exception {
        ImmutableList<String> lines = asCharSource(getResource("ua/user-agents.txt"), UTF_8).readLines();
        return with(lines)
                .remove(startsWith("#"))
                .convert(new Converter<String, Object[]>() {
                    @Override
                    public Object[] convert(String from) {
                        return from.split(":::", 2);
                    }
                });
    }

    @Test
    @Title("Сравнение с продакшеном mobile_get_info")
    @Issue("DARIA-45808")
    public void b2bMobileGetInfo() throws IOException {
        Oper respNew = jsx(MobileGetInfo.class).userAgent(userAgent).post().via(hc);

        Oper respBase = jsx(ru.yandex.autotests.innerpochta.wmi.core.oper.MobileGetInfo.class)
                .setHost(props().b2bUri().toString()).userAgent(userAgent).post().via(hc);

        MatcherAssert.assertThat(respNew.toDocument(), equalToDoc(respBase.toDocument()));

    }
}
