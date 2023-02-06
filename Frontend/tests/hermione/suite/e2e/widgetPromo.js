const assert = require('chai').assert;
const selectors = require('../../page-objects');

const isWidgetOnboardingVisible = async function() {
    return this.browser.isVisible(selectors.index.WidgetOnboarding);
};

const isWidgetTopVisible = async function() {
    return this.browser.isVisible(selectors.index.WidgetTop);
};

describe('e2e', function() {
    describe('Промо виджетов', function() {
        // считаем, что у пользователя не стоит виджет, но можно поставить
        // поэтому передаём spa_widget_promo_force:1
        hermione.only.in('chromeMobile');
        it('Промо виджетов. Главная страница.', async function() {
            // 1. открыли первый раз - виджетов быть не должно
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        showmethehamster: {
                            spa_widget_top: 1,
                            spa_widget_top_maps: 1,
                            spa_widget_onboarding: 1,
                            spa_widget_promo_force: 1,
                            spa_pro_promo: 0,
                            is_autotest_promos: 1
                        },
                        usemock: 'spa_promo' // зафиксировал данные из админки, чтобы тесты не попадали в случае изменения приоритетов
                    }
                })
                .execute(() => {
                    localStorage.clear();
                })
                .refresh() // двойной вызов страницы, но по-другому не гарантировать чистый LS
                .ywWaitForVisible(selectors.mainScreen);

            let widgetOnboardingVisible = await isWidgetOnboardingVisible.call(this);
            assert.isFalse(widgetOnboardingVisible, 'WidgetOnboarding не должен отобразиться на первый заход');

            // 2. открыли второй раз - должен быть widgetOnboarding
            await this.browser
                .refresh()
                .ywWaitForVisible(selectors.mainScreen)
                .ywHideCamerasAndNews()
                .ywWaitForVisible(selectors.index.WidgetOnboarding, 'WidgetOnboarding не появился при повторном открытии страницы');

            // 3. Скрываем виджет, чтобы сохранились данные о его показе в LS
            await this.browser
                .execute(elem => {
                    document.querySelector(elem).remove();
                    // приходится удалять контент слайдера, чтобы клик сработал именно в fade, а не в него
                }, selectors.index.SideblockSlider)
                .click(selectors.index.SideblockFade);

            widgetOnboardingVisible = await isWidgetOnboardingVisible.call(this);
            assert.isFalse(widgetOnboardingVisible, 'WidgetOnboarding не скрылся после клика в закрытие сайдблока');

            // 4. Рефрешим. Не должно быть ни одного виджета.
            // WidgetTop будет показан только через 2 недели после показа widgetOnboarding
            await this.browser
                .refresh()
                .ywWaitForVisible(selectors.mainScreen);

            widgetOnboardingVisible = await isWidgetOnboardingVisible.call(this);
            let widgetTopVisible = await isWidgetTopVisible.call(this);

            assert.isFalse(widgetOnboardingVisible, 'WidgetOnboarding уже был показан и должен быть скрыт');
            assert.isFalse(widgetTopVisible, 'WidgetTop ещё рано отображать');

            // 5. Модифицируем LS widgetOnboarding, как-будто он был показан позже двух недель назад
            // чтобы добиться показа промки widgetTop далее
            // Рефрешим ещё раз и должны увидеть виджет сверху и не должно быть widgetOnboarding
            await this.browser.execute(() => {
                nowDate = new Date();
                const expires = (new Date(nowDate.setDate(nowDate.getDate() + 10))).toUTCString();
                localStorage.setItem('widgetOnboarding', `1; expires=${expires}`);
            });

            await this.browser
                .refresh()
                .ywWaitForVisible(selectors.mainScreen)
                .ywWaitForVisible(selectors.index.WidgetTop, 'WidgetTop не отобразился спустя 2 недели после widgetOnboarding');

            widgetOnboardingVisible = await isWidgetOnboardingVisible.call(this);
            assert.isFalse(widgetOnboardingVisible, 'WidgetOnboarding уже был показан и должен быть скрыт');

            // 6. Рефрешим ещё раз. Оба виджета были показаны и больше их быть не должно.
            // подчищаем за собой LS на всякий случай
            await this.browser
                .refresh()
                .ywWaitForVisible(selectors.mainScreen);

            widgetOnboardingVisible = await isWidgetOnboardingVisible.call(this);
            widgetTopVisible = await isWidgetTopVisible.call(this);

            assert.isFalse(widgetOnboardingVisible, 'WidgetOnboarding уже был показан и должен быть скрыт');
            assert.isFalse(widgetTopVisible, 'WidgetTop уже был показан и должен быть скрыт');

            await this.browser.execute(() => {
                localStorage.clear();
            });
        });
    });
});
