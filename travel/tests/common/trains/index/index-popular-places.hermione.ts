import {index} from 'suites/trains';
import {assert} from 'chai';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

describe(index.name, () => {
    it('Переход на поиск с популярных ж/д маршрутов', async function () {
        const app = new TestTrainsApp(this.browser);

        await app.goToIndexPage();

        const {indexPage} = app;

        const isDisplayed = await indexPage.crossLinksGallery.isDisplayed();

        assert.isTrue(
            isDisplayed,
            'Должен отображаться блок "Выгодные предложения по ж/д билетам"',
        );

        assert.isAbove(
            await indexPage.crossLinksGallery.items.count(),
            0,
            'Отображается несколько карточек направлений',
        );

        await indexPage.crossLinksGallery.items.forEach(async crossLinkItem => {
            assert.isNotEmpty(
                await crossLinkItem.getFrom(),
                'Должен отображаться пункт отправления',
            );
            assert.isNotEmpty(
                await crossLinkItem.getTo(),
                'Должен отображаться пункт прибытия',
            );

            assert.isTrue(
                await crossLinkItem.price.isVisible(),
                'Должна отображаться цена направления',
            );
        });

        /** Ждем пока вариант станет видимым, в данном случае используем задержку до 30 сек так как наблюдаются частые ложные срабатывания, предположительно из-за задержек на стороне фермы */
        const variantVisibilityDelay = 30000;

        const mskSbpRecipe = await indexPage.crossLinksGallery.items.find(
            async recipe => {
                return (
                    (await recipe.getFrom()) === 'Москва' &&
                    (await recipe.getTo()) === 'Санкт-Петербург'
                );
            },
        );

        if (!mskSbpRecipe) {
            throw new Error('Рецепт из Москвы в Санкт-Петербург не найден');
        }

        await mskSbpRecipe.click();

        await this.browser.switchToNextTab();

        await app.directionPage.title.waitForVisible(variantVisibilityDelay);

        if (app.directionPage.isDesktop) {
            assert.isTrue(
                await app.searchPage.searchForm.whenDatePicker.calendar.isVisible(),
                'Должен быть открыт календарь для десктопа',
            );
        }

        assert.isFalse(
            await app.searchPage.searchForm.whenDatePicker.startTrigger.value.isVisible(),
            'Должно быть пустым поле даты Туда',
        );

        assert.isTrue(
            await (
                await app.directionPage.variants.variants.first()
            ).waitForVisible(variantVisibilityDelay),
            'Должно отображаться расписание поездов',
        );

        assert.equal(
            await app.searchPage.searchForm.fromSuggest.getTriggerValue(),
            'Москва',
            'Должна быть указана Москва в саджесте Откуда',
        );

        assert.equal(
            await app.searchPage.searchForm.toSuggest.getTriggerValue(),
            'Санкт-Петербург',
            'Должен быть указан Санкт-Петербург в саджесте Куда',
        );
    });
});
