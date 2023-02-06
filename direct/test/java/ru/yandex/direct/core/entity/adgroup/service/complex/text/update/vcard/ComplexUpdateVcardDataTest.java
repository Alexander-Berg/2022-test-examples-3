package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.vcard;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateVcardDataTest extends ComplexUpdateVcardTestBase {

    // в контексте добавления баннеров

    @Test
    public void adGroupWithAddedBannerWithVcard() {
        Vcard vcard = randomApartVcard();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner()
                .withVcard(vcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));
        assertThat("данные визитки не соответствуют ожидаемым",
                actualVcards.get(0).getApart(),
                equalTo(vcard.getApart()));

        assertThat("баннер должен быть прилинкован к соответствующей визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualVcards.get(0).getId()));
    }

    @Test
    public void adGroupWithTwoAddedBannersWithVcards() {
        Vcard vcard1 = randomApartVcard();
        Vcard vcard2 = randomApartVcard();
        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner()
                .withVcard(vcard1);
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner()
                .withVcard(vcard2);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(2));
        Vcard actualVcard1 = findVcardByApartWithAssertion(actualVcards, vcard1);
        Vcard actualVcard2 = findVcardByApartWithAssertion(actualVcards, vcard2);

        assertThat("первый баннер должен быть прилинкован к соответствующей визитке",
                actualBanner1.getVcardId(),
                equalTo(actualVcard1.getId()));
        assertThat("второй баннер должен быть прилинкован к соответствующей визитке",
                actualBanner2.getVcardId(),
                equalTo(actualVcard2.getId()));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithEmptyAddedBannerAndAddedBannersWithVcard() {
        createSecondAdGroup();

        Vcard vcard = randomApartVcard();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner()
                .withVcard(vcard);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                findBanners(adGroupInfo1), emptyIterable());

        var actualBanners = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));
        assertThat("данные визитки не соответствуют ожидаемым",
                actualVcards.get(0).getApart(),
                equalTo(vcard.getApart()));

        assertThat("баннер должен быть прилинкован к соответствующей визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualVcards.get(0).getId()));
    }

    // проверка отсутствия влияния vcardId в контексте добавления баннера

    @Test
    public void adGroupWithAddedBannerWithVcardId() {
        VcardInfo existingVcardInfo = createRandomApartVcard();

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner();
        complexBanner.getBanner().withVcardId(existingVcardInfo.getVcardId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        assertThat("баннер не должен быть прилинкован к визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                nullValue());
    }

    @Test
    public void adGroupWithAddedBannerWithVcardAndVcardId() {
        VcardInfo existingVcardInfo = createRandomApartVcard();

        Vcard addedVcard = randomApartVcard();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner()
                .withVcard(addedVcard);
        complexBanner.getBanner().withVcardId(existingVcardInfo.getVcardId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(2));

        Vcard actualAddedVcard = findVcardByApartWithAssertion(actualVcards, addedVcard);

        assertThat("баннер должен быть прилинкован к вложенной в него визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualAddedVcard.getId()));
    }

    // в контексте обновления баннеров

    @Test
    public void adGroupWithUpdatingBannerVcardFromNull() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        Vcard vcard = randomApartVcard();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(vcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));
        assertThat("данные визитки не соответствуют ожидаемым",
                actualVcards.get(0).getApart(),
                equalTo(vcard.getApart()));

        assertThat("баннер должен быть прилинкован к соответствующей визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualVcards.get(0).getId()));
    }

    @Test
    public void adGroupWithUpdatingBannerVcardToNull() {
        VcardInfo vcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, vcardInfo);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));

        assertThat("баннер не должен быть прилинкован к визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                nullValue());
    }

    @Test
    public void adGroupWithUpdatingBannerVcardFromOneToAnother() {
        VcardInfo oldVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, oldVcardInfo);

        Vcard newVcard = randomApartVcard();

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(newVcard);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(2));

        Vcard actualNewVcard = findVcardByApartWithAssertion(actualVcards, newVcard);

        assertThat("баннер должен быть прилинкован к новой визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualNewVcard.getId()));
    }

    @Test
    public void adGroupWithUpdatedBannerWithUnchangedVcard() {
        VcardInfo oldVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, oldVcardInfo);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(oldVcardInfo.getVcard());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));

        assertThat("баннер должен быть прилинкован к старой визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualVcards.get(0).getId()));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithEmptyUpdatedBannerAndUpdatedBannerWithVcard() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo2);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        Vcard vcard = randomApartVcard();
        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner(bannerInfo2)
                .withVcard(vcard);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                findBanners(adGroupInfo1), emptyIterable());

        var actualBanners = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(1));

        assertThat("первый баннер не должен быть прилинкован к визитке",
                actualBanner1.getVcardId(),
                nullValue());
        assertThat("второй баннер должен быть прилинкован к визитке",
                actualBanner2.getVcardId(),
                equalTo(actualVcards.get(0).getId()));
    }

    // проверка отсутствия влияния vcardId в контексте обновления баннера

    @Test
    public void adGroupWithUpdatedBannerWithVcardId() {
        VcardInfo existingVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        complexBanner.getBanner().withVcardId(existingVcardInfo.getVcardId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        assertThat("баннер не должен быть прилинкован к визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                nullValue());
    }

    @Test
    public void adGroupWithUpdatingBannerVcardFromNullAndVcardId() {
        VcardInfo existingVcardInfo = createRandomApartVcard();
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        Vcard addedVcard = randomApartVcard();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withVcard(addedVcard);
        complexBanner.getBanner().withVcardId(existingVcardInfo.getVcardId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(2));

        Vcard actualAddedVcard = findVcardByApartWithAssertion(actualVcards, addedVcard);

        assertThat("баннер должен быть прилинкован к вложенной в него визитке",
                ((TextBanner) actualBanners.get(0)).getVcardId(),
                equalTo(actualAddedVcard.getId()));
    }

    // в контексте добавления и обновления баннеров вместе

    @Test
    public void adGroupWithUpdatedBannerWithVcardAndAdGroupWithUpdatedAndAddedBannersWithVcard() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        Vcard vcard1 = randomApartVcard();
        Vcard vcard2 = randomApartVcard();
        Vcard vcard3 = randomApartVcard();
        ComplexTextBanner updatedComplexBanner1 = randomTitleTextComplexBanner(bannerInfo1)
                .withVcard(vcard1);
        ComplexTextBanner updatedComplexBanner2 = randomTitleTextComplexBanner(bannerInfo2)
                .withVcard(vcard2);
        ComplexTextBanner addedComplexBanner = randomTitleTextComplexBanner()
                .withVcard(vcard3);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(updatedComplexBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(addedComplexBanner, updatedComplexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        var actualBanners1 = findBanners(adGroupInfo1);
        var actualBanners2 = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в первой группе не соответствует ожидаемому",
                actualBanners1, hasSize(1));
        assumeThat("количество баннеров во второй группе не соответствует ожидаемому",
                actualBanners2, hasSize(2));

        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners1, updatedComplexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners2, updatedComplexBanner2);
        var actualBanner3 = findBannerByTitleWithAssumption(actualBanners2, addedComplexBanner);

        List<Vcard> actualVcards = findClientVcards();
        assertThat("количество визиток у клиента не соответствует ожидаемому",
                actualVcards, hasSize(3));
        Vcard actualVcard1 = findVcardByApartWithAssertion(actualVcards, vcard1);
        Vcard actualVcard2 = findVcardByApartWithAssertion(actualVcards, vcard2);
        Vcard actualVcard3 = findVcardByApartWithAssertion(actualVcards, vcard3);

        assertThat("первый баннер должен быть прилинкован к соответствующей визитке",
                actualBanner1.getVcardId(),
                equalTo(actualVcard1.getId()));
        assertThat("второй баннер должен быть прилинкован к соответствующей визитке",
                actualBanner2.getVcardId(),
                equalTo(actualVcard2.getId()));
        assertThat("третий баннер должен быть прилинкован к соответствующей визитке",
                actualBanner3.getVcardId(),
                equalTo(actualVcard3.getId()));
    }

    private Vcard findVcardByApartWithAssertion(List<Vcard> vcards, Vcard vcardToFind) {
        String apartToFind = vcardToFind.getApart();
        List<Vcard> found = filterList(vcards, vc -> vc.getApart().equals(apartToFind));
        assertThat("не найдена визитка по apart = " + apartToFind,
                found, not(emptyIterable()));
        assertThat("найдена более одной визитки по apart = " + apartToFind,
                found, hasSize(1));
        return found.get(0);
    }
}
