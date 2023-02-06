package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.banner;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexImageHashBanner;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

/**
 * Тесты линковки создаваемых/обновляемых баннеров.
 * Проверяется только факт добавления или обновления без подробностей.
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateBannerDataTest extends ComplexUpdateBannerTestBase {

    // добавление

    @Test
    public void adGroupWithAddedBanner() {
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, contains(complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }

    @Test
    public void adGroupWithAddedBannerAndSkippedExistingBanner() {
        TextBannerInfo skippedExistingBannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                skippedExistingBannerInfo.getBanner().getTitle(),
                complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(2));
    }

    @Test
    public void adGroupWithTwoAddedBanners() {
        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                complexBanner1.getBanner().getTitle(),
                complexBanner2.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(2));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithAddedBanner() {
        createSecondAdGroup();

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        List<String> titles = findBannerTitles(adGroupInfo2);
        assertThat(titles, contains(complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }

    // обновление

    @Test
    public void adGroupWithUpdatedBanner() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, contains(complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }

    @Test
    public void adGroupWithUpdatedBannerAndSkippedExistingBanner() {
        TextBannerInfo skippedExistingBannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                skippedExistingBannerInfo.getBanner().getTitle(),
                complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(2));
    }

    @Test
    public void adGroupWithTwoUpdatedBanners() {
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner(bannerInfo2);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                complexBanner1.getBanner().getTitle(),
                complexBanner2.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(2));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithUpdatedBanner() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        List<String> titles = findBannerTitles(adGroupInfo2);
        assertThat(titles, contains(complexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(1));
    }

    // добавление + обновление

    @Test
    public void adGroupWithOneAddedAndOneUpdatedBanners() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner addedComplexBanner = randomTitleTextComplexBanner();
        ComplexTextBanner updatedComplexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(addedComplexBanner, updatedComplexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                addedComplexBanner.getBanner().getTitle(),
                updatedComplexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(2));
    }

    @Test
    public void adGroupWithOneAddedAndOneUpdatedAndOneSkippedBanners() {
        TextBannerInfo skippedExistingBannerInfo = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner addedComplexBanner = randomTitleTextComplexBanner();
        ComplexTextBanner updatedComplexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(addedComplexBanner, updatedComplexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> titles = findBannerTitles(adGroupInfo1);
        assertThat(titles, containsInAnyOrder(
                skippedExistingBannerInfo.getBanner().getTitle(),
                addedComplexBanner.getBanner().getTitle(),
                updatedComplexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(3));
    }

    @Test
    public void adGroupWithAddedBannerAndEmptyAdGroupAndAdGroupWithOneAddedAndOneUpdatedBanners() {
        createSecondAdGroup();
        createThirdAdGroup();

        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo3);

        ComplexTextBanner addedComplexBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner addedComplexBanner2 = randomTitleTextComplexBanner();
        ComplexTextBanner updatedComplexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(addedComplexBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2);
        ComplexTextAdGroup adGroupForUpdate3 = createValidAdGroupForUpdate(adGroupInfo3)
                .withComplexBanners(asList(addedComplexBanner2, updatedComplexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2, adGroupForUpdate3);

        List<String> titles1 = findBannerTitles(adGroupInfo1);
        assertThat(titles1, contains(addedComplexBanner1.getBanner().getTitle()));

        List<String> titles3 = findBannerTitles(adGroupInfo3);
        assertThat(titles3, containsInAnyOrder(
                addedComplexBanner2.getBanner().getTitle(),
                updatedComplexBanner.getBanner().getTitle()));

        assertThat(getClientBannersCount(), is(3));
    }

    // разные типы баннеров - добавление

    @Test
    public void adGroupWithAddedImageHashBanner() {
        ComplexImageHashBanner complexBanner = randomHrefComplexImageHashBanner(createImage());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> hrefs = findBannerHrefs(adGroupInfo1);
        assertThat(hrefs, contains(complexBanner.getBanner().getHref()));

        assertThat(getClientBannersCount(), is(1));
    }

    /**
     * Проверяет, что выставление id сайтлинков и визиток не ломается при наличии баннеров,
     * которые не поддерживают их
     */
    @Test
    public void adGroupWithAddedImageHashBannerAndTextBannerWithVcardAndSitelinks() {
        ComplexTextBanner complexTextBanner = randomTitleTextComplexBanner()
                .withSitelinkSet(randomHrefSitelinkSet())
                .withVcard(fullVcard());
        complexTextBanner.getBanner().withHref("http://yandex.ru/" + RandomStringUtils.randomAlphanumeric(10));
        ComplexImageHashBanner complexImHashBanner = randomHrefComplexImageHashBanner(createImage());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexTextBanner, complexImHashBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> hrefs = findBannerHrefs(adGroupInfo1);
        assertThat(hrefs, containsInAnyOrder(
                complexTextBanner.getBanner().getHref(),
                complexImHashBanner.getBanner().getHref()));

        assertThat(getClientBannersCount(), is(2));
    }

    // разные типы баннеров - обновление

    @Test
    public void adGroupWithUpdatedImageHashBanner() {
        ImageHashBannerInfo bannerInfo = createImageHashBanner();
        ComplexImageHashBanner complexBanner = randomHrefComplexImageHashBanner(bannerInfo, createImage());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> hrefs = findBannerHrefs(adGroupInfo1);
        assertThat(hrefs, contains(complexBanner.getBanner().getHref()));

        assertThat(getClientBannersCount(), is(1));
    }

    /**
     * Проверяет, что выставление id сайтлинков и визиток не ломается при наличии баннеров,
     * которые не поддерживают их
     */
    @Test
    public void adGroupWithUpdatedImageHashBannerAndTextBannerWithVcardAndSitelinks() {
        TextBannerInfo textBannerInfo = createRandomTitleTextBanner(adGroupInfo1);
        ComplexTextBanner complexTextBanner = randomTitleTextComplexBanner(textBannerInfo)
                .withSitelinkSet(randomHrefSitelinkSet())
                .withVcard(fullVcard());
        complexTextBanner.getBanner().withHref("http://yandex.ru/" + RandomStringUtils.randomAlphanumeric(10));

        ImageHashBannerInfo imHashBannerInfo = createImageHashBanner();
        ComplexImageHashBanner complexImHashBanner = randomHrefComplexImageHashBanner(imHashBannerInfo, createImage());

        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexTextBanner, complexImHashBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        List<String> hrefs = findBannerHrefs(adGroupInfo1);
        assertThat(hrefs, containsInAnyOrder(
                complexTextBanner.getBanner().getHref(),
                complexImHashBanner.getBanner().getHref()));

        assertThat(getClientBannersCount(), is(2));
    }

    private ImageHashBannerInfo createImageHashBanner() {
        ImageHashBannerInfo imageHashBannerInfo = new ImageHashBannerInfo()
                .withBanner(activeImageHashBanner(campaignId, adGroupInfo1.getAdGroupId()))
                .withAdGroupInfo(adGroupInfo1);
        steps.bannerSteps().createBanner(imageHashBannerInfo.getBanner(), adGroupInfo1);
        return imageHashBannerInfo;
    }

    private String createImage() {
        return steps.bannerSteps().createImageAdImageFormat(adGroupInfo1.getClientInfo()).getImageHash();
    }
}
