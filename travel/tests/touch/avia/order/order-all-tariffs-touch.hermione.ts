import {order} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';
import {buildNextQueryFilter} from 'helpers/utilities/avia/buildNextQueryFilter';

const getNextFilter = buildNextQueryFilter();

describe(order.name, () => {
    it('Проверка выбора тарифов на странице варианта в таче', async function () {
        const app = new TestAviaApp(this.browser);
        const {searchPage, orderPage} = app;

        await app.goToSearchPage({
            from: {name: 'Екатеринбург', id: 'c54'},
            to: {name: 'Москва', id: 'c213'},
            startDate: moment().add(1, 'months').format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: getNextFilter(),
        });

        await searchPage.fog.waitUntilProcessed();

        const searchVariant = await searchPage.variants.first();

        await searchVariant.moveToOrder();

        await orderPage.waitForLoading();

        await orderPage.offers.scrollIntoView();

        const cards = orderPage.offers.tariffsOnOrderPage;

        assert.isTrue(
            await cards.every(card => {
                return card.isTariffInfoVisible();
            }),
            'Должна присутствовать тарифная информация в каждом блоке',
        );

        const oldTariffName = await (
            await orderPage.forward.flights.first()
        ).tariffName.getText();

        const middleCard = await cards.at(1);

        const price = await middleCard.price.getText();

        await middleCard.price.click();

        assert.equal(
            await (await orderPage.offers.cheapest).price.getText(),
            price,
            'Должны совпадать цена с жёлтой кнопки и цена из выбранного тарифа',
        );

        const newTariffName = await (
            await orderPage.forward.flights.first()
        ).tariffName.getText();

        assert.notEqual(
            oldTariffName,
            newTariffName,
            'Должно было измениться название тарифа в карточке рейса',
        );
    });
});
