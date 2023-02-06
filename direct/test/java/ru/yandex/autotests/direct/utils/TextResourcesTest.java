package ru.yandex.autotests.direct.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.direct.utils.beans.BeanWrapper;
import ru.yandex.autotests.direct.utils.beans.TextResourceBean;
import ru.yandex.autotests.direct.utils.matchers.TextResourceMatcherTest;
import ru.yandex.autotests.direct.utils.textresource.TextResourcesMongoHelper;
import ru.yandex.autotests.irt.restheart.beans.RestheartGetResponseBean;
import ru.yandex.autotests.irt.restheart.client.RestheartHttpClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.direct.utils.textresource.TextResources.getBean;
import static ru.yandex.autotests.direct.utils.textresource.TextResources.getText;
import static ru.yandex.autotests.direct.utils.textresource.TextResources.saveTextResource;
import static ru.yandex.autotests.direct.utils.textresource.TextResources.searchBeans;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class TextResourcesTest {

    TextResourceBean bean = new TextResourceBean();
    Gson gson = new Gson();

    @Before
    public void setUp() {
        bean.setRu("русский");
        bean.setEn("english");
        bean.setKey(String.valueOf(DateTime.now().getMillis()));
        saveTextResource(bean, bean.getKey());
    }

    @Test
    public void getTextResource() {
        TextResourceBean actualBean = getBean(bean.getKey());
        assertThat("Получили ожидаемый ресурс", actualBean, beanEquals(bean));
    }

    @After
    public void tearDown() {
        new TextResourcesMongoHelper().deleteTextResource(bean.getKey());
    }

    @Test
    public void seachBeansByKey() {
        TextResourceBean searchBean = new TextResourceBean();
        searchBean.setKey(bean.getKey());
        List<TextResourceBean> found = searchBeans(searchBean);
        assertThat("Нашлись бины по ключу", found, hasSize(greaterThan(0)));
    }

    @Test
    public void seachBeansByRu() {
        TextResourceBean searchBean = new TextResourceBean();
        searchBean.setRu(bean.getRu());
        List<TextResourceBean> found = searchBeans(searchBean);
        assertThat("Нашлись бины по ru", found, hasSize(greaterThan(0)));
    }

    @Test
    public void seachBeansByEn() {
        TextResourceBean searchBean = new TextResourceBean();
        searchBean.setEn(bean.getEn());
        List<TextResourceBean> found = searchBeans(searchBean);
        assertThat("Нашлись бины по en", found, hasSize(greaterThan(0)));
    }

    @Test
    public void seachBeansByAllFields() {
        List<TextResourceBean> found = searchBeans(bean);
        assertThat("Нашлись бины по всем полям", found, hasSize(greaterThan(0)));
    }

    @Test
    public void canGetRuText() {
        assertThat("Получаем русский текст для ITextResource",
                getText(TextResourceMatcherTest.TestTextResource.TEST_STRING1, "ru"), equalTo("тест"));
    }

    @Test
    public void canGetEnText() {
        assertThat("Получаем английский текст для ITextResource",
                getText(TextResourceMatcherTest.TestTextResource.TEST_STRING1, "en"), equalTo("test"));
    }

    /**
     * Метод для переезда из коллекции, завернутой в BeanWrapper в коллекцию TextResourceBean с ключом
     */
    public void moveCollection() throws UnsupportedEncodingException {
        RestheartHttpClient client = new RestheartHttpClient();

        RestheartGetResponseBean<String> responseBean =
                client.useDB("BeanTemplates").useCollection("resources").get("{}");

        Type type = new TypeToken<BeanWrapper<TextResourceBean>>() {
        }.getType();

        List<BeanWrapper<TextResourceBean>> texts =
                responseBean.getResultDocuments()
                        .stream()
                        .map(t -> (BeanWrapper<TextResourceBean>) gson.fromJson(t, type))
                        .collect(Collectors.toList());
        for (BeanWrapper text : texts) {
            ((TextResourceBean) text.getBean()).setKey(text.getTemplateName());
        }
        List<TextResourceBean> beans = new ArrayList<>();
        texts.forEach(t -> beans.add(t.getBean()));
        for (TextResourceBean bean : beans) {
            client.useDB("BeanTemplates").useCollection("TextResources").add(bean);
        }
    }
}
