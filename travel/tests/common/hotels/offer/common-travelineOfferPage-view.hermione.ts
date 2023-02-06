import {travellineOfferPage} from 'suites/hotels';
import {assert} from 'chai';

import {TestTravellineOfferPage} from 'helpers/project/hotels/pages/TestTravellineOfferPage/TestTravellineOfferPage';

const {name: suiteName} = travellineOfferPage;

describe(suiteName, function () {
    it('Общий вид страницы', async function () {
        const page = new TestTravellineOfferPage(this.browser);

        await page.goToOfferPage();

        assert.isTrue(
            await page.logo.isVisible(),
            'На странице должен отображаться логотип',
        );

        assert.isFalse(
            await page.checkbox.isChecked(),
            'На странице должен присутствовать неотмеченный чекбокс',
        );

        assert.isTrue(
            await page.button.isVisible(),
            'На странице должна быть кнопка',
        );
    });
});
