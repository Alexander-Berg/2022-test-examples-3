'use strict';

const PO = require('../DistrPopup.page-object');

specs({
    feature: 'Popup на СЕРПе',
    type: 'С отложенным появлением',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'test',
            foreverdata: '2527250467',
        }, PO.page());

        await this.browser.yaShouldNotBeVisible(PO.distrPopup());
    });

    hermione.only.in('chrome-desktop', 'Только chrome тригерит visibilitychange');
    it('Клик в сниппет', async function() {
        const PO = this.PO;

        await this.browser.click(PO.firstSnippet.organic.title.link());

        const tabIds = await this.browser.yaWaitUntil('Не открылась новая вкладка', async () => {
            const tabIds = await this.browser.getTabIds();
            return tabIds.length > 1 && tabIds;
        });

        await this.browser.switchTab(tabIds.pop());
        await this.browser.close();
        await this.browser.yaWaitForVisible(PO.distrPopup());
        await this.browser.assertView('tricky', PO.distrPopup());
        await this.browser.yaCheckCounter2(() => {}, { path: '/tech/promo_popup/delayed/show' });
    });
});
