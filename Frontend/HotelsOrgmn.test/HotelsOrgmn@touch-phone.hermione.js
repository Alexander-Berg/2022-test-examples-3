'use strict';
const filtersHelpers = require('../../../../../../hermione/client-scripts/hotels-filters');
const PO = require('./HotelsOrgmn.page-object')('touch-phone');
const dateRangePO = {
    control: PO.hotelsOrgmn.form.datePicker(),
    input: PO.hotelsOrgmn.form.datePicker.input(),
    sideblockCalendar: PO.sideBlockCalendar(),
    day: PO.sideBlockCalendar.day(),
    save: PO.sideBlockCalendar.button(),
};

/** Возвращает цены в карточках на выдаче в виде строки */
const getPrices = async function() {
    const pricesArray = await this.browser.yaGetTexts(PO.hotelsOrgmn.list.offer.priceInfo());
    const prices = pricesArray.join();
    assert.isOk(prices);
    return prices;
};

/** Ждет, когда цены в карточках на выдаче изменятся. Возвращает новые цены в виде строки */
const waitForPricesChanged = async function(pricesBefore) {
    let prices;
    await this.browser.yaWaitUntil(
        'Цены не изменились',
        async () => {
            prices = await getPrices.call(this);
            assert.notEqual(prices, pricesBefore);
            return true;
        },
    );

    return prices;
};

specs({
    feature: 'Hotels / Колдунщик многих организаций',
    type: 'Стандартный вид',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'отели в москве',
            rearr: 'EntitySearch_off',
            data_filter: 'companies',
        }, PO.hotelsOrgmn());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.hotelsOrgmn());
    });

    it('Заголовок открывает список в оверлее', async function() {
        await this.browser.yaOpenOverlayAjax(
            PO.hotelsOrgmn.header.title(),
            this.PO.mapListSideBlock(),
            'Ошибка при открытии оверлея с картой отелей',
        );
        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/$main/$result/hotels-orgmn/title',
            behaviour: { type: 'dynamic' },
        });
        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.list.firstItem(), 'Не показался список в оверлее');
    });

    hermione.only.in('iphone', 'Тест не браузерозависимый');
    it('Ссылки в блоке extralinks', async function() {
        await this.browser.yaWaitForVisible(PO.hotelsOrgmn.header.extralinks());
        await this.browser.yaWaitUntil(
            'Не удалось открыть попап с экстралинками',
            async () => {
                await this.browser.click(PO.hotelsOrgmn.header.extralinks());
                return await this.browser.yaWaitForVisible(PO.extralinksPopup());
            },
        );

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/$main/$result/hotels-orgmn/extralinks',
        });

        await this.browser.click(PO.extralinksPopup.firstLink(), {
            path: '/$page/$main/$result/hotels-orgmn/extralinks/extralinks-popup/abuse',
        });
        await this.browser.yaWaitForVisible(this.PO.feedback());
    });

    it('Клик в карту на выдаче открывает карту в оверлее', async function() {
        this.browser.yaCheckBaobabCounter(
            PO.hotelsOrgmn.map.image(),
            {
                path: '/$page/$main/$result/hotels-orgmn/map/teaser',
                behaviour: { type: 'dynamic' },
            },
        );

        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.map());
        await this.browser.yaWaitUntilMapLoaded(this.PO.mapListSideBlock.map());
        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.map.pin(), 'Не показались пины на карте');

        await this.browser.setBaseMetrics(metrics => metrics.concat('web.total_dynamic_click_count'));
    });

    it('Клик в кнопку разворачивания карты на выдаче открывает карту в оверлее', async function() {
        this.browser.yaCheckBaobabCounter(
            PO.hotelsOrgmn.map.expand(),
            {
                path: '/$page/$main/$result/hotels-orgmn/map/open',
                behaviour: { type: 'dynamic' },
            },
        );

        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.map());
        await this.browser.yaWaitUntilMapLoaded(this.PO.mapListSideBlock.map());
        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.map.pin(), 'Не показались пины на карте');

        await this.browser.setBaseMetrics(metrics => metrics.concat('web.total_dynamic_click_count'));
    });

    it('Контрол выбора гостей перезагружает отели', async function() {
        let pricesBefore = await getPrices.call(this);

        await this.browser.yaShouldBeVisible(PO.hotelsOrgmn.form.guestPicker(), 'Выбора гостей нет в колдунщике');

        await this.browser.yaCheckBaobabCounter(
            PO.hotelsOrgmn.form.guestPicker(),
            {
                path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/guests/button',
                behaviour: { type: 'dynamic' },
            },
        );
        await this.browser.yaWaitForVisible(PO.guestPickerPopup(), 'Попап с выбором гостей не появился');

        // проверяем счетчики на + и -
        await this.browser.yaCheckBaobabCounter(
            PO.guestPickerPopup.Adults.PlusButton(),
            {
                path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/guests/plus',
                behaviour: { type: 'dynamic' },
            },
        );
        await this.browser.yaCheckBaobabCounter(
            PO.guestPickerPopup.Adults.MinusButton(),
            {
                path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/guests/minus',
                behaviour: { type: 'dynamic' },
            },
        );

        // увеличиваем количество взрослых
        await this.browser.click(PO.guestPickerPopup.Adults.PlusButton());

        // добавляем ребенка
        await this.browser.yaCheckBaobabCounter(
            PO.guestPickerPopup.AddChild.Select(),
            {
                path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/guests/add',
                behaviour: { type: 'dynamic' },
            },
        );
        await this.browser.click(PO.guestPickerPopup.AddChild.Select.Option());

        // сохраняем значения
        await this.browser.yaCheckBaobabCounter(
            PO.guestPickerPopup.Save(),
            {
                path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/guests/save',
                behaviour: { type: 'dynamic' },
            },
        );
        await this.browser.yaWaitForHidden(PO.guestPickerPopup(), 'Попап с выбором количества гостей не скрылся');

        await waitForPricesChanged.call(this, pricesBefore);

        // проверка метрик
        await this.browser.setBaseMetrics(metrics => metrics.concat('web.total_dynamic_click_count'));
    });

    it('Контрол выбора дат перезагружает отели', async function() {
        let pricesBefore = await getPrices.call(this);

        // меняем дефолтные даты
        let newDateRange;
        const requestUrl = await this.browser.yaGetLastAjaxDataFast(
            'ag_dynamic',
            { field: 'url' },
            async () => {
                // запоминаем установленные даты
                newDateRange = await this.browser.yaSetDateRange(2, 3, dateRangePO);
            },
        );

        assert.include(requestUrl, newDateRange.split('/')[0], 'Ajax запрос не содержит выбранной даты');

        await this.browser.yaWaitForHidden(PO.hotelsOrgmn.progress());

        await waitForPricesChanged.call(this, pricesBefore);

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/$main/$result/hotels-orgmn/hotel-list-form/single-dates-picker/button',
            behaviour: { type: 'dynamic' },
        });
    });

    it('Значения формы пробрасываются в оверлей', async function() {
        let pricesBefore = await getPrices.call(this);

        // меняем количество гостей
        await this.browser.click(PO.hotelsOrgmn.form.guestPicker());
        await this.browser.yaWaitForVisible(PO.guestPickerPopup(), 'Попап с выбором гостей не появился');
        await this.browser.click(PO.guestPickerPopup.Adults.PlusButton());
        await this.browser.click(PO.guestPickerPopup.Save());
        await this.browser.yaWaitForHidden(PO.guestPickerPopup(), 'Попап с выбором количества гостей не скрылся');
        pricesBefore = await waitForPricesChanged.call(this, pricesBefore);
        const [guestsValue] = await this.browser.yaGetAttributes(PO.hotelsOrgmn.form.guestPicker.input(), 'value');

        // меняем даты
        let dateRangeValue = await this.browser.yaSetDateRange(2, 3, dateRangePO);
        await waitForPricesChanged.call(this, pricesBefore);

        // открываем оверлей карты
        await this.browser.yaOpenOverlayAjax(
            PO.hotelsOrgmn.more(),
            this.PO.mapListSideBlock(),
            'Ошибка при открытии оверлея с картой отелей',
        );
        // запрос номеров может быть долгим
        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.list(), 'Список не загрузился');
        await this.browser.yaWaitForHidden(this.PO.mapListSideBlock.taskProgress(), 'Прогресс-бар должен скрыться');
        await this.browser.yaWaitForVisible(this.PO.mapListSideBlock.list.firstItem(), 'Карточки отелей не загрузились');
        await this.browser.click(this.PO.mapListSideBlock.list.firstItem());

        // проверка формы в оверлее отеля
        await this.browser.yaWaitForVisible(this.PO.bcardSideBlock(), 'Оверлей отеля не открылся');
        await this.browser.yaShouldBeVisible(this.PO.bcardSideBlock.hotelsFilters(), 'Форма выбора дат в оверлее отеля не показалась');
        const { Ages: sideblockGuestsValue, Date: sideblockDateValue } = await this.browser
            .execute(filtersHelpers.getState, this.PO.bcardSideBlock.hotelsFilters());

        assert.equal(sideblockGuestsValue, guestsValue, 'Значение гостей в оверлее не совпадает с выдачей');
        assert.equal(sideblockDateValue, dateRangeValue.split('/')[0], 'Значение даты в оверлее не совпадает с выдачей');
    });

    it('Клик в оффер на выдаче открывает оверлей отеля', async function() {
        const queryValidator = query => {
            assert(query.text, 'нет параметра text в ссылке карточки');
            assert(query.oid, 'нет параметра oid в ссылке карточки');
            assert(query.ag_dynamic, 'нет параметра ag_dynamic в ссылке карточки');

            return true;
        };

        await this.browser.yaCheckLink2({
            selector: PO.hotelsOrgmn.list.offer.wrapLink(),
            url: {
                href: '/search',
                ignore: ['protocol', 'hostname'],
                queryValidator,
            },
            message: 'Сломана ссылка отеля',
            target: '_blank',
        });

        await this.browser.yaCheckBaobabCounter(PO.hotelsOrgmn.list.offer(), {
            path: '/$page/$main/$result/hotels-orgmn/hotel-list/item[@permalink and @minPrice and @operatorId and @imageUrl]/card',
        });
        await this.browser.yaWaitForVisible(this.PO.bcardSideBlock());

        await this.browser.setBaseMetrics(metrics => metrics.concat('web.total_dynamic_click_count'));
    });
});
