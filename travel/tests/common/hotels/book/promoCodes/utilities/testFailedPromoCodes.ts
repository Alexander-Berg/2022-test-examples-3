import {assert} from 'chai';

import {SECOND} from 'helpers/constants/dates';

import {IBookOfferRequestParams} from 'helpers/project/hotels/types/IBookOffer';

import {TestHotelsBookPage} from 'helpers/project/hotels/pages/TestHotelsBookPage/TestHotelsBookPage';
import {maleGuests} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';

interface IAssertValues {
    promoCode: string;
    errorText: string;
    originalPriceAmount?: number;
}

async function testFailedPromoCodesModal(
    hotelsBookPage: TestHotelsBookPage,
    assertValues: IAssertValues,
): Promise<void> {
    const hotelPagePromoCodes = hotelsBookPage.priceInfo.promoCodes;
    const {promoCode, errorText, originalPriceAmount = 0} = assertValues;

    // - do: В инпут написать промокод "SUCCESS_ALREADY_APPLIED", нажать применить
    await hotelPagePromoCodes.applyPromoCode(promoCode);

    const discountAmount =
        await hotelPagePromoCodes.discountPrice.getPriceValue();
    const discountedAmount =
        await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

    // - assert: В блоке с промокодом появилась сумма скидки -200₽
    assert.equal(
        await hotelPagePromoCodes.discountPrice.getText(),
        '− 200 ₽',
        'В корзинке должна быть указана сумма промокода -200₽',
    );
    // - assert: Появилась ссылка "Отменить"
    assert(
        await hotelPagePromoCodes.resetLinkButton.isVisible(),
        'Кнопка отмены промокода должна быть отображена после применения промокода',
    );
    // - assert: Сумма заказа стала на 200р меньше, чем была в п.3
    assert.equal(
        originalPriceAmount - discountAmount,
        discountedAmount,
        'Цена после примения промокода должна быть равной цене до применения скидки минус размер скидки',
    );

    // - do: Заполнить информацию о гостях и контактную информацию любыми валидными данными.
    await hotelsBookPage.bookForm.fillForm(maleGuests, contacts);
    // - do: Нажать оплатить.
    await hotelsBookPage.bookForm.submit();

    // - assert: Появился попап с текстом ошибки
    assert(
        await hotelsBookPage.reservedWithRestrictionsModal.isVisible(
            30 * SECOND,
        ),
        'Должен появиться модал ошибки создания заказа',
    );
    assert.equal(
        await hotelsBookPage.reservedWithRestrictionsModal.text.getText(),
        errorText,
        `Текст в модале ошибки должен соотвествовать тексту: ${errorText}`,
    );
    // - assert: В попапе две кнопки - "Изменить промокод", "Оплатить"
    assert.equal(
        await hotelsBookPage.reservedWithRestrictionsModal.secondaryActionButton.getText(),
        'Изменить промокод',
        'Текст кнопки отмены в модале ошибки должен соотвествовать: "Изменить промокод"',
    );
    assert.equal(
        await hotelsBookPage.reservedWithRestrictionsModal.primaryActionButton.getText(),
        'Оплатить',
        'Текст кнопки продолжения в модале ошибки должен соотвествовать: "Оплатить"',
    );
    // - assert: В попапе две цены - без скидки и со скидкой
    assert.equal(
        await hotelsBookPage.reservedWithRestrictionsModal.price.getPriceValue(),
        originalPriceAmount,
        'Значение оригинальной цены в модале должно соответствовать оригинальной цене бронирования',
    );
    assert.equal(
        await hotelsBookPage.reservedWithRestrictionsModal.lastPrice.getPriceValue(),
        discountedAmount,
        'Значение цены со скидкой в модале должно соответствовать цене со скидкой из корзинки',
    );
}

export async function testFailedPromoCodes(
    browser: WebdriverIO.Browser,
    offerParams: IBookOfferRequestParams,
    assertValues: IAssertValues,
): Promise<void> {
    const app = new TestHotelsBookApp(browser);

    const {hotelsBookPage, hotelsHappyPage} = app;

    const hotelPagePromoCodes = hotelsBookPage.priceInfo.promoCodes;

    // - do: Перейти на страницу бронирования для любого отеля, на любые даты
    await hotelsBookPage.goToPage(offerParams);
    // - assert: Открылась страница бронирования.
    await hotelsBookPage.bookStatusProvider.isOfferFetched(30 * SECOND);

    await app.paymentTestContextHelper.setPaymentTestContext();

    await hotelsBookPage.priceInfo.promoCodes.checkBox.scrollIntoView();
    // - assert: В корзинке есть галка "У меня есть промокод", по умолчанию выключена. Поле инпут для промокода и кнопка "Применить" не показывается.
    await hotelPagePromoCodes.testInitialPromoCodesState();
    // - do: Включить галку "У меня есть промокод"
    await hotelPagePromoCodes.checkBox.click();
    // - assert: Появилось Поле инпут для промокода и кнопка "Применить"
    await hotelPagePromoCodes.testActivePromoCodesState();

    // - do: Запомнить итоговую сумму заказа
    const originalPriceAmount =
        await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

    await testFailedPromoCodesModal(hotelsBookPage, {
        ...assertValues,
        originalPriceAmount,
    });

    // - do: Нажать кнопку "Отменить заказ"
    await hotelsBookPage.reservedWithRestrictionsModal.secondaryActionButton.click();
    // - assert: Открылась страница бронирования.
    await hotelsBookPage.bookStatusProvider.isOfferFetched(30 * SECOND);
    // - assert: Галка "У меня есть промокод" отжата
    // - assert: Поле инпут для промокода и кнопка "Применить" не показывается.
    await hotelPagePromoCodes.testInitialPromoCodesState();
    // - assert: Сумма без учета скидки(из п.3)
    assert.equal(
        await hotelsBookPage.priceInfo.totalPrice.getPriceValue(),
        originalPriceAmount,
        'Значение цены должно соотвествовать изначальному для данного бронирования без промокода',
    );

    await hotelPagePromoCodes.checkBox.click();

    await testFailedPromoCodesModal(hotelsBookPage, {
        ...assertValues,
        originalPriceAmount,
    });

    // - do: Нажать кнопку "Оплатить"
    await hotelsBookPage.reservedWithRestrictionsModal.primaryActionButton.click();

    // - assert: Открылась страница успешного заказа
    await hotelsHappyPage.loader.waitUntilLoaded(30 * SECOND);

    await hotelsHappyPage.orderActions.detailsLink.click();

    const orderPage = new TestOrderHotels(browser);

    await orderPage.loader.waitUntilLoaded();

    // - assert: Сумма "Оплачено" без скидки.
    assert.equal(
        await orderPage.mainInfo.orderHotelsPrice.totalPrice.totalPrice.getPriceValue(),
        originalPriceAmount,
        'Цена на HappyPage должна совпадать с оригинальной ценой бронирования без промокода',
    );

    const {receiptsAndDocs} = orderPage.mainInfo.orderHotelsPrice;

    await receiptsAndDocs.openDetails();

    // - assert: Лейбла с промокодом нет
    assert.isTrue(
        (await receiptsAndDocs.detailsModal.details.promoCodes.count()) === 0,
        'Информация о скидке по промокоду должна отсутствовать на странице заказа в ЛК',
    );
}
