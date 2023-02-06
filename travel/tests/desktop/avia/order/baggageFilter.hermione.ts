import {assert} from 'chai';
import moment from 'moment';
import {order} from 'suites/avia';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {buildNextQueryFilter} from 'helpers/utilities/avia/buildNextQueryFilter';

const whenMoment = moment().add(1, 'days');
const when = whenMoment.format('YYYY-MM-DD');
const getNextFilter = buildNextQueryFilter();

describe(order.name, () => {
    it('Проверка связи между фильтром багажа в поиске и на покупке', async function () {
        const app = new TestAviaApp(this.browser);
        const {searchDesktopPage, orderPage} = app;

        await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Екатеринбург', id: 'c54'},
            startDate: when,
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: getNextFilter(),
        });

        await searchDesktopPage.filters.baggage.scrollIntoView();
        await searchDesktopPage.filters.baggage.click();

        await searchDesktopPage.fog.waitUntilProcessed();

        let searchVariant = await searchDesktopPage.variants.first();

        await searchVariant.moveToOrder();

        await orderPage.waitForLoading();

        assert.isTrue(
            await orderPage.baggageFilter.isChecked(),
            'Фильтр багажа должен быть включен',
        );

        await this.browser.back();

        await searchDesktopPage.filters.baggage.scrollIntoView();
        await searchDesktopPage.filters.baggage.click();

        await searchDesktopPage.fog.waitUntilProcessed();

        // Заново находим сегменты, т.к. линки на них могли потеряться
        searchVariant = await searchDesktopPage.variants.first();

        await searchVariant.moveToOrder();

        await orderPage.waitForLoading();

        assert.isTrue(
            !(await orderPage.baggageFilter.isChecked()),
            'Фильтр багажа должен быть выключен',
        );
    });
});
