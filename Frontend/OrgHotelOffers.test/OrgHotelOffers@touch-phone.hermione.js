'use strict';

const PO = require('./OrgHotelOffers.page-object')('touch-phone');

specs({
    feature: 'Hotels / Колдунщик одной организации',
    type: 'Стандартный вид',
}, function() {
    it('Партнеры не дублируются после перезапроса', async function() {
        await this.browser.yaOpenSerp({
            text: 'мета москва',
            data_filter: 'companies',
        }, PO.oneOrg.list());

        await this.browser.yaOpenOverlayAjax(
            PO.oneOrg.hotelFilters.submit(),
            PO.overlayPanel.tabsPanes.rooms(),
            'Ошибка при открытии таба "Номера" в оверлее',
            { timeout: 15000 },
        );

        await this.browser.yaWaitForVisible(
            this.PO.overlayPanel.oneOrgTabs.tabsPanes.rooms.hotelRooms.hotelOffersSearch.hotelRoomsList(),
        );
        await this.browser.click(PO.overlayPanel.back());
        await this.browser.yaWaitForVisible(PO.oneOrg.list());

        const result = await this.browser.execute(function(offerSelector, operatorSelector) {
            let offers = document.querySelectorAll(offerSelector);
            let partners = Array.prototype.map.call(offers, function(item) {
                return item.querySelector(operatorSelector).textContent;
            });
            return partners.every(function(item) {
                return partners.indexOf(item) === partners.lastIndexOf(item);
            });
        }, PO.oneOrg.list.offer(), PO.oneOrg.list.offer.link.operator());

        assert.isTrue(result, 'В спике офферов есть повторяющиеся партнеры');
    });
});
