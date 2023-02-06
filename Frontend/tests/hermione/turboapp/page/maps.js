const assert = require('chai').assert;
const selectors = require('../../page-objects');

describe('Страницы', function() {
    describe('Карты', function() {
        it('Карта в нативном приложении', async function() {
            await this.browser
                .ywOpenPage('moscow/maps/nowcast', {
                    lang: this.lang,
                    query: { nowcast: '1' }
                })
                .ywWaitForVisible(selectors.maps.Screen, 10000);

            const layerSelectorVisible = await this.browser.isExisting(selectors.maps.LayerSelector);
            const forecastButtonVisible = await this.browser.isExisting(selectors.maps.ForecastBtn);
            const alertVisible = await this.browser.isExisting(selectors.maps.ReportAndAlertAlert);

            const DEFAULT_ERROR_MESSAGE = 'На карте в нативе не должно быть';
            assert.isFalse(layerSelectorVisible, `${DEFAULT_ERROR_MESSAGE} выбора слоёв`);
            assert.isFalse(forecastButtonVisible, `${DEFAULT_ERROR_MESSAGE} кнопки перехода на прогноз`);
            assert.isFalse(alertVisible, `${DEFAULT_ERROR_MESSAGE} алерта`);
        });
    });
});
