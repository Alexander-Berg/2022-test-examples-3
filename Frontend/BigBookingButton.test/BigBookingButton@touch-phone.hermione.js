'use strict';

const PO = require('./BigBookingButton.page-object')('touch-phone');

// ПП не умеет много srcparams в урле. При раскатке hotels_rooms_awesome убрать исключение searchapp-phone
hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Hotels / Колдунщик одной организации',
    type: 'Стандартный вид',
    experiment: 'Редизайн таба номера',
}, function() {
    describe('Большая кнопка Выбрать номер', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                {
                    srcskip: 'YABS_DISTR',
                    text: 'МетаМосква отель',
                    exp_flags: 'hotels_rooms_awesome=1',
                    srcparams: [
                        'GEOV:experimental=middle_yandex_travel_CatRoomMaxOtherPct=25',
                        'GEOV:experimental=middle_yandex_travel_AutoCatRoomForAll=1',
                        'GEOV:experimental=middle_yandex_travel_EnableCatRoom=1',
                        'GEOV:experimental=middle_yandex_travel_CatRoomShowOther=0',
                    ],
                    data_filter: 'companies',
                },
                PO.companies.bigBookingButton(),
            );
        });

        it('Внешний вид', async function() {
            // скрин с ценой
            await this.browser.yaAssertViewExtended(
                'plain',
                PO.companies.bigBookingButton(),
            );

            // меняем даты
            await this.browser.click(PO.companies.bigBookingButton());
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.rooms.form());
            await this.browser.yaSetDateRange(100, 100, {
                control: PO.overlay.tabsPanes.rooms.form.datePicker(),
                input: PO.overlay.tabsPanes.rooms.form.datePicker.input(),
                sideblockCalendar: PO.sideBlockCalendar(),
                day: PO.sideBlockCalendar.day(),
                save: PO.sideBlockCalendar.button(),
            });
            await this.browser.click(PO.overlay.tabsPanes.rooms.form.submit());
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.rooms.list());
            await this.browser.click(PO.overlayBack());
            await this.browser.yaWaitForHidden(PO.overlay());
            await this.browser.yaShouldBeVisible(PO.companies.bigBookingButton());
            await this.browser.yaShouldNotBeVisible(PO.companies.bigBookingButton.price());

            // скрин без цены
            await this.browser.yaAssertViewExtended(
                'without-price',
                PO.companies.bigBookingButton(),
            );
        });

        it('Открывает таб Номера', async function() {
            await this.browser.yaCheckBaobabCounter(
                PO.companies.bigBookingButton(),
                {
                    path: '/$page/$main/$result/composite/booking-button-wrapper/big-booking-button',
                    attrs: { offerId: '*' },
                    behaviour: { type: 'dynamic' },
                },
            );
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.rooms());
            await this.browser.setBaseMetrics(metrics => metrics.concat('web.total_dynamic_click_count'));
        });

        it('Цена обновляется при смене дат', async function() {
            // запоминаем цену
            const priceBefore = await this.browser.getText(PO.companies.bigBookingButton.price());
            assert.isOk(priceBefore, 'Цена должна быть задана');

            // открываем оверлей
            await this.browser.yaOpenOverlayAjax(PO.companies.tabsMenu.about(), PO.overlay());
            await this.browser.yaWaitForVisible(PO.overlay.bigBookingButton());

            // проверяем цену
            const priceBeforeOverlay = await this.browser.getText(PO.overlay.bigBookingButton.price());
            assert.equal(priceBeforeOverlay, priceBefore, 'Цена в оверлее не совпадает с ценой на выдаче');

            // меняем даты
            await this.browser.click(PO.overlay.tabsMenu.rooms());
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.rooms.form());
            await this.browser.yaSetDateRange(0, 3, {
                control: PO.overlay.tabsPanes.rooms.form.datePicker(),
                input: PO.overlay.tabsPanes.rooms.form.datePicker.input(),
                sideblockCalendar: PO.sideBlockCalendar(),
                day: PO.sideBlockCalendar.day(),
                save: PO.sideBlockCalendar.button(),
            });
            await this.browser.click(PO.overlay.tabsPanes.rooms.form.submit());
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.rooms.list());
            await this.browser.click(PO.overlay.tabsMenu.about());
            await this.browser.yaWaitForVisible(PO.overlay.tabsPanes.about());

            // проверяем цену
            const priceAfterOverlay = await this.browser.getText(PO.overlay.bigBookingButton.price());
            assert.notEqual(priceAfterOverlay, priceBefore, 'Цена в оверлее должна измениться');

            // возвращаемся на выдачу
            await this.browser.click(PO.overlayBack());
            await this.browser.yaWaitForHidden(PO.overlay());
            await this.browser.yaShouldBeVisible(PO.companies.bigBookingButton());

            // проверяем цену
            const priceAfter = await this.browser.getText(PO.companies.bigBookingButton.price());
            assert.notEqual(priceAfter, priceBefore, 'Цена на выдаче должна измениться');
        });
    });
});
