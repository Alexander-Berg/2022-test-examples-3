'use strict';

const { hotelOrg: oneOrgPo, popup: modalPo } = require('../../../Companies.test/Companies.page-object/index@desktop');
const { calendar: calendarPopupPo } = require('../../../../../components/Calendar/Calendar.test/Calendar.page-object/index@desktop.js');
const { GuestsDropdownPopup: GuestsDropdownPopupPO } = require('../../../../../components/GuestsDropdown/GuestsDropdown.test/GuestsDropdown.page-object/index@desktop.js');

specs({
    feature: 'Hotels / Колдунщик одной организации',
    type: 'Стандартный вид',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'radisson collection',
            data_filter: 'companies',
            srcskip: 'YABS_DISTR',
        }, oneOrgPo.tabsPanes.about.hotelsReactForm.list());
    });

    it('Внешний вид офферов на главном табе', async function() {
        await this.browser.assertView('cols-12', oneOrgPo.tabsPanes.about.hotelsReactForm.list());
        await this.browser.setViewportSize({ width: 1230, height: 1200 });
        await this.browser.assertView('cols-10', oneOrgPo.tabsPanes.about.hotelsReactForm.list());
        await this.browser.setViewportSize({ width: 1130, height: 1200 });
        await this.browser.assertView('cols-8', oneOrgPo.tabsPanes.about.hotelsReactForm.list());
        await this.browser.setViewportSize({ width: 1030, height: 1200 });
        await this.browser.assertView('cols-6', oneOrgPo.tabsPanes.about.hotelsReactForm.list());
    });

    it('Ссылка оффера ведет на портал Путешествий', async function() {
        await this.browser.yaCheckLink2({
            selector: oneOrgPo.tabsPanes.about.hotelsReactForm.list.first.link(),
            url: {
                href: 'https://travel.yandex.ru/redir',
                ignore: ['query', 'hash'],
            },
            baobab: {
                path: '/$page/$parallel/$result/tabs/about/hotel_prices/hotel_offers/result',
            },
            message: 'Сломана ссылка на первом отельном оффере',
        });
    });

    // по мотивам задачи https://st.yandex-team.ru/SERP-119682
    hermione.only.in('chrome-desktop', 'Не браузерозависимо');
    it('Партнеры не дублируются после перезапроса', async function() {
        await this.browser.click(oneOrgPo.tabsMenu.rooms());
        await this.browser.yaWaitForVisible(oneOrgPo.tabsPanes.rooms.hotelRooms());
        await this.browser.click(oneOrgPo.tabsPanes.rooms.hotelRooms.hotelOffersSearch.hotelForm.submit());
        await this.browser.yaWaitForVisible(oneOrgPo.tabsPanes.rooms.hotelRooms.hotelOffersSearch.hotelOffersList());
        await this.browser.click(oneOrgPo.tabsMenu.about());
        await this.browser.yaWaitForVisible(oneOrgPo.tabsPanes.about.hotelsReactForm.list());

        const result = await this.browser.execute(function(offerSelector, operatorSelector) {
            let offers = document.querySelectorAll(offerSelector);
            let partners = Array.prototype.map.call(offers, function(item) {
                return item.querySelector(operatorSelector).textContent;
            });
            return partners.every(function(item) {
                return partners.indexOf(item) === partners.lastIndexOf(item);
            });
        },
        oneOrgPo.tabsPanes.about.hotelsReactForm.list.item(),
        oneOrgPo.tabsPanes.about.hotelsReactForm.list.item.link.operator(),
        );

        assert.isTrue(result, 'В спике офферов есть повторяющиеся партнеры');
    });

    // по багу в задаче https://st.yandex-team.ru/SERP-137281
    hermione.only.in('chrome-desktop', 'Не браузерозависимо');
    it('В табе Про отель в попапе есть офферы на длительную поездку', async function() {
        await this.browser.click(oneOrgPo.tabsPanes.about.hotelsReactForm.filters.dateAt());
        await this.browser.yaWaitForVisible(calendarPopupPo());
        await this.browser.click(calendarPopupPo.secondMonth.secondWeek.firstDay());
        await this.browser.click(calendarPopupPo.secondMonth.thirdWeek.lastDay());
        await this.browser.click(oneOrgPo.tabsPanes.about.hotelsReactForm.filters.personDropdown());
        await this.browser.yaWaitForVisible(GuestsDropdownPopupPO());
        await this.browser.click(GuestsDropdownPopupPO.Adults.PlusButton());
        await this.browser.click(GuestsDropdownPopupPO.Adults.PlusButton());
        await this.browser.click(oneOrgPo.tabsPanes.about.hotelsReactForm.filters.submit());
        await this.browser.yaWaitForVisible(
            modalPo.oneOrg.tabsPanes.rooms.hotelRooms.hotelOffersSearch.hotelOffersList());
        await this.browser.click(modalPo.oneOrg.tabsMenu.about());
        await this.browser.yaWaitForVisible(
            modalPo.oneOrg.tabsPanes.about.hotelsReactForm.list());
    });
});
