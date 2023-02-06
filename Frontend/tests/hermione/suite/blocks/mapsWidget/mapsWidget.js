const selectors = require('../../../page-objects').index;

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Виджет карт', function() {
            it.langs.full();
            it('Виджет карт', async function() {
                await this.browser
                    .ywOpenPage('moscow', {
                        lang: this.lang,
                        query: {
                            usemock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
                        },
                    })
                    .ywWaitForVisible(selectors.MapsWidget, 5000)
                    .ywHideCamerasAndNews()
                    .assertView('MapsWidget', selectors.MapsWidget, { invisibleElements: [selectors.MobileWidgetPromo] });
            });

            it('Виджет карт без наукаста', async function() {
                await this.browser
                    .ywOpenPage('los-angeles', {
                        lang: this.lang
                    })
                    .ywWaitForVisible(selectors.MapsWidget, 5000)
                    .ywHideCamerasAndNews()
                    .assertView('MapsWidget', selectors.MapsWidget, { invisibleElements: [selectors.MobileWidgetPromo] });
            });
        });
    });
});
