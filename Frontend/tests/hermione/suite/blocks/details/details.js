const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Страница детального прогноза', function() {
        const { details } = selectors;
        const testCases = {
            temp: { name: 'Temperature', selector: details.DetailsTemp },
            wind: { name: 'Wind', selector: details.DetailsWind },
            humidity: { name: 'Humidity', selector: details.DetailsHumidity },
            pressure: { name: 'Pressure', selector: details.DetailsPressure },
            sun: { name: 'Sun', selector: details.DetailsSun },
            other: { name: 'Other', selector: details.DetailsOther },
        };

        it.langs.full();
        it('Блоки детального прогноза', async function() {
            await this.browser
                .ywOpenPage('moscow/details', {
                    lang: this.lang,
                    query: {
                        usemock: 'all_wind_directions'
                    },
                })
                .ywWaitForVisible(details.Tabs, 10000)
                .ywRemoveShadowFromContainer(details.CardContainer)
                .assertView(testCases.temp.name, testCases.temp.selector)
                .assertView(testCases.wind.name, testCases.wind.selector)
                .assertView(testCases.humidity.name, testCases.humidity.selector)
                .assertView(testCases.pressure.name, testCases.pressure.selector)
                .assertView(testCases.sun.name, testCases.sun.selector)
                .assertView(testCases.other.name, testCases.other.selector);
        });
    });
});
