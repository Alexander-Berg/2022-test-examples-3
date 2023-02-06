import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';

describe('Портал: Главная', function () {
    it('Рекламный блок', async function () {
        const aviaApp = new TestAviaApp(this.browser);

        await aviaApp.goToIndexPage();

        assert.isTrue(
            await aviaApp.indexPage.adfoxBanner.isDisplayed(),
            'Должен отображаться рекламный блок на главной Авиа',
        );

        const trainsApp = new TestTrainsApp(this.browser);

        await trainsApp.goToIndexPage();

        assert.isTrue(
            await trainsApp.indexPage.adfoxBanner.isDisplayed(),
            'Должен отображаться рекламный блок на главной ЖД',
        );

        const hotelsApp = new TestHotelsApp(this.browser);

        await hotelsApp.goToIndexPage();

        assert.isTrue(
            await hotelsApp.indexPage.adfoxBanner.isDisplayed(),
            'Должен отображаться рекламный блок на главной Отелей',
        );

        const busesApp = new TestBusesApp(this.browser);

        await busesApp.goToIndexPage();

        assert.isTrue(
            await busesApp.indexPage.adfoxBanner.isDisplayed(),
            'Должен отображаться рекламный блок на главной Автобусов',
        );
    });
});
