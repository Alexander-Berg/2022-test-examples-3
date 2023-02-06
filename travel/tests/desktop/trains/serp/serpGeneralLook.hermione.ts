import {map} from 'p-iteration';
import {assert} from 'chai';
import moment, {Moment} from 'moment';
import {serp} from 'suites/trains';
import {URL} from 'url';
import {random} from 'lodash';

import {msk, spb} from 'helpers/project/trains/data/cities';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';
import {setAbExperiment} from 'helpers/utilities/experiment/setAbExperiment';

const {name: suiteName} = serp;

describe(suiteName, () => {
    skipBecauseProblemWithIM();
    it('Общий вид выдачи в десктопе - шапка', async function () {
        const {date, dateString} = getDate();
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;
        const {
            portalHeader: {portalLogo, navigations, userInfo},
            searchForm,
            searchHeader,
            searchToolbar,
            filters,
            variants,
        } = searchPage;

        assert.isTrue(
            await portalLogo.isDisplayed(),
            'Логотип путешествий не отображен',
        );
        assert.isTrue(
            await navigations.areDisplayed(['hotels', 'avia', 'bus']),
            'Ссылки на сервисы не отображены',
        );
        assert.isTrue(
            await userInfo.accountLink.isDisplayed(),
            'Иконка личного кабинета не отображена',
        );
        assert.isTrue(
            await userInfo.login.isDisplayed(),
            'Кнопка войти не отображена',
        );
        assert.isTrue(
            await searchForm.isDisplayed(),
            'Должна отображаться форма поиска',
        );

        const directionFrom = await searchForm.fromSuggest.getInputValue();
        const directionTo = await searchForm.toSuggest.getInputValue();
        const when =
            await searchForm.whenDatePicker.startTrigger.value.getText();

        assert.equal(directionFrom, 'Москва', 'from в поисковой форме неверно');
        assert.equal(
            directionTo,
            'Санкт-Петербург',
            'to в поисковой форме неверно',
        );

        assert.equal(
            when,
            date.locale('ru').format('D MMM'),
            'when в поисковой форме неверно',
        );

        assert.isTrue(
            await searchHeader.isDisplayed(),
            'Header выдачи не отображен',
        );

        const minPriceFromVariants = await variants.getVariantMinPrice();
        const minPriceFromHeader = await searchHeader.getPriceFrom();

        assert.equal(
            minPriceFromVariants,
            minPriceFromHeader,
            'Минимальная цена в заголовке определяется неверно на странице поиска',
        );

        assert.isTrue(
            await searchToolbar.isDisplayed(),
            'Блок с сортировками не отображен',
        );
        assert.isTrue(
            await filters.isDisplayed(),
            'Блок с фильтрами не отображен',
        );
    });

    skipBecauseProblemWithIM();
    it('Общий вид выдачи в десктопе - время, футер', async function () {
        const {date, dateString} = getDate();
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;
        const {
            searchFooter,
            breadcrumbs,
            footer,
            variants,
            variantsDateSeparator,
        } = searchPage;

        await searchPage.waitVariantsAndTariffsLoaded();

        const variantAndSegment =
            await variants.findVariantAndSegmentByOptions();

        assert.exists(
            variantAndSegment,
            'Должен быть хотя бы один вариант в списке',
        );

        await variantAndSegment.variant.checkVariantSegments();

        if (await variantsDateSeparator.isDisplayed()) {
            const currentDateEnd = date
                .locale('ru')
                .format('D MMM')
                .slice(0, -1)
                .toUpperCase();
            const nextDateFull = await variantsDateSeparator.getText();

            assert.isTrue(
                !nextDateFull.endsWith(currentDateEnd),
                'Под линией перехода суток отображается та же дата',
            );
        }

        assert.equal(
            `Посмотреть рейсы обратно на ${date.locale('ru').format('D MMMM')}`,
            await searchFooter.linkBackward.getText(),
            'Ссылка "Рейс обратно" некорректно отображается',
        );
        assert.isTrue(
            await searchFooter.partnerInfoDisclaimer.isDisplayed(),
            'Дисклеймер партнерской информации не отображается',
        );

        const breadcrumbsItems = await breadcrumbs.items;

        const breadcrumbsItemsExpected = [
            'Ж/д билеты',
            'Купить билеты на поезда Москва — Санкт-Петербург',
            `На ${date.locale('ru').format('D MMMM')}`,
        ];

        if (breadcrumbsItemsExpected.length !== breadcrumbsItems.length) {
            throw new Error('Количество хлебных кромешек неверно');
        }

        const texts = await map(breadcrumbsItems, item => item.getText());

        for (let i = 0; i < breadcrumbsItemsExpected.length; i++) {
            assert.include(
                texts[i],
                breadcrumbsItemsExpected[i],
                `Хлебная крошка ${i} неверна`,
            );
        }

        assert.isTrue(
            await footer.isDisplayed(),
            'Портальный футер не отображается',
        );
    });

    skipBecauseProblemWithIM();
    it('Общий вид выдачи в десктопе - сегмент подробно', async function () {
        const {dateString} = getDate();
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;
        const {variants} = searchPage;

        await variants.checkVariantsSegments();
    });

    skipBecauseProblemWithIM();
    it('Общий вид выдачи в десктопе - переход на выбор мест', async function () {
        const {dateString} = getDate();
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;
        const {variants} = searchPage;

        const variantAndSegment =
            await variants.findVariantAndSegmentByOptions();

        assert.exists(
            variantAndSegment,
            'Должен быть хотя бы один вариант в списке',
        );

        await variantAndSegment.variant.clickToBoyActionButton();

        const currentUrl = await this.browser.getUrl();
        const {pathname} = new URL(currentUrl);

        assert.equal(
            pathname,
            '/trains/order/',
            'При клике на выбор места произошел редирект на неверный урл',
        );
    });

    it('Общий вид выдачи в десктопе - отельный кросс-сейл', async function () {
        await setAbExperiment(
            this.browser,
            'KOMOD_trains_search_hotels_cross_sale',
            'enabled',
        );

        const {dateString} = getDate();
        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;

        await searchPage.crossSaleMap.scrollIntoView();
        await searchPage.crossSaleMap.waitForLoading();

        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.isVisible(),
            'Карта кросс-сейла не отобразилась',
        );
        assert.isTrue(
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.hasActiveHotelCard(),
            'Нет карточки отеля в кросс-сейле',
        );
    });
});

function getDate(): {date: Moment; dateString: string} {
    const date = moment().add(random(0, 10), 'day').add(1, 'month');
    const dateString = date.format('YYYY-MM-DD');

    return {date, dateString};
}
