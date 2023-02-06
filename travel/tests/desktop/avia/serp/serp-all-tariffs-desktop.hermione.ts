import {serp} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';

describe(serp.name, () => {
    it('Проверка выбора тарифов на выдаче в десктопе', async function () {
        const testApp = new TestAviaApp(this.browser);

        await testApp.goToSearchPage({
            from: {name: 'Екатеринбург', id: 'c54'},
            to: {name: 'Санкт-Петербург', id: 'c2'},
            startDate: moment().add(1, 'months').format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'c=0,9144',
        });

        await testApp.searchPage.waitForSearchComplete();

        const firstVariant = await testApp.searchPage.variants.first();

        assert.isTrue(
            await firstVariant.desktopResultVariant.isBaggageInfoVisible(),
            'Должна отображаться багажная информация в сниппете',
        );

        await firstVariant.desktopResultVariant.buyButton.click();

        assert.isTrue(
            await firstVariant.desktopResultVariant.tariffSelectorPopup.isVisible(),
            'Должен открыться тарифный попап',
        );

        const cards = firstVariant.desktopResultVariant.tariffSelectorPopup;

        assert.isTrue(
            await cards.every(card => {
                return card.isTariffInfoVisible();
            }),
            'Должна присутствовать тарифная информация в каждом блоке',
        );
    });
});
