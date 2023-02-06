import {serp} from 'suites/avia';
import moment from 'moment';
import {assert} from 'chai';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import dateFormats from 'helpers/utilities/date/formats';

describe(serp.name, () => {
    it('Проверка выбора тарифов на выдаче в таче', async function () {
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

        assert.isTrue(
            await testApp.searchPage.sorting.ascIcon.isVisible(),
            'Должен быть выбран порядок сортировки По возрастанию',
        );

        assert.equal(
            await testApp.searchPage.sorting.typeSelect.getValue(),
            'Сначала рекомендуемые',
            'На кнопке должно быть указано "Сначала рекомендуемые"',
        );

        const firstVariant = await testApp.searchPage.variants.first();

        assert.isTrue(
            await firstVariant.mobileResultVariant.baggageInfo.isVisible(),
            'Должна отображаться багажная информация в сниппете',
        );

        await firstVariant.mobileResultVariant.baggageInfo.click();

        const cards = firstVariant.mobileResultVariant.tariffSelectorPopup;

        assert.isTrue(
            await cards.isVisible(),
            'Должен открыться тарифный попап',
        );

        assert.isTrue(
            await cards.every(card => {
                return card.isTariffInfoVisible();
            }),
            'Должна присутствовать тарифная информация в каждом блоке',
        );

        await this.browser.back();

        await cards.waitForHidden(2000);

        assert.isFalse(
            await cards.isVisible(),
            'Должен закрыться тарифный попап',
        );

        await assert.isFalse(
            await cards.every(card => {
                return card.isTariffInfoVisible();
            }),
            'Должна отсутствовать информация из блоков',
        );

        await firstVariant.mobileResultVariant.baggageInfo.click();

        assert.isTrue(
            await cards.isVisible(),
            'Должен открыться тарифный попап',
        );
    });
});
