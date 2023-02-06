import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    it('Применение промокода незалогин', async function () {
        const testOfferParams = getTestOfferParams({
            occupancy: '2',
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
            priceAmount: 1000,
        });

        const hotelsBookPage = new TestHotelsBookPage(this.browser);

        await hotelsBookPage.goToPage(testOfferParams);
        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        const isVisibleInputBefore =
            await hotelsBookPage.priceInfo.promoCodes.input.isVisible();
        const isVisibleButtonBefore =
            await hotelsBookPage.priceInfo.promoCodes.button.isVisible();

        assert(
            !isVisibleInputBefore,
            'Поле ввода промокода должно быть спрятано до нажатия на чекбокс',
        );
        assert(
            !isVisibleButtonBefore,
            'Кнопка примения промокода должна быть спрятаня до нажатия на чекбокс',
        );

        await hotelsBookPage.priceInfo.promoCodes.checkBox.scrollIntoView();
        await hotelsBookPage.priceInfo.promoCodes.checkBox.click();

        const isVisibleInput =
            await hotelsBookPage.priceInfo.promoCodes.input.isVisible();
        const isVisibleButton =
            await hotelsBookPage.priceInfo.promoCodes.button.isVisible();

        assert(
            !isVisibleInput,
            'Поле ввода промокода не должно отображаться после нажатия на чекбокс',
        );
        assert(
            !isVisibleButton,
            'Кнопка применения промокода не должна отображаться после нажатия на чекбокс',
        );

        const authText =
            await hotelsBookPage.priceInfo.promoCodes.authText.getText();

        assert.equal(
            authText,
            'Использование промокодов доступно только после авторизации',
            'Текст под полем ввода промокода должен соответствовать: "Использование промокодов доступно только после авторизации"',
        );
    });
});
