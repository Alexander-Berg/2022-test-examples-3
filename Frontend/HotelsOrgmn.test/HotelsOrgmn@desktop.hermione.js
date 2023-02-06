'use strict';

const { getReactFormDates } = require('../../../../../../features/companies/helpers.js');
const PO = require('./HotelsOrgmn.page-object')('desktop');

// Использую цены от партнеров как надежный идентификатор набора данных
// При изменении фильтра дат список может не измениться

const getPrices = async function() {
    const texts = await this.browser.yaGetTexts(PO.hotelsOrgmn.hotelCard.priceValues());
    return texts.join(', ');
};

const waitForPricesChange = function(pricesBefore) {
    return this.browser.yaWaitUntil(
        `Cписок цен ${pricesBefore} не изменился`,
        async () => {
            const pricesAfter = await getPrices.call(this);

            return pricesAfter && (pricesBefore !== pricesAfter);
            // дефолтного таймаута порой не хватает
        }, 50000,
    );
};

const loadPage = async function() {
    await this.browser.yaOpenSerp(
        {
            text: 'отели в москве',
            data_filter: 'companies',
            // выключаем карусель
            rearr: 'scheme_Local/OrgWizard/MinCarouselSize=1000',
        },
        PO.hotelsOrgmn(),
    );
};

specs({
    feature: 'Hotels / Колдунщик многих организаций',
    type: 'Стандартный вид',
}, function() {
    beforeEach(loadPage);

    it('Внешний вид', async function() {
        await this.browser.setViewportSize({ width: 1280, height: 600 });
        // после ресайза карты обновляются предложения, поэтому нужно перезагрузить страницу
        // для стабильных результатов на скриншотах
        await loadPage.call(this);
        await this.browser.assertView('orgmn_hotels', PO.hotelsOrgmn(), {
            invisibleElements: PO.hotelsOrgmn.map.content() + ' canvas',
        });

        await this.browser.setViewportSize({ width: 1100, height: 600 });
        await loadPage.call(this);
        await this.browser.assertView('orgmn_hotels_plain', PO.hotelsOrgmn(), {
            invisibleElements: PO.hotelsOrgmn.map.content() + ' canvas',
        });

        await this.browser.setViewportSize({ width: 900, height: 600 });
        await loadPage.call(this);
        await this.browser.assertView('orgmn_hotels_narrow', PO.hotelsOrgmn(), {
            invisibleElements: PO.hotelsOrgmn.map.content() + ' canvas',
        });

        await this.browser.setViewportSize({ width: 1600, height: 600 });
        await loadPage.call(this);
        await this.browser.assertView('orgmn_hotels_wide', PO.hotelsOrgmn(), {
            invisibleElements: PO.hotelsOrgmn.map.content() + ' canvas',
        });
    });

    it('Клик в ссылку в футере ведет на условия подключения', async function() {
        await this.browser.yaShouldBeVisible(PO.hotelsOrgmn.footerLink(), 'Ссылка в футере колдунщика не найдена');
        await this.browser.yaCheckLink2({
            selector: PO.hotelsOrgmn.footerLink(),
            url: {
                href: 'https://yandex.ru/support/webmaster/search-appearance/hotel-list.html',
            },
            baobab: {
                path: '$result/hotels-orgmn/connection-link',
            },
            message: 'Неправильная ссылка на условия подключения',
        });
    });

    it('Клик в отзывы открывает попап на вкладке отзывы', async function() {
        await this.browser.click(PO.hotelsOrgmn.hotelCard.reviews());
        await this.browser.yaWaitForVisible(PO.popup.reviews(), 'Отзывы не загрузились');
    });

    it('Клик по кнопке \"На большую карту\" ведет на Яндекс.Карты', async function() {
        await this.browser.yaShouldBeVisible(PO.hotelsOrgmn.map.mapLinks.goToMap(), 'Ссылка на большую карту не найдена');
        await this.browser.yaCheckLink2({
            selector: PO.hotelsOrgmn.map.mapLinks.goToMap(),
            url: {
                href: 'https://yandex.ru/maps/',
                ignore: ['query'],
            },
            baobab: {
                path: '$result/hotels-orgmn/MapTypeDynamic/goto-map',
            },
            message: 'Неправильная ссылка на большую карту',
        });
    });

    it('Ховер по карточке подсвечивает пин', async function() {
        const getActivePinIndex = ({ activeIconLayout, pin }) => $(activeIconLayout).closest(pin).index();
        const selectors = {
            activeIconLayout: PO.hotelsOrgmn.map.pin.activeIconLayout(),
            pin: PO.hotelsOrgmn.map.pin(),
        };

        await this.browser.moveToObject(PO.hotelsOrgmn.firstCard());

        const firstCardPinIndex = await this.browser.execute(getActivePinIndex, selectors);

        await this.browser.moveToObject(PO.hotelsOrgmn.secondCard());

        const secondCardPinIndex = await this.browser.execute(getActivePinIndex, selectors);

        assert(firstCardPinIndex !== secondCardPinIndex, 'Ховер по другой карточке не изменил подсвечиваемый пин');
    });

    it('Клик по пину скроллит к выбранной карточке', async function() {
        const getCardOffset = selector => $(selector).offset();

        const cardOffsetBeforePinClick = await this.browser.execute(getCardOffset, PO.hotelsOrgmn.firstCard());

        // Порядок пинов противоположен порядку карточек
        await this.browser.yaMoveAndClick(PO.hotelsOrgmn.map.firstPin(), 0, 0);
        await this.browser.pause(200);

        const cardOffsetAfterPinClick = await this.browser.execute(getCardOffset, PO.hotelsOrgmn.firstCard());

        assert(cardOffsetBeforePinClick.left !== cardOffsetAfterPinClick.left, 'Нет подскролла после клика по пину');
    });

    it('Отели обновляются при изменении значений в форме', async function() {
        const form = PO.hotelsOrgmn.hotelListForm;

        let pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        // Изменяем диапазон дат посещения отеля
        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
        await this.browser.yaWaitForHidden(PO.datePickerPopup(), 'DatePicker не закрылся');

        // Проверяем изменения в списке
        await waitForPricesChange.call(this, pricesBefore);

        pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        // Изменяем фильтр услуг
        await this.browser.yaShouldBeVisible(form.servicesSelect());
        await this.browser.click(form.servicesSelect());
        await this.browser.click(form.servicesPopup.firstOption());

        // Проверяем изменения в списке
        await waitForPricesChange.call(this, pricesBefore);

        pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        // Увеличиваем количество гостей
        await this.browser.click(form.guestsDropdown());
        await this.browser.yaWaitForVisible(PO.guestsDropdownPopup(), 'Не открылся попап для выбора гостей');
        await this.browser.click(PO.guestsDropdownPopup.Adults.PlusButton());
        await this.browser.click(form.guestsDropdown());
        await this.browser.yaWaitForHidden(PO.guestsDropdownPopup());

        // Проверяем изменения в списке
        await waitForPricesChange.call(this, pricesBefore);
    });

    it('Отели обновляются при перетаскивании карты', async function() {
        const pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        const mapContent = this.browser.$(PO.hotelsOrgmn.map.content());
        await mapContent.dragAndDrop({ x: 100, y: 100 });
        await waitForPricesChange.call(this, pricesBefore);
    });

    it('Отели обновляются при зуме карты', async function() {
        const pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        await this.browser.yaWaitForVisible(PO.hotelsOrgmn.map.zoominButton(), 'Кнопка зума карты не появилась');
        await this.browser.click(PO.hotelsOrgmn.map.zoominButton());

        await waitForPricesChange.call(this, pricesBefore);
    });

    it('Отели подгружаются при подскролле списка', async function() {
        const hotelsCards = await this.browser.$$(PO.hotelsOrgmn.hotelCard());
        const cardsCount = hotelsCards.length;
        assert.isOk(cardsCount);
        await this.browser.yaScrollContainer(PO.hotelsOrgmn.hotelCardsContainer(), 10000);

        await this.browser.yaWaitUntil('Карточки не подгрузились', async () => {
            const newHotelsCards = await this.browser.$$(PO.hotelsOrgmn.hotelCard());

            return newHotelsCards.length > cardsCount;
        });
    });

    it('Даты из формы пробрасываются в попап', async function() {
        const form = PO.hotelsOrgmn.hotelListForm;

        let pricesBefore = await getPrices.call(this);
        assert.isOk(pricesBefore);

        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
        await this.browser.yaWaitForHidden(PO.datePickerPopup(), 'DatePicker не закрылся');

        await waitForPricesChange.call(this, pricesBefore);

        const datesSerp = await this.browser.getAttribute(form.datePicker.input(), 'value');

        await this.browser.yaMoveAndClick(PO.hotelsOrgmn.hotelCard(), 5, 5);

        await this.browser.yaWaitForVisible(PO.popup.content(), 'Попап не загрузился');

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '$result/hotels-orgmn/hotel-list/item/link',
        });

        await this.browser.setBaseMetrics(metrics => metrics.concat([
            'feature.web.wizards_common.wizard_travel_companies_parallel.total.shows',
            'feature.web.wizards_common.wizard_travel_companies_parallel.total.external_clicks',
            'feature.web.wizards_common.wizard_travel_companies_parallel.total.dynamic_clicks',
        ]));
        const { toDate, fromDate } = await getReactFormDates(this.browser, PO.popup.hotelsFilters());
        const isoFromDate = fromDate.split('.').reverse().join('-');
        const isoToDate = toDate.split('.').reverse().join('-');

        assert.strictEqual(isoFromDate + '/' + isoToDate, datesSerp);
    });
});
