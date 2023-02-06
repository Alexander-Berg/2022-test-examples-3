package ru.yandex.autotests.httpclient.metabeanprocessor.beanmapper;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.*;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeFirstTestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeSecondTestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeThirdTestBean;
import ru.yandex.autotests.httpclient.metabeanprocessor.beans.apilikebean.ApiLikeZeroTestBean;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper.map;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
* Created by shmykov on 09.02.15.
*/
public class BeanMapperTest {

    private ZeroTestBean expectedTestBean;
    private ApiLikeZeroTestBean srcBean;


    @Before
    public void initBeans() {
        expectedTestBean = new ZeroTestBean();
        TestBean testBean = new TestBeanBuilder().createTestBean();
        testBean.setCanUseDayBudget("0");
        SecondTestBean secondTestBean = new SecondTestBeanBuilder().createSecondTestBean();
        List<String> fios = new ArrayList<>();
        fios.add("Иванов Иван");
        fios.add("Вася Пупкин");
        secondTestBean.setFio(fios);
        secondTestBean.setCid(30824416);
        secondTestBean.setRunningUnmoderated("0");
        testBean.setSecondTestBean(secondTestBean);
        FourthTestBean fourthTestBean = new FourthTestBeanBuilder().setEmail("at-direct-backend-c@yandex.ru").
                createFourthTestBean();
        ThirdTestBean thirdTestBean = new ThirdTestBeanBuilder().createThirdTestBean();
        List<ThirdTestBean> thirdTestBeans = new ArrayList<>();
        thirdTestBean.setEmail(fourthTestBean);
        thirdTestBeans.add(thirdTestBean);
        thirdTestBean = new ThirdTestBeanBuilder().createThirdTestBean();
        fourthTestBean = new FourthTestBeanBuilder().setEmail("at-tester@yandex-team.ru").
                createFourthTestBean();
        thirdTestBean.setEmail(fourthTestBean);
        thirdTestBeans.add(thirdTestBean);
        testBean.setThirdTestBeans(thirdTestBeans.toArray(new ThirdTestBean[2]));
        expectedTestBean.setArray(new TestBean[]{testBean});

        srcBean = new ApiLikeZeroTestBean();
        ApiLikeFirstTestBean apiLikeFirstTestBean = new ApiLikeFirstTestBean();
        List<ApiLikeFirstTestBean> list = new ArrayList<>();
        apiLikeFirstTestBean.setCanUseDayBudget(false);
        ApiLikeSecondTestBean apiLikeSecondTestBean = new ApiLikeSecondTestBean();
        List<String> credentials = new ArrayList<>();
        credentials.add("Иванов Иван");
        credentials.add("Вася Пупкин");
        apiLikeSecondTestBean.setCredentials(credentials.toArray(new String[2]));
        apiLikeSecondTestBean.setCampaignId("30824416");
        apiLikeSecondTestBean.setRunningUnmoderated("0");
        apiLikeFirstTestBean.setNestedTestBean(apiLikeSecondTestBean);
        ApiLikeThirdTestBean apiLikeThirdTestBean = new ApiLikeThirdTestBean();
        List<ApiLikeThirdTestBean> thirdBeanslist = new ArrayList<>();
        apiLikeThirdTestBean.setEmail("at-direct-backend-c@yandex.ru");
        apiLikeThirdTestBean.setNotUsedField("won't be mapped!");
        thirdBeanslist.add(apiLikeThirdTestBean);
        apiLikeThirdTestBean = new ApiLikeThirdTestBean();
        apiLikeThirdTestBean.setEmail("at-tester@yandex-team.ru");
        thirdBeanslist.add(apiLikeThirdTestBean);
        apiLikeFirstTestBean.setThirdTestBeans(thirdBeanslist.toArray(new ApiLikeThirdTestBean[2]));
        list.add(apiLikeFirstTestBean);
        srcBean.setList(list);
    }

    @Test
    public void convertWithMappingBuilderTest() {
        ZeroTestBean dstBean = map(srcBean, ZeroTestBean.class, new ZeroTestBeanMappingBuilder());
        assertThat("Бины не одинаковые", dstBean, beanEquivalent(expectedTestBean));
    }

    @Test
    public void convertWithDefaultMappingBuilderTest() {
        ThirdTestBean dstBean = map(srcBean.getList().get(0).getThirdTestBeans()[0], ThirdTestBean.class);
        assertThat("Бины не одинаковые", dstBean, beanEquivalent(expectedTestBean.getArray()[0].getThirdTestBeans()[0]));
    }
}
