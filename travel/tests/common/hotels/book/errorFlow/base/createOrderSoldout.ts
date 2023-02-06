import {assert} from 'chai';

import {SECOND} from 'helpers/constants/dates';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';
import {IExpectedValues} from './types';

import {TestHotelPage} from 'helpers/project/hotels/pages/HotelPage/TestHotelPage';
import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import {TestHotelsBookErrorPage} from 'helpers/project/hotels/pages/TestHotelsBookErrorPage/TestHotelsBookErrorPage';
import {
    maleGuests,
    femaleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';

export async function createOrderSoldout(
    browser: WebdriverIO.Browser,
    testOfferParams: IBookOfferRequestParams,
    expectedValues: IExpectedValues,
): Promise<void> {
    const bookPage = new TestHotelsBookPage(browser);

    // - do: Перейти на страницу брони по сгенерированному урлу
    await bookPage.goToPage(testOfferParams);
    await bookPage.bookStatusProvider.isOfferFetched(30000);

    // - do: Заполнить данные гостей и контактные данные валидными значениями
    await bookPage.bookForm.fillForm(
        [...maleGuests, ...femaleGuests],
        contacts,
    );

    // - do: Кликнуть по Оплатить
    await bookPage.bookForm.submit();

    const bookErrorPage = new TestHotelsBookErrorPage(browser);

    await bookErrorPage.bookHotelInfo.waitForVisible(20 * SECOND);

    // | Через какое-то время произошел переход на страницу с сообщением Все номера такого типа уже забронированы
    const errorTitle = await bookErrorPage.errorTitle.getText();

    assert.equal(
        errorTitle,
        'Все номера такого типа уже забронированы',
        'Должен быть заголовок "Все номера такого типа уже забронированы"',
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

    // | В самом низу блока об отеле расположены две кнопки
    // | Начать новый поиск и Найти номера в отеле
    assert(
        await bookErrorPage.searchHotelsButton.isVisible(),
        'Должна быть кнопка: Начать новый поиск',
    );
    assert(
        await bookErrorPage.searchHotelButton.isVisible(),
        'Должна быть кнопка: Найти номера в отеле',
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
    // - assert: Произошел переход на страницу бронирования (особенность Тестового Контекста)
    await bookPage.bookStatusProvider.isOfferFetched(30 * SECOND);
    // - do: Кликнуть по Оплатить
    await bookPage.bookForm.submit();
    // - assert: Произошел переход на страницу с сообщением Все номера такого типа уже забронированы
    // - do: Кликнуть по Найти номера в отеле
    await bookErrorPage.searchHotelButton.waitForVisible(20 * SECOND);
    await bookErrorPage.searchHotelButton.click();

    // - assert: Открылась страница отеля Вега Измайлово с теми же параметрами дат и гостей
    const hotelPage = new TestHotelPage(browser);

    await hotelPage.state.waitForLoadingFinished(30 * SECOND);
}
