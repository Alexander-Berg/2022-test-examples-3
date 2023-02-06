import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import formatInteger from 'helpers/utilities/numbers/formatInteger';
import dateFormats from 'helpers/utilities/date/formats';
import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    it('Общий вид страницы бронирования отелей', async function () {
        const testOfferParams = getTestOfferParams({
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
            priceAmount: 1000,
        });

        const hotelsBookPage = new TestHotelsBookPage(this.browser);

        await hotelsBookPage.goToPage(testOfferParams);

        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        assert.isTrue(
            await hotelsBookPage.bookSupportPhone.isVisible(),
            'Не отобразился телефон поддержки',
        );

        const checkinDateByPage =
            await hotelsBookPage.bookSearchParams.getCheckinDate();
        const checkinDateByParams = moment(testOfferParams.checkinDate).format(
            'D MMMM, ddd',
        );

        assert.equal(
            checkinDateByPage,
            checkinDateByParams,
            `Не совпадает параметр checkinDate.
                checkinDateByPage: ${checkinDateByPage}.
                checkinDateByParams: ${checkinDateByParams}.
            `,
        );

        const checkoutDateByPage =
            await hotelsBookPage.bookSearchParams.getCheckoutDate();
        const checkoutDateByParams = moment(
            testOfferParams.checkoutDate,
        ).format('D MMMM, ddd');

        assert.equal(
            checkoutDateByPage,
            checkoutDateByParams,
            `Не совпадает параметр checkoutDate.
                checkoutDateByPage: ${checkoutDateByPage}.
                checkoutDateByParams: ${checkoutDateByParams}.
            `,
        );

        const guestsByPage = await hotelsBookPage.bookSearchParams.getGuests();
        const guestsByParams = '1 взрослый';

        assert.equal(
            guestsByPage,
            guestsByParams,
            `Не совпадает параметр guests.
                guestsByPage: ${guestsByPage}.
                guestsByParams: ${guestsByParams}.
            `,
        );

        const offerNameByPage = await hotelsBookPage.offerName.getText();
        const offerNameByParams = testOfferParams.offerName;

        assert.equal(
            offerNameByPage,
            offerNameByParams,
            `Не совпадает параметр offerName.
                offerNameByPage: ${offerNameByPage}.
                offerNameByParams: ${offerNameByParams}.
            `,
        );

        const [
            cancellationTriggerDateAndDescriptionByPage,
            cancellationTriggerTimeByPage,
        ] = (await hotelsBookPage.cancellationInfo.getTriggerText()).split(
            ', ',
        );
        const cancellationTriggerByParams = `Бесплатная отмена без штрафа до ${moment(
            testOfferParams.checkinDate,
        ).format(dateFormats.HUMAN)}`;

        assert.equal(
            cancellationTriggerDateAndDescriptionByPage,
            cancellationTriggerByParams,
            `Не совпадает параметр cancellationTrigger.
                cancellationTriggerByPage: ${cancellationTriggerDateAndDescriptionByPage}.
                cancellationTriggerByParams: ${cancellationTriggerByParams}.
            `,
        );

        assert.match(
            cancellationTriggerTimeByPage,
            /\d\d:\d\d/,
            'Указано время отмены брони',
        );

        const totalGuestFieldsByPage = (
            await hotelsBookPage.bookForm.getGuestFields()
        ).length;
        const totalGuestFieldsByParams = testOfferParams.occupancy;

        assert.equal(
            totalGuestFieldsByPage,
            totalGuestFieldsByParams,
            `Не совпадает число полей с гостями.
                totalGuestFieldsByPage: ${totalGuestFieldsByPage}.
                totalGuestFieldsByParams: ${totalGuestFieldsByParams}.
            `,
        );

        assert.isTrue(
            await hotelsBookPage.bookForm.checkFirstAndLastNameFields(),
            'Не отобразились поля ввода имени и фамилии гостя',
        );

        assert.isTrue(
            await hotelsBookPage.bookForm.checkEmailField(),
            'Не отобразилось поле ввода email гостя',
        );

        assert.isTrue(
            await hotelsBookPage.bookForm.checkPhoneField(),
            'Не отобразилось поле ввода телефона гостя',
        );

        const totalPriceByPage =
            await hotelsBookPage.priceInfo.totalPrice.getText();
        const totalPriceByParams = `${formatInteger(
            testOfferParams.priceAmount,
        )}\u2006₽`;

        assert.equal(
            totalPriceByPage,
            totalPriceByParams,
            `Не совпадает итоговая цена оффера.
                totalPriceByPage: ${totalPriceByPage}.
                totalPriceByParams: ${totalPriceByParams}.
            `,
        );
    });
});
