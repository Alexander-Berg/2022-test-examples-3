import {serp} from 'suites/avia';
import moment from 'moment';
import {random, sample} from 'lodash';
import {assert} from 'chai';

import {AVIA_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import dateFormats from 'helpers/utilities/date/formats';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {oneWayBOYAviaVariant} from 'helpers/project/avia/data/aviaVariants';

const fromCollection = [{name: 'Москва', id: 'c213'}];
const toCollection = [
    {name: 'Санкт-Петербург', id: 'c2'},
    {name: 'Сочи', id: 'c239'},
];

describe(serp.name, () => {
    it('Проверка отображения признаков заказа на Я.Путешествиях', async function () {
        const app = new TestAviaApp(this.browser, {
            ...AVIA_SUCCESS_TEST_CONTEXT_PARAMS,
            aviaVariants: oneWayBOYAviaVariant,
            mockAviaVariants: true,
        });

        await app.goToSearchPage({
            from: sample(fromCollection) ?? fromCollection[0],
            to: sample(toCollection) ?? toCollection[0],
            startDate: moment()
                .add(random(1, 5), 'day')
                .format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot&c=0,26',
        });

        const searchPage = app.searchPage;

        await searchPage.waitForSearchComplete();

        const correctVariant = await searchPage.findVariantWithBadge(
            'заказ на я.путешествиях',
        );

        assert.exists(
            correctVariant,
            'Должен быть сниппет с бейджем "заказ на я.путешествиях"',
        );

        await correctVariant.moveToOrder();

        const orderPage = app.orderPage;

        await orderPage.waitForLoading();

        assert.isTrue(
            await orderPage.offers.isCorrectLogo('8f239320ef013b826f40'),
            'Должна присутствовать корректная иконка партнёра',
        );
    });
});
