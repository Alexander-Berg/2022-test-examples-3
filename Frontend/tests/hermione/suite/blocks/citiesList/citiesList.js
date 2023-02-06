const selectors = require('../../../page-objects').month;

describe('Блоки', function() {
    describe('Страница месяца', function() {
        it.langs.full();
        it('Другие города', async function() {
            await this.browser
                .ywOpenPage('month/april', {
                    lang: this.lang,
                    query: {
                        usemock: `turboapp-month${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                        mockgql: `gql-month-april${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                    },
                })
                .ywInitLazyLoading()
                .ywWaitForVisible(selectors.CitiesList, 5000)
                .ywRemoveShadowFromContainer(selectors.CitiesList)
                .ywHideCamerasAndNews()
                .assertView('citiesList', selectors.CitiesList)
                .assertView('citiesList_with_countries', selectors.CitiesListWithCountries);
        });
    });
});
