import moment, {Moment} from 'moment';
import {assert} from 'chai';
import {map} from 'p-iteration';
import {serp} from 'suites/trains';
import {URL} from 'url';
import {random} from 'lodash';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';
import {spb, msk} from 'helpers/project/trains/data/cities';
import {TestTrainsMobileFilters} from 'helpers/project/trains/components/TestTrainsMobileFilters';
import {setAbExperiment} from 'helpers/utilities/experiment/setAbExperiment';

const {name: suiteName} = serp;

describe(suiteName, () => {
    function getRandomDate(): {date: Moment; dateString: string} {
        const date = moment().add(1, 'month').add(random(0, 3), 'day');
        const dateString = date.format('YYYY-MM-DD');

        return {date, dateString};
    }

    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид сниппетов в таче - шапка', async function () {
        const {date, dateString} = getRandomDate();

        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;

        const {
            portalHeader,
            portalHeader: {
                portalLogo,
                userInfo,
                navigationSideSheet,
                searchInformation,
            },
            searchForm,
        } = searchPage;

        assert.isTrue(
            await portalLogo.isDisplayed(),
            'Логотип путешествий не отображен',
        );

        assert.isTrue(
            await userInfo.plus.isDisplayed(),
            'Должна отображаться иконка баллов плюса',
        );

        assert.isTrue(
            await userInfo.favoriteLink.isDisplayed(),
            'Должна отображаться иконка избранного',
        );

        assert.isTrue(
            await navigationSideSheet.toggleButton.isDisplayed(),
            'Должна отображаться иконка бокового меню',
        );

        assert.isTrue(
            await searchInformation.isDisplayed(),
            'Краткая форма поиска не отображена',
        );

        const [directionFrom, directionTo] =
            await searchInformation.getDirections();

        assert.equal(directionFrom, 'Москва', 'from в поисковой форме неверно');
        assert.equal(
            directionTo,
            'Санкт-Петербург',
            'to в поисковой форме неверно',
        );

        await portalHeader.openSearchForm();

        const when =
            await searchForm.whenDatePicker.startTrigger.value.getText();

        assert.equal(
            when,
            date.locale('ru').format('D MMMM'),
            'when в поисковой форме неверно',
        );

        await searchInformation.close();

        assert.isTrue(
            await searchInformation.lupa.isDisplayed(),
            'В форме поиска не отображена лупа',
        );

        assert.isTrue(
            await (
                searchPage.filters as TestTrainsMobileFilters
            ).toggleButton.isDisplayed(),
            'Должна отображаться кнопка переключения фильтров',
        );
    });

    skipBecauseProblemWithIM();
    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид сниппетов в таче - время, футер', async function () {
        const {date, dateString} = getRandomDate();

        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;

        const {
            searchFooter,
            breadcrumbs,
            footer,
            filters,
            variantsDateSeparator,
        } = searchPage;

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

        if ('toggleButton' in filters) {
            assert.isTrue(
                await filters.toggleButton.isDisplayed(),
                'Кнопка фильтры не отображается',
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

    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид сниппетов в таче - сегмент подробно', async function () {
        const {dateString} = getRandomDate();

        const app = new TestTrainsApp(this.browser);

        await app.setSearchAutoMock();
        await app.goToSearchPage(msk.slug, spb.slug, dateString, false);

        const {searchPage} = app;
        const {variants} = searchPage;

        await variants.checkVariantsSegments();
    });

    skipBecauseProblemWithIM();
    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид сниппетов в таче - редиректы', async function () {
        const {dateString} = getRandomDate();

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

        {
            const currentUrl = await this.browser.getUrl();
            const {pathname} = new URL(currentUrl);

            assert.equal(
                pathname,
                '/trains/order/',
                'При клике на место из вкладки "вопрос" произошел редирект на неверный урл',
            );
        }

        {
            const {orderPlacesStepPage} = app;

            await orderPlacesStepPage.waitTrainDetailsLoaded();

            assert.isTrue(
                await orderPlacesStepPage.layout.orderSteps.searchStep.isDisplayed(
                    4000,
                ),
                'Кнопка "Выбор поезда" не отображается',
            );

            await orderPlacesStepPage.layout.orderSteps.searchStep.click();

            await searchPage.waitVariantsAndTariffsLoaded();

            const currentUrl = await this.browser.getUrl();
            const {pathname} = new URL(currentUrl);

            assert.equal(
                pathname,
                '/trains/moscow--saint-petersburg/',
                'При клике на место со страницы "выбора мест" произошел редирект на неверный урл',
            );
        }
    });

    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид сниппетов в таче - отельный кросс-сейл', async function () {
        const {dateString} = getRandomDate();

        await setAbExperiment(
            this.browser,
            'KOMOD_trains_search_hotels_cross_sale',
            'enabled',
        );

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
            await searchPage.crossSaleMap.mapCard.crossSaleMap.map.hasMarkers(),
            'Нет маркеров отелей в карте кросс-сейла',
        );
    });
});
