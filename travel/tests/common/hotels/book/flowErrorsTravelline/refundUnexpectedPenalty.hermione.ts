import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import {MINUTE} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import {
    maleGuests,
    femaleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('(ТЛ) Обработка ошибки при возврате', async function () {
        const tomorrow = moment().add(1, 'days');
        const dayAfterTomorrow = moment().add(2, 'days');

        const testOfferParams = getTestOfferParams({
            occupancy: '2',
            cancellation: 'CR_PARTIALLY_REFUNDABLE',
            offerName: 'test',
            checkinDate: tomorrow.format(dateFormats.ROBOT),
            checkoutDate: dayAfterTomorrow.format(dateFormats.ROBOT),
            priceAmount: 1000,
            partiallyRefundRate: 50,
            priceMismatchRate: 2,
            hotelDataLookupOutcome: 'HO_REAL',
            refundOutcome: 'RF_UNEXPECTED_PENALTY',
        });

        // - do: Забронировать заказ по ссылке из описания
        const app = new TestHotelsBookApp(this.browser);

        const {hotelsBookPage, hotelsHappyPage, hotelsPaymentPage} = app;

        await hotelsBookPage.goToPage(testOfferParams);
        // Открылась страница бронирования
        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        await app.paymentTestContextHelper.setPaymentTestContext();

        // Заполнить данные гостей и контактные данные валидными значениями
        await hotelsBookPage.bookForm.fillForm(
            [...maleGuests, ...femaleGuests],
            contacts,
        );
        // Кликнуть по Оплатить
        await hotelsBookPage.bookForm.submit();

        await hotelsPaymentPage.waitUntilLoaded();

        // - assert: Произошел переход на хэппи пейдж
        await hotelsHappyPage.waitUntilLoaded();
        await hotelsHappyPage.orderActions.detailsLink.click();

        const orderPage = new TestOrderHotels(this.browser);

        await orderPage.loader.waitUntilLoaded();

        // - do: Кликнуть по кнопке Отменить бронирование
        await orderPage.hotelActions.actions.cancelButton.click();

        // - do: В раскрывшемся попапе кликнуть Отменить бронирование
        await orderPage.cancelOrderModal.isVisible();
        await orderPage.cancelOrderModal.buttonSubmitClick();

        // - assert: Появился лоадер с текстом “Загрузка заказа Загружаем подробную информацию о заказе”
        // | Далее открылась страница ошибки при возврате
        const cancelledOrderPage = new TestOrderHotels(this.browser);

        await cancelledOrderPage.loader.waitUntilLoaded();

        // | Заголовок ошибки Не получилось отменить бронирование
        const expectedErrorSectionTitle = 'Не получилось отменить бронирование';

        assert.equal(
            await cancelledOrderPage.error.title.getText(),
            expectedErrorSectionTitle,
            `Должно быть сообщение об ошибке: "${expectedErrorSectionTitle}"`,
        );

        // | Далее указан номер поддержки и номер заказа в формате YA-XXXX-XXXX-XXXX
        const errorOrderId = await cancelledOrderPage.error.orderId.getText();
        const regexOrderId = /^YA-\d{4}-\d{4}-\d{4}$/;

        assert(
            regexOrderId.test(errorOrderId),
            'Должен быть номер заказа в правильном формате',
        );

        // | Ниже отображается информация об отеле
        // | Название - Вега Измайлово
        const hotelNameWithStars =
            await cancelledOrderPage.mainInfo.hotelName.getText();
        const expectedHotelName = 'Вега Измайлово';

        assert(
            hotelNameWithStars.startsWith(expectedHotelName),
            `Имя гостинницы на BookErrorPage должно быть ${expectedHotelName}`,
        );

        // | Адрес
        assert(
            await cancelledOrderPage.mainInfo.hotelAddress.isVisible(),
            'Должен быть Адрес',
        );

        // | Даты заезда и выезда - завтра и послезавтра
        const checkinDate =
            await cancelledOrderPage.mainInfo.checkinCheckoutDates.checkoutDate.getText();

        assert.equal(
            checkinDate,
            tomorrow.format(dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY),
            'Дата заезда на BookErrorPage должна быть завтра',
        );

        const checkoutDate =
            await cancelledOrderPage.mainInfo.checkinCheckoutDates.checkoutDate.getText();

        assert.equal(
            checkoutDate,
            dayAfterTomorrow.format(dateFormats.HUMAN_DATE_WITH_SHORT_WEEKDAY),
            'Дата выезда на BookErrorPage должна быть послезавтра',
        );

        // | Кол-во гостей - 2
        const expectedGuests = 2;

        assert.equal(
            await cancelledOrderPage.guests.guests.count(),
            expectedGuests,
            `Кол-во гостей должно быть: ${expectedGuests}`,
        );

        // | Название оффера - test
        const offerName = await cancelledOrderPage.hotelInfo.roomName.getText();

        assert.equal(
            offerName,
            testOfferParams.offerName,
            `Название оффера должно быть: "${testOfferParams.offerName}"`,
        );

        // | Информация про питание
        assert(
            await cancelledOrderPage.hotelInfo.mealInfo.isVisible(),
            'Должна быть Информация про питание',
        );

        // | Информация про кровати
        assert(
            await cancelledOrderPage.hotelInfo.bedGroups.isVisible(),
            'Должна быть Информация про кровати',
        );

        // | Справа от названия отеля есть фото отеля
        assert(
            await cancelledOrderPage.hotelInfo.images.isVisible(),
            'Должно быть Фото отеля',
        );
    });
});
