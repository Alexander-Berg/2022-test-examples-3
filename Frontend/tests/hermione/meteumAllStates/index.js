const selectors = require('../page-objects');

const langs = ['hu', 'es', 'it', 'de', 'pt', 'ro', 'fr', 'la', 'en', 'br'];

async function runTestByLang({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .assertView(`${lang}_${page}_${subPage}`, selectors.fullPage);
    }
}

async function burgerByLang({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .click(selectors.index.MenuButton)
            .execute(menuSelector => document.querySelector(menuSelector).style.height = 'auto', selectors.index.MenuMenu)
            .assertView(`${lang}_${page}_${subPage}`, selectors.index.MenuMenu);
    }
}

async function mapsWidgetByLang({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .ywWaitForVisible(selectors.index.MapsWidget, 5000)
            .execute(menuSelector => {
                document.querySelector(menuSelector).querySelector('.Slider-Scroll').style.display = 'block';
            }, selectors.index.MapsWidget)
            .assertView(`${lang}_${page}_${subPage}`, selectors.index.MapsWidget);
    }
}

async function forecastChartByLang({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .ywWaitForVisible(selectors.index.ForecastWidget, 5000)
            .execute(button => {
                const b = document.querySelector(button);

                // пришлось клик кидать через execute
                // потому что блок уже может быть открыт при загрузке страницы
                // и тогда тест падает на том, что нет кликабельного элемента
                if (b) {
                    b.click();
                }
            }, selectors.index.ForecastWidgetBtnClosed)
            .pause(500)
            .ywWaitForVisible(selectors.index.ForecastWidgetOpened, 5000)
            .assertView(`${lang}_${page}_${subPage}`, selectors.index.ForecastWidget);
    }
}

async function skiPopupByLang({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .click(selectors.index.SkiResortCardMenu)
            .pause(200)
            .execute(elem => {
                document.querySelector(elem).style.paddingBottom = '100px';
            }, selectors.index.SkiResortCard)
            .assertView(`${lang}_${page}_${subPage}`, selectors.index.SkiResortCard);
    }
}

async function detailsTabs({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .assertView(`${lang}_${page}_${subPage}`, selectors.details.Tabs);
    }
}

async function monthsTabs({ query = {}, page, subPage, path = '' }) {
    for (const lang of langs) {
        await this.browser
            .ywOpenMeteumPage(path, { lang, ...query })
            .assertView(`${lang}_${page}_${subPage}`, selectors.month.Tabs);
    }
}

const skiQueries = {
    lat: 43.67996979,
    lon: 40.20554352,
    showmethehamster: { spa_ski: 1 }
};

hermione.skip.in(/.*/, 'не для CI');
describe('Meteum fullpage', function() {
    this.timeout(240000);
    describe('Main', function() {
        hermione.only.in('chromeMobileMeteum');
        it('Full-with-ski', async function() {
            return runTestByLang.call(this, {
                page: 'main',
                subPage: 'full-ski',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileMeteum');
        it('Burger', async function() {
            return burgerByLang.call(this, {
                page: 'main',
                subPage: 'burger',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileMeteum');
        it('MapsWidget', async function() {
            return mapsWidgetByLang.call(this, {
                page: 'main',
                subPage: 'mapsWidget',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileLandScape');
        it('ForecastChart', async function() {
            return forecastChartByLang.call(this, {
                page: 'main',
                subPage: 'forecastChart',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileMeteum');
        it('skiPopup', async function() {
            return skiPopupByLang.call(this, {
                page: 'main',
                subPage: 'skiPopup',
                query: skiQueries
            });
        });
    });

    describe('Details', function() {
        hermione.only.in('chromeMobileMeteum');
        it('Full-with-ski', async function() {
            return runTestByLang.call(this, {
                path: 'details',
                page: 'details',
                subPage: 'full-ski',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileLandScapeUltra');
        it('Tabs', async function() {
            return detailsTabs.call(this, {
                path: 'details',
                page: 'details',
                subPage: 'tabs',
                query: skiQueries
            });
        });
    });

    describe('Months', function() {
        hermione.only.in('chromeMobileMeteum');
        it('Full-30-days', async function() {
            return runTestByLang.call(this, {
                path: 'month',
                page: 'month',
                subPage: 'full-30-days',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileMeteum');
        it('Full-Jan', async function() {
            return runTestByLang.call(this, {
                path: 'month/january',
                page: 'month',
                subPage: 'full-jan',
                query: skiQueries
            });
        });

        hermione.only.in('chromeMobileLandScapeUltra');
        it('Tabs', async function() {
            return monthsTabs.call(this, {
                path: 'month',
                page: 'month',
                subPage: 'tabs',
                query: skiQueries
            });
        });
    });
});
