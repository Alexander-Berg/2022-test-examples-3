const _ = require('lodash');

describe('Experiments', function() {
    it('открывается страница с экспериментом', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            expFlags: { 'turbo-app-experiment-example': 1 },
        });
        await browser.yaWaitForVisible('.ScreenContent');
        await browser.assertView('experiment-component', '.ExperimentActions');
    });

    it('приехали и работают редьюсеры', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            expFlags: { 'turbo-app-experiment-example': 1 },
        });
        await browser.yaWaitForVisible('.ScreenContent');

        let appState;

        appState = await browser
            .execute(function() {
                return window.Ya.store.getState();
            })
            .then(({ value }) => value);

        assert(_.get(appState, 'cart.myTestCounter') === 0, 'Поле myTestCounter не приехало');

        await browser.click('.ExperimentActions button');
        await browser.click('.ExperimentActions button');

        appState = await browser
            .execute(function() {
                return window.Ya.store.getState();
            })
            .then(({ value }) => value);

        assert(_.get(appState, 'cart.myTestCounter') === 2, 'Поле myTestCounter не изменилось');
    });
});
