package ru.yandex.autotests.direct.web.util.matchers;

import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.autotests.direct.utils.matchers.BeanCompareStrategy;
import ru.yandex.autotests.direct.web.objects.banners.BannerInfoWeb;
import ru.yandex.autotests.direct.web.objects.banners.BannerPhraseInfoWeb;
import ru.yandex.autotests.direct.web.objects.banners.commons.ContactInfoWeb;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.direct.web.util.matchers.NumberApproximatelyEqual.approxEqualTo;
import static ru.yandex.autotests.direct.web.util.matchers.beans.BeanCollectionAssert.withBeans;
import static ru.yandex.autotests.direct.web.util.matchers.beans.IsBeanCollectionContaining.hasBean;
import static ru.yandex.autotests.direct.web.util.matchers.beans.IsBeanCollectionContaining.hasBeans;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class BeanMatchersTest {
    //@Test
    public void beanListAssertion() {
        List<BannerInfoWeb> bannersList1 = new ArrayList<>();
        List<BannerInfoWeb> bannersList2 = new ArrayList<>();

        BannerInfoWeb bannerInfo1 = new BannerInfoWeb();
        bannerInfo1.setBannerId(1L);
        BannerInfoWeb bannerInfo2 = new BannerInfoWeb();
        bannerInfo2.setBannerId(2L);
        BannerInfoWeb bannerInfo3 = new BannerInfoWeb();
        bannerInfo3.setBannerId(3L);

        bannersList1.add(bannerInfo1);
        bannersList1.add(bannerInfo2);
        bannersList1.add(bannerInfo3);

        BannerInfoWeb bannerInfo4 = new BannerInfoWeb();
        bannerInfo4.setBannerId(4L);

        BannerInfoWeb bannerInfo5WithId2 = new BannerInfoWeb();
        bannerInfo5WithId2.setBannerId(2L);

        bannersList2.add(bannerInfo2);
        bannersList2.add(bannerInfo3);

        withBeans(bannersList1).assertThat(everyItem(Matchers.<BannerInfoWeb>hasProperty("bannerID")))
                .and(hasItem(Matchers.<BannerInfoWeb>hasProperty("bannerID", equalTo(2))))
                .and(hasBean(bannerInfo2))
                .and(hasBean(bannerInfo5WithId2))
                .and(hasItems(beanEquals(bannerInfo1), beanEquals(bannerInfo2)))
                .and(hasBeans(bannersList2))
                .and(not(hasBean(bannerInfo4)));
    }

    @Test
    public void beanArraysAreNotAsserted() {
        BannerInfoWeb bannerInfo1 = new BannerInfoWeb();
        bannerInfo1.setBannerId(1L);
        BannerInfoWeb bannerInfo2 = new BannerInfoWeb();
        bannerInfo2.setBannerId(1L);
        BannerPhraseInfoWeb phrase1 = new BannerPhraseInfoWeb();
        phrase1.setPhraseID(1L);
        BannerPhraseInfoWeb phrase2 = new BannerPhraseInfoWeb();
        phrase2.setPhraseID(1L);
        BannerPhraseInfoWeb phrase3 = new BannerPhraseInfoWeb();
        phrase1.setPhraseID(2L);
        BannerPhraseInfoWeb phrase4 = new BannerPhraseInfoWeb();
        phrase2.setPhraseID(2L);
        bannerInfo1.setPhrases(new BannerPhraseInfoWeb[]{phrase1, phrase3});
        bannerInfo2.setPhrases(new BannerPhraseInfoWeb[]{phrase4, phrase2});

        assertThat(bannerInfo1, beanEquals(bannerInfo2));
    }

    @Test(expected = AssertionError.class)
    public void beansAssert() {
        BannerInfoWeb bannerInfo1 = new BannerInfoWeb();
        bannerInfo1.setBannerId(1L);
        bannerInfo1.setDomain("ya.ru");
        ContactInfoWeb contactInfo1 = new ContactInfoWeb();
        contactInfo1.setCity("spb");
        bannerInfo1.setContactInfo(contactInfo1);

        BannerInfoWeb bannerInfo2 = new BannerInfoWeb();
        bannerInfo2.setBannerId(1L);
        bannerInfo2.setDomain("ya.ru");
        ContactInfoWeb contactInfo2 = new ContactInfoWeb();
        contactInfo2.setCity("msk");
        bannerInfo2.setContactInfo(contactInfo2);

        assertThat(contactInfo1, beanEquals(contactInfo2));
    }

    @Test(expected = AssertionError.class)
    public void beansAssertWithStrategy() {
        SomeBean expected = new SomeBean();
        expected.setStringValue("stringVal");
        expected.setIntValue(14);
        expected.setIntegerValue(123);
        expected.setDoubleValue(1234.5);
        expected.setEnumField(SomeBean.Enum.FIRST);
        expected.setSomeClassField(new SomeBean.SomeClass("yooo"));
        expected.setArray(new String[]{"1111", "22222"});

        SomeBean actual = new SomeBean();
        actual.setStringValue("stringVal");
        actual.setIntValue(7);
        actual.setDoubleValue(1030.5);
        actual.setEnumField(SomeBean.Enum.SECOND);
        actual.setSomeClassField(new SomeBean.SomeClass("yeeee"));
        actual.setArray(new String[]{"3333", "444"});

        BeanCompareStrategy beanCompareStrategy = new BeanCompareStrategy();
        beanCompareStrategy.putFieldMatcher("intValue", approxEqualTo(expected.getIntValue()).withDifference(7));
        beanCompareStrategy.putFieldMatcher("doubleValue",
                approxEqualTo(expected.getDoubleValue()).withDifferenceInPercents(15));
        assertThat(actual, beanEquals(expected).accordingStrategy(beanCompareStrategy));
    }
}
