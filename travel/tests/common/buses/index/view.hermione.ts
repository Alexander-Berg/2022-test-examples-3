import {assert} from 'chai';
import {SUITES} from 'suites/buses';

import {SECOND} from 'helpers/constants/dates';

import TestApp from 'helpers/project/TestApp';

describe(SUITES.pages.index.name, () => {
    it('Общий вид главной страницы.', async function () {
        await this.browser.url(SUITES.pages.index.url);

        const {busesApp} = new TestApp(this.browser);
        const {indexPage} = busesApp;

        assert.isTrue(
            await indexPage.isDisplayed(),
            'Должна отображаться страница.',
        );

        assert.isTrue(
            !(await indexPage.previousSearches.isDisplayed()),
            'Не должно быть предыдущих поисков при первом открытии страницы.',
        );

        assert.isTrue(
            await indexPage.advantages.isVisible(),
            'Должны отображаться преимущества Автобусов.',
        );
        assert.isTrue(
            await indexPage.advantages.advantages.every(async advantage => {
                return (
                    (await advantage.icon.isVisible()) &&
                    (await advantage.description.isVisible())
                );
            }),
            'Все преимущества должны иметь картинку и описание',
        );

        assert.equal(
            await indexPage.advantages.title.getText(),
            `10 ${
                indexPage.isTouch ? 'млн' : 'миллионов'
            } путешественников ежегодно бронируют у нас билеты на автобусы, номера в отелях и туры`,
            'Должен отображаться заголовок "10 миллионов путешественников ежегодно бронируют у нас билеты на автобусы, номера в отелях и туры" в блоке преимуществ',
        );

        assert.isTrue(
            await indexPage.crossLinksGallery.isVisible(),
            'Должен отображаться блок "Билеты на автобусы по России и СНГ"',
        );

        assert.isAbove(
            await indexPage.crossLinksGallery.items.count(),
            0,
            'Должны быть предложения по другим направлениям',
        );

        assert.equal(
            await indexPage.crossLinksGallery.title.getText(),
            'Билеты на автобусы по России и СНГ',
            'Должен отображаться заголовок "Билеты на автобусы по России и СНГ" в блоке',
        );

        assert.isTrue(
            await indexPage.howToBuyATicket.isVisible(),
            'Должны отображаться шаги покупки билета.',
        );

        assert.equal(
            await indexPage.howToBuyATicket.title.getText(),
            'Как купить билет на автобус онлайн',
            'Заголовок блока с шагами покупки должен быть "Как купить билет на автобус онлайн"',
        );

        if (!indexPage.isTouch) {
            await indexPage.searchForm.fromSuggest.setSuggestValue('Москва');
            await indexPage.searchForm.toSuggest.setSuggestValue(
                'Санкт-Петербург',
            );
            await indexPage.searchForm.submitButton.click();

            const {searchPage} = busesApp;

            assert.isTrue(
                await searchPage.waitUntilLoaded(),
                'Должна отображать страница поиска.',
            );

            this.browser.back();

            assert.isTrue(
                await indexPage.previousSearches.isDisplayed(),
                'Должны отображаться предыдущие поиски.',
            );
        }

        await this.browser.url(
            `${SUITES.pages.index.url}?lr=${SUITES.regions.ekb.id}`,
        );

        assert.isTrue(
            await indexPage.isDisplayed(),
            'Должна отображаться страница.',
        );

        assert.equal(
            await indexPage.searchForm.fromSuggest.getInputValue(),
            'Екатеринбург',
            'Должна отображаться верная геопозиция в саджесте - Екатеринбург.',
        );

        await this.browser.url(
            `${SUITES.pages.index.url}?lr=${SUITES.regions.msk.id}`,
        );

        assert.isTrue(
            await indexPage.isDisplayed(),
            'Должна отображаться страница.',
        );

        // Ждем загрузки саджеста
        await this.browser.pause(5 * SECOND);

        assert.equal(
            await indexPage.searchForm.fromSuggest.getInputValue(),
            'Москва',
            'Должна отображаться верная геопозиция в саджесте - Москва.',
        );
    });
});
