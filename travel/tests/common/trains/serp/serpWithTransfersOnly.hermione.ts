import {assert} from 'chai';
import {serp} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {msk, tixvin} from 'helpers/project/trains/data/cities';
import skipBecauseProblemWithIM from 'helpers/skips/skipBecauseProblemWithIM';
import getNextWeekRandomDay from 'helpers/project/trains/utils/getNextWeekRandomDay';
import {TestTrainsMobileFilters} from 'helpers/project/trains/components/TestTrainsMobileFilters';

const {name: suiteName} = serp;

describe(suiteName, () => {
    hermione.config.testTimeout(4 * MINUTE);

    skipBecauseProblemWithIM();
    it('Поезда / Выдача Проверка вариантов с пересадками', async function () {
        const {dateString} = getNextWeekRandomDay();
        const app = new TestTrainsApp(this.browser);

        await app.goToSearchPage(msk.slug, tixvin.slug, dateString, true);

        const {searchPage} = app;
        const {
            filters,
            variants,
            preloader,
            searchHeader,
            searchToolbar,
            notificationBanner,
        } = searchPage;

        assert.isTrue(
            await preloader.isDisplayed(),
            'Во время начала поллинга должны отображаться скелетоны',
        );

        await searchPage.waitVariantsAndTariffsLoaded();

        if (searchPage.isDesktop) {
            assert.isTrue(
                await filters.isDisplayed(),
                'Блок с фильтрами должен быть отображен',
            );

            assert.isTrue(
                await searchToolbar.isDisplayed(),
                'Блок с сортировками должен быть отображен',
            );

            assert.isTrue(
                await searchHeader.isDisplayed(),
                'Заголовок над списком вариантов должен быть отображен',
            );

            assert.include(
                await searchHeader.getText(),
                `из ${msk.nameGenitive} в ${tixvin.nameAccusative}`,
                'Заголовок над списком вариантов должен содержать информацию о пунктах маршрута',
            );
        }

        if (filters.isTouch && filters instanceof TestTrainsMobileFilters) {
            assert.isTrue(
                await filters.toggleButton.isDisplayed(),
                'Кнопка переключения видимости фильтров должна быть отображена',
            );
        }

        assert.isTrue(
            await notificationBanner.isDisplayed(),
            'Должен отображаться информационный баннер',
        );

        assert.include(
            await notificationBanner.getText(),
            'Найдены маршруты только с пересадками.',
            'Должен отображаться информационный баннер, указывающий на выдачу с вариантами только с пересадкими',
        );

        assert.isTrue(
            await variants.checkVariantsWithTransfer(),
            'Все варианты должны быть с пересадками',
        );

        await variants.checkVariantsDirectionMainFields();
        await variants.checkVariantsSegments();
    });
});
