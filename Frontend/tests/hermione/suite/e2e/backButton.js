const assert = require('chai').assert;
const selectors = require('../../page-objects');

describe('e2e', function() {
    /**
     * этот тест нужно прогонять в браузере с разумной высотой, так как в нем открываются карты
     * и в браузерах с высотой окна 3000px, webdriver падает, так как ему не хватает памяти
     */
    hermione.only.in('chromeMobile414x700');
    it('Стрелка назад должна переводить на предыдущий экран', async function() {
        const compareObj = {
            initValue: '',
            compareValue: null
        };

        await this.browser
            .ywOpenPage('moscow', {
                lang: this.lang,
                query: {
                    usemock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
                    showmethehamster: {
                        spa_maps: 1,
                        spa_maps_user_report: 0,
                        spa_maps_ugc_button: 0,
                        spa_maps_ugc_promo: 0,
                        spa_maps_ugc: 0
                    },
                },
            })
            .ywWaitForVisible(selectors.mainScreen)
            .ywHideCamerasAndNews()
            .click(selectors.index.MapImg)
            .ywWaitForVisible(selectors.mapsMap)
            .leftClick(selectors.maps.Screen, 100, 300)
            .pause(3000)
            .click(selectors.maps.ForecastBtn)
            .ywWaitForVisible(selectors.mainScreen)
            .execute(selector => {
                const els = document.querySelectorAll(selector.index.LocationTitle);
                const elem = els[els.length - 1];
                const initTitle = elem && elem.innerHTML;

                document.querySelector(selector.index.ForecastFirst).click();
                return initTitle;
            }, selectors)
            .then(res => {
                compareObj.initValue = res.value;
            })
            .ywWaitForVisible(selectors.detailsScreen)
            .click(selectors.details.BackBtn)
            .ywWaitForVisible(selectors.mainScreen)
            .execute((selector, obj) => {
                const els = document.querySelectorAll(selector.index.LocationTitle);
                const elem = els[els.length - 1];
                const title = elem && elem.innerHTML;

                obj.compareValue = title;
                return obj;
            }, selectors, compareObj)
            .then(res => {
                assert.equal(res.value.initValue, res.value.compareValue);
            });
    });
});
