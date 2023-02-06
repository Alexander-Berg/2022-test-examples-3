'use-strict';

const PO = require('./OrgContacts.page-object')('desktop');

specs({
    feature: 'Одна организация',
    experiment: 'Контакты',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.OrgContacts());
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.OrgContacts());
    });

    it('Внешний вид при разрешении 1200px', async function() {
        await this.browser.setViewportSize({ width: 1200, height: 800 });
        await this.browser.assertView('plain-1200px', PO.OrgContacts());
    });

    it('Внешний вид при разрешении 1050px', async function() {
        await this.browser.setViewportSize({ width: 1050, height: 800 });
        await this.browser.assertView('plain-1050px', PO.OrgContacts());
    });

    it('Внешний вид на бедных данных', async function() {
        await this.browser.yaOpenSerp({
            text: 'парк отель солнечный',
            data_filter: 'companies',
        }, PO.OrgContacts());

        await this.browser.assertView('contacts-compact-pure', PO.OrgContacts());
    });
});
