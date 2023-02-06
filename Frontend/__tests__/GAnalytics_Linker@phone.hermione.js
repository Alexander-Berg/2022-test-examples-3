const { URL } = require('url');
const PO = require('../../../../hermione/page-objects');

specs({
    feature: 'GAnalytics Linker',
}, () => {
    hermione.only.in('chrome-phone', 'Ускоряем браузеронезависимые тесты');
    describe('Провязывание сессии стратегией "ga"', () => {
        hermione.only.notIn('safari13');
        it('Клик по ссылке в основном контенте страницы', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-ga.json'))
                .yaWaitForVisible('.mvideo-info__item-content')
                // Без подскрола Гермиона не может кликнуть по элементу.
                .yaScrollPage('.mvideo-info__item-content')
                .element('.mvideo-info__item-content').then(function({ value }) {
                    // Действие `.click()` не приводит к срабатыванию события `touchstart`,
                    // поэтому приходится использовать метод `.touchClick()`.
                    return this.touchClick(value.ELEMENT);
                })
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.strictEqual(params.get('_ga'), '1.23.456');
                    assert.strictEqual(params.get('dimension3'), 'turbo');
                    assert.strictEqual(params.get('fromTurbo'), '1');
                });
        });

        hermione.only.notIn('safari13');
        it('Клик по ссылке в extras', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-ga.json'))
                .yaWaitForVisible('.mvideo-button_active')
                .element('.mvideo-button_active').then(function({ value }) {
                    // Действие `.click()` не приводит к срабатыванию события `touchstart`,
                    // поэтому приходится использовать метод `.touchClick()`.
                    return this.touchClick(value.ELEMENT);
                })
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.strictEqual(params.get('_ga'), '1.23.456');
                    assert.strictEqual(params.get('dimension3'), 'turbo');
                    assert.strictEqual(params.get('fromTurbo'), '1');
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка формы поиска', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-ga.json'))
                .yaWaitForVisible('.mvideo-button_active')
                .setValue(PO.blocks.inputSearch.control(), 'text')
                .submitForm(PO.blocks.formPresetSearch())
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.deepEqual(params.getAll('_ga'), ['1.23.456'], 'неверное значение параметра _ga');
                    assert.deepEqual(params.getAll('fromTurbo'), ['1'], 'неверное значение параметра fromTurbo');
                    assert.deepEqual(params.getAll('dimension3'), ['turbo'], 'неверное значение параметра dimension3');
                })
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[0]))
                .submitForm(PO.blocks.formPresetSearch())
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[2]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.deepEqual(params.getAll('_ga'), ['1.23.456'], 'повторная отправка формы дублирует параметр _ga');
                    assert.deepEqual(params.getAll('dimension3'), ['turbo'], 'повторная отправка формы дублирует параметр dimension3');
                    assert.deepEqual(params.getAll('fromTurbo'), ['1'], 'повторная отправка формы дублирует параметр fromTurbo');
                });
        });
    });

    hermione.only.in('chrome-phone', 'Ускоряем браузеронезависимые тесты');
    describe('Провязывание сессии стратегией "turbo"', () => {
        hermione.only.notIn('safari13');
        it('Клик по ссылке в основном контенте страницы', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-turbo.json'))
                .yaWaitForVisible('.mvideo-info__item-content')
                .yaScrollPage('.mvideo-info__item-content')
                .element('.mvideo-info__item-content').then(function({ value }) {
                    // Действие `.click()` не приводит к срабатыванию события `touchstart`,
                    // поэтому приходится использовать метод `.touchClick()`.
                    return this.touchClick(value.ELEMENT);
                })
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.strictEqual(params.get('turboGaClientId'), '789.101');
                    assert.isTrue(params.has('turboGAClientIdExpiryTime'));
                    assert.strictEqual(params.get('dimension3'), 'turbo');
                    assert.strictEqual(params.get('fromTurbo'), '1');
                });
        });

        hermione.only.notIn('safari13');
        it('Клик по ссылке в extras', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-turbo.json'))
                .yaWaitForVisible('.mvideo-button_active')
                .element('.mvideo-button_active').then(function({ value }) {
                    // Действие `.click()` не приводит к срабатыванию события `touchstart`,
                    // поэтому приходится использовать метод `.touchClick()`.
                    return this.touchClick(value.ELEMENT);
                })
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.strictEqual(params.get('turboGaClientId'), '789.101');
                    assert.isTrue(params.has('turboGAClientIdExpiryTime'));
                    assert.strictEqual(params.get('dimension3'), 'turbo');
                    assert.strictEqual(params.get('fromTurbo'), '1');
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка формы поиска', function() {
            return this.browser
                .then(getUrl('page/oc-turboext-linker-turbo.json'))
                .yaWaitForVisible('.mvideo-button_active')
                .setValue(PO.blocks.inputSearch.control(), 'text')
                .submitForm(PO.blocks.formPresetSearch())
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[1]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.deepEqual(params.getAll('turboGaClientId'), ['789.101']);
                    assert.lengthOf(params.getAll('turboGAClientIdExpiryTime'), 1);
                    assert.deepEqual(params.getAll('dimension3'), ['turbo']);
                    assert.deepEqual(params.getAll('fromTurbo'), ['1']);
                })
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[0]))
                .submitForm(PO.blocks.formPresetSearch())
                .yaWaitForNewTab()
                .getTabIds().then(tabIds => this.browser.switchTab(tabIds[2]))
                .url().then(function({ value }) {
                    const params = new URL(value).searchParams;
                    assert.deepEqual(params.getAll('turboGaClientId'), ['789.101'], 'повторная отправка формы дублирует параметр');
                    assert.lengthOf(params.getAll('turboGAClientIdExpiryTime'), 1, 'повторная отправка формы дублирует параметр');
                    assert.deepEqual(params.getAll('dimension3'), ['turbo'], 'повторная отправка формы дублирует параметр');
                    assert.deepEqual(params.getAll('fromTurbo'), ['1'], 'повторная отправка формы дублирует параметр');
                });
        });
    });
});

/**
 * Запросить страницу
 *
 * @param {string} stub
 * @returns {function}
 */
function getUrl(stub) {
    return function() {
        return this
            .url(`?stub=${encodeURIComponent(stub)}&exp_flags=analytics-disabled=0`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForExist('#YAnalyticsFrame')
            // Отправляем clientId вручную, чтобы не зависить от загрузки GA
            .execute(function() {
                window.postMessage(JSON.stringify({
                    action: 'info',
                    alias: 'ga',
                    namespace: 'YAnalytics',
                    params: {},
                    result: {
                        linkerParam: '_ga=1.23.456',
                        clientId: '789.101',
                    },
                }), '*');
            });
    };
}
