'use strict';

const PO = require('./OrgSkiStatus.page-object/touch-phone@index');

specs({
    feature: 'Одна организация',
    type: 'Горнолыжный курорт',
}, function() {
    h.it('Статус работы – внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3233895734',
            data_filter: 'companies',
        }, PO.NoticeStripe());

        await this.browser.assertView('org_ski_status', [PO.OrgHeader(), PO.OneOrgTabs()]);
    });
});
