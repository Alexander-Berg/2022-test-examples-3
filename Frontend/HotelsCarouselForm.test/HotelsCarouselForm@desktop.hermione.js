'use strict';

const { getReactFormDates } = require('../../../../../../features/companies/helpers');
const { popup: popupPo } = require('../../../Companies.test/Companies.page-object/index@desktop');

const PO = require('./HotelsCarouselForm.page-object').desktop;

const ISODateToString = isoDate => {
    const [y, m, d] = isoDate.split('-');
    return `${d}.${m}.${y}`;
};

specs({
    feature: 'Hotels / Карусель',
}, function() {
    function openHotelsCarouselForm(additional) {
        let options = {
            text: 'Отели в Алуште',
            srcskip: 'YABS_DISTR',
        };

        if (additional) {
            options = { ...options, ...additional };
        }

        return this.browser.yaOpenSerp(options, PO.companiesMapList());
    }

    it('Внешний вид', async function() {
        const { browser } = this;

        await openHotelsCarouselForm.call(this);

        await browser.yaWaitForVisible(PO.companiesMapList.hotelsFilters());

        await browser.assertView('list-view', PO.companiesMapList.hotelsFilters());

        await browser.click(PO.openMapBtn());

        await browser.assertView('map-view', PO.companiesMapList.hotelsFilters(), { hideElements: [PO.companiesMapList.map()] });
    });

    it('Внешний вид на узких экранах', async function() {
        const { browser } = this;

        await openHotelsCarouselForm.call(this);

        await browser.yaWaitForVisible(PO.companiesMapList.hotelsFilters());

        await this.browser.setViewportSize({ width: 1080, height: 1200 });

        await browser.assertView('list-view_1080', PO.companiesMapList.hotelsFilters());

        await this.browser.setViewportSize({ width: 940, height: 1200 });

        await browser.assertView('list-view_940', PO.companiesMapList.hotelsFilters());
    });

    it('Внешний вид с подскрольными рекомендациями', async function() {
        const { browser } = this;

        await openHotelsCarouselForm.call(this, { exp_flags: 'related-above-enable-for-testing=1' });

        await browser.yaWaitForVisible(PO.companiesMapList.hotelsFilters());

        await this.browser.yaScroll(401);
        await this.browser.yaScroll(0);
        await this.browser.yaWaitForVisible(this.PO.relatedAbove(), 'Переформулировки не появились');

        await this.browser.yaAssertViewExtended(
            'plain-with-related-above',
            this.PO.main(),
            {
                invisibleElements: [PO.companiesMapList.HotelsCarouselList.MapCard.image()],
                hideElements: [
                    this.PO.header(),
                    this.PO.serpNavigation(),
                    this.PO.mainContent(),
                ],
            },
        );

        await this.browser.click(PO.openMapBtn());
        await this.browser.yaWaitForVisible(PO.companiesMapList.map(), 'Не удалось перейти в режим карты');

        await this.browser.yaAssertViewExtended(
            'opened-with-related-above',
            this.PO.main(),
            {
                invisibleElements: [PO.companiesMapList.HotelsCarouselList.MapCard.image()],
                hideElements: [
                    this.PO.header(),
                    this.PO.serpNavigation(),
                    this.PO.mainContent(),
                ],
            },
        );
    });

    it('Взаимодействия с контролами в режиме списка', async function() {
        const form = PO.companiesMapList.hotelsFilters;

        await openHotelsCarouselForm.call(this);

        // Использую цены от партнеров как надежный идентификатор набора данных
        // При изменении фильтра дат список может не измениться
        const getPrices = () => this.browser.execute(function(selector) {
            return window.$(selector).text();
        }, PO.companiesMapList.HotelsCarouselList.HotelCard.PriceValues());

        const waitForPricesDifferent = pricesBefore => this.browser.yaWaitUntil(
            'Cписок не поменялся, ' + pricesBefore,
            async () => {
                const pricesAfter = await getPrices();

                return pricesAfter && (pricesBefore !== pricesAfter);
            },
            15000, // поиск отелей может длиться долго
        );

        let pricesBefore = await getPrices();
        assert.isOk(pricesBefore);

        // Изменяем диапазон дат посещения отеля
        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
        await this.browser.yaWaitForHidden(PO.datePickerPopup(), 'DatePicker не закрылся');

        // Проверяем изменения в списке
        await waitForPricesDifferent(pricesBefore);

        pricesBefore = await getPrices();
        assert.isOk(pricesBefore);

        // Увеличиваем количество гостей
        await this.browser.click(form.GuestsDropdown());
        await this.browser.yaWaitForVisible(PO.GuestsDropdownPopup(), 'Не открылся попап для выбора гостей');
        await this.browser.click(PO.GuestsDropdownPopup.Adults.PlusButton());
        await this.browser.click(form.GuestsDropdown());
        await this.browser.yaWaitForHidden(PO.GuestsDropdownPopup());

        // Проверяем изменения в списке
        await waitForPricesDifferent(pricesBefore);
    });

    it('Взаимодействия с контролами в режиме карты', async function() {
        const form = PO.companiesMapList.hotelsFilters;

        await openHotelsCarouselForm.call(this);

        const getPrices = async () => {
            return await this.browser.execute(function(selector) {
                return window.$(selector).text();
            }, PO.companiesMapList.map.map2.pinTitle());
        };

        // Открываем режим карты
        await this.browser.click(PO.openMapBtn());
        await this.browser.yaWaitForVisible(PO.companiesMapList.map(), 'Не удалось перейти в режим карты');

        let pricesBefore = await getPrices();
        assert.isOk(pricesBefore, 'Значения цен не прогрузились');

        // Меняем диапазон дат для посещения отеля
        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());
        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
        await this.browser.yaWaitForHidden(PO.datePickerPopup(), 'DatePicker не закрылся');
        await this.browser.yaWaitUntil(
            'После изменения дат цены не обновились',
            async () => pricesBefore !== await getPrices(),
            15000, // поиск отелей может длиться долго
        );

        pricesBefore = await getPrices();
        assert.isOk(pricesBefore, 'Значения новых цен не прогрузились');

        // Изменяем количество гостей
        await this.browser.click(form.GuestsDropdown());
        await this.browser.yaWaitForVisible(PO.GuestsDropdownPopup(), 'Не открылся попап для выбора гостей');
        await this.browser.click(PO.GuestsDropdownPopup.Adults.PlusButton());
        await this.browser.click(PO.GuestsDropdownPopup.Adults.PlusButton());
        await this.browser.click(form.GuestsDropdown());
        await this.browser.yaWaitForHidden(PO.GuestsDropdownPopup(), 'Попап для выбора гостей не закрылся');

        await this.browser.yaWaitUntil(
            'Ждем обновления цен',
            async () => pricesBefore !== await getPrices(),
        );
    });

    it('Взаимодействия между картой и списком', async function() {
        const form = PO.companiesMapList.hotelsFilters;

        await openHotelsCarouselForm.call(this);

        // Использую цены от партнеров как надежный идентификатор набора данных
        // При изменении фильтра дат список может не измениться
        const getPrices = () => this.browser.yaGetTexts(
            PO.companiesMapList.HotelsCarouselList.HotelCard.PriceValues(),
        );

        const waitForPricesDifferent = pricesBefore => this.browser.yaWaitUntil(
            'Cписок не поменялся, ' + pricesBefore,
            () => (this.browser.yaGetTexts(
                PO.companiesMapList.HotelsCarouselList.HotelCard.PriceValues(),
            )).toString() !== pricesBefore.toString(),
        );

        // Сохраняем показатели списка до манипуляций с картой
        const pricesBefore = await getPrices();

        // Открываем режим карты
        await this.browser.click(PO.openMapBtn());
        await this.browser.yaWaitForVisible(PO.companiesMapList.map(), 'Не удалось перейти в режим карты');

        // Изменяем количество гостей
        await this.browser.click(form.GuestsDropdown());
        await this.browser.yaWaitForVisible(PO.GuestsDropdownPopup(), 'Не открылся попап для выбора гостей');
        await this.browser.waitForBlinked(async () => {
            await this.browser.click(PO.GuestsDropdownPopup.Adults.PlusButton());
            await this.browser.click(form.GuestsDropdown());
            await this.browser.yaWaitForHidden(PO.GuestsDropdownPopup());
        }, PO.companiesMapList.progress(), 'Загрузка новых предложений после изменения количества гостей не произошла');

        // Возвращаемся в режим списка
        await this.browser.click(PO.companiesMapList.map.map2.closeButton());

        // Проверяем изменения в списке
        await waitForPricesDifferent(pricesBefore);
    });

    it('Взаимодействия между формой и всплывающим окном', async function() {
        const form = PO.companiesMapList.hotelsFilters;

        await openHotelsCarouselForm.call(this);

        // Меняем даты посещения отеля
        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');

        // Ждем окончания загрузки нового списка
        await this.browser.waitForBlinked(async () => {
            await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());
            await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
        }, PO.companiesMapList.progress(), 'Загрузка новых предложений после изменения даты не закончилась');

        await this.browser.yaWaitForHidden(PO.datePickerPopup(), 'DatePicker не закрылся');

        const datesOnSerp = await this.browser.getAttribute(form.datePicker.input(), 'value');

        // Смотрим на форму внутри всплывающего окна
        // В центре карточки могут оказаться отзывы, поэтому смещаем в левый верхний угол
        await this.browser.yaMoveAndClick(PO.companiesMapList.HotelsCarouselList.HotelCard.TopicLink(), 10, 10);
        await this.browser.yaWaitForVisible(popupPo.oneOrg.tabsPanes.about.hotelsReactForm());

        const filtersState = await getReactFormDates(this.browser, popupPo.oneOrg.tabsPanes.about.hotelsReactForm());

        const [dateFrom, dateTo] = datesOnSerp.split('/');
        assert.deepEqual(filtersState, { fromDate: ISODateToString(dateFrom), toDate: ISODateToString(dateTo) }, 'Значения в форме не обновились');
    });

    it('Сброс ценового фильтра после смены количества ночей', async function() {
        const form = PO.companiesMapList.hotelsFilters;

        await openHotelsCarouselForm.call(this);

        await this.browser.click(form.carouselPrice.range());

        // Меняем даты посещения отеля
        await this.browser.click(form.datePicker.control());
        await this.browser.yaWaitForVisible(PO.datePickerPopup(), 'DatePicker не открылся');

        await this.browser.click(PO.datePickerPopup.calendar.secondMonth.secondWeek.firstDay());

        const result = await this.browser.yaGetLastAjaxDataFast(
            'ag_dynamic',
            { field: 'url' },
            async () => {
                await this.browser.click(PO.datePickerPopup.calendar.secondMonth.thirdWeek.firstDay());
            },
        );

        assert.isFalse(result.includes('hotel_price_range'), 'Фильтр по цене не должен быть задан');
    });
});
