import {assert} from 'chai';

import {MINUTE, SECOND} from 'helpers/constants/dates';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';
import {IExpectedValues} from './types';

import {TestHotelsBookErrorPage} from 'helpers/project/hotels/pages/TestHotelsBookErrorPage/TestHotelsBookErrorPage';
import {
    maleGuests,
    femaleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import {TestHotelPage} from 'helpers/project/hotels/pages/HotelPage/TestHotelPage';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';

export async function confirmationNotFound(
    browser: WebdriverIO.Browser,
    testOfferParams: IBookOfferRequestParams,
    expectedValues: IExpectedValues,
): Promise<void> {
    const app = new TestHotelsBookApp(browser);

    const {hotelsBookPage} = app;

    // - do: Перейти на страницу брони по сгенерированному урлу
    await hotelsBookPage.goToPage(testOfferParams);
    // - assert: Открылась страница бронирования
    await hotelsBookPage.bookStatusProvider.isOfferFetched();

    await app.paymentTestContextHelper.setPaymentTestContext();

    // - do: Заполнить данные гостей и контактные данные валидными значениями
    await hotelsBookPage.bookForm.fillForm(
        [...maleGuests, ...femaleGuests],
        contacts,
    );
    // - do: Кликнуть по Оплатить
    await hotelsBookPage.bookForm.submit();

    // - assert: Вместо блока ввода данных карты появился лоадер с текстом “Платеж производится. Пожалуйста, подождите”
    // | Далее на всю страницу появился лоадер с текстом “Оплата заказа Оплачиваем заказ и готовим выписку”, все элементы на странице задизейблены
    const bookErrorPage = new TestHotelsBookErrorPage(browser);

    await bookErrorPage.errorSectionTitle.waitForVisible(3 * MINUTE);

    // | Далее произошел переход на страницу ошибки при бронировании
    // | Заголовок ошибки Забронировать номер не получилось. Деньги вернутся в течение часа
    const errorSectionTitle = await bookErrorPage.errorSectionTitle.getText();

    assert.equal(
        errorSectionTitle,
        'Забронировать номер не получилось. Деньги вернутся в течение часа',
        'Должен быть заголовок "Забронировать номер не получилось. Деньги вернутся в течение часа"',
    );

    // | Ниже информация для пользователя о том, куда обратиться
    // | Ссылка на поддержку https://yandex.ru/support/travel/troubleshooting.html
    const supportLinkHref = await bookErrorPage.supportLink.getAttribute(
        'href',
    );

    assert.equal(
        supportLinkHref,
        'https://yandex.ru/support/travel/troubleshooting.html',
        'Должна быть правильная ссылка на поддержу',
    );

    // | Указан номер заказа в формате YA-XXXX-XXXX-XXXX
    const errorOrderId = await bookErrorPage.errorOrderId.getText();
    const regexOrderId = /^YA-\d{4}-\d{4}-\d{4}$/;

    assert(
        regexOrderId.test(errorOrderId),
        `Номер заказа ${errorOrderId} должен быть в формате: YA-XXXX-XXXX-XXXX`,
    );

    // | Ниже отображается информация об отеле
    // | Название - Вега Измайлово
    const hotelNameWithStars =
        await bookErrorPage.bookHotelInfo.hotelNameLink.getText();

    assert(
        hotelNameWithStars.startsWith(expectedValues.hotelName),
        `Имя гостинницы на BookErrorPage должно быть ${expectedValues.hotelName}`,
    );

    // | Адрес и рейтинг
    assert(
        await bookErrorPage.bookHotelInfo.addressAndRating.isVisible(),
        'Должен быть Адрес и рейтинг',
    );

    // | Даты заезда и выезда - завтра и послезавтра
    const checkinDate =
        await bookErrorPage.bookHotelInfo.bookSearchParams.getCheckinDate();

    assert.equal(
        checkinDate,
        expectedValues.checkinDate,
        'Дата заезда на BookErrorPage должна быть завтра',
    );

    const checkoutDate =
        await bookErrorPage.bookHotelInfo.bookSearchParams.getCheckoutDate();

    assert.equal(
        checkoutDate,
        expectedValues.checkoutDate,
        'Дата выезда на BookErrorPage должна быть послезавтра',
    );

    // | Кол-во гостей - 2
    const guests =
        await bookErrorPage.bookHotelInfo.bookSearchParams.getGuests();

    assert.equal(
        guests,
        expectedValues.guests,
        `Кол-во гостей должно быть: ${expectedValues.guests}`,
    );

    // | Название оффера - test
    const offerName = await bookErrorPage.bookHotelInfo.offerName.getText();

    assert.equal(
        offerName,
        testOfferParams.offerName,
        `Название оффера должно быть: "${testOfferParams.offerName}"`,
    );

    // | Информация про питание
    assert(
        await bookErrorPage.bookHotelInfo.mealInfo.isVisible(),
        'Должна быть Информация про питание',
    );

    // | Информация про кровати
    if (expectedValues.canCheckBedGroups) {
        assert(
            await bookErrorPage.bookHotelInfo.bedsGroups.isVisible(),
            'Должна быть Информация про кровати',
        );
    }

    // | Справа от названия отеля есть фото отеля
    assert(
        await bookErrorPage.bookHotelInfo.hotelImage.isVisible(),
        'Должно быть Фото отеля',
    );

    // | Под фото название отеля от партнера и ссылка Описание отеля от партнера
    const partnerDescriptionLink = await bookErrorPage.bookHotelInfo
        .partnerDescriptionLink;

    assert(
        await partnerDescriptionLink.isVisible(),
        'Должна быть ссылка Описание отеля от партнера',
    );

    // - do: Кликнуть по ссылке Описание отеля от партнера
    await partnerDescriptionLink.click();
    // - assert: Открылся модал
    await bookErrorPage.bookPartnerHotelInfo.modal.isVisible();

    // | Модал содержит название отеля, фото, карту и описание от партнера
    assert(
        await bookErrorPage.bookPartnerHotelInfo.hotelName.isVisible(),
        'Должно быть: название отеля',
    );

    if (expectedValues.canCheckPartnerImages) {
        assert(
            await bookErrorPage.bookPartnerHotelInfo.imagesCarousel.isVisible(),
            'Должно быть: карусель фото',
        );
    }

    assert(
        await bookErrorPage.bookPartnerHotelInfo.mapContainer.isVisible(),
        'Должно быть: карта',
    );

    if (expectedValues.canCheckPartnerDescriptions) {
        assert(
            await bookErrorPage.bookPartnerHotelInfo.hotelDescriptions.isVisible(),
            'Должно быть: описание от партнера',
        );
    }

    // - do: Кликнуть по крестику над модалом
    await bookErrorPage.bookPartnerHotelInfo.modal.closeButton.click();
    // - assert: Модал закрылся
    await bookErrorPage.bookPartnerHotelInfo.modal.waitForHidden();

    // - do: Кликнуть по кнопке Начать новый поиск
    await bookErrorPage.searchHotelsButton.click();

    // - assert: Произошел переход на выдачу отелей из гео-региона бронируемого отеля
    const searchPath = '/hotels/search';

    assert(
        new RegExp(searchPath).test(await browser.getUrl()),
        `Должен быть путь в урле: ${searchPath}`,
    );

    // - do: Вернуться на предыдущую страницу
    browser.back();
    // - do: Кликнуть по Найти номера в отеле
    await bookErrorPage.searchHotelButton.waitForVisible(20 * SECOND);
    await bookErrorPage.searchHotelButton.click();

    // - assert: Открылась страница отеля Вега Измайлово с теми же параметрами дат и гостей
    const hotelPage = new TestHotelPage(browser);

    await hotelPage.state.waitForLoadingFinished(30 * SECOND);
}
