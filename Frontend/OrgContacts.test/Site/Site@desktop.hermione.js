'use-strict';

const PO = require('../OrgContacts.page-object')('desktop');

specs('Контакты организации', function() {
    describe('Сайт', function() {
        it('Должен открыться сайт организации в новой вкладке', async function() {
            await this.browser.yaOpenSerp({
                text: 'кафе пушкин',
                data_filter: 'companies',
            }, PO.OrgContacts());

            await this.browser.yaCheckVacuum(
                { type: 'show', orgid: '1018907821', event: 'show_org' },
                'Не сработала метрика на показ организации',
            );

            await this.browser.yaCheckLink2({
                selector: PO.OrgContacts.SiteItem.Link(),
                url: {
                    href: 'https://cafe-pushkin.ru/',
                },
                baobab: {
                    path: '/$page/$parallel/$result/composite/tabs/about/contacts/site[@action = "site"]',
                },
            });

            await this.browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '1018907821', event: 'site', goal: '' },
                'Не сработала метрика на клик в сайт',
            );
        });
    });
});
