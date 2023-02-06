'use strict';

const PO = require('./SocialNetworks.page-object').touchPhone;

specs({
    feature: '1Орг',
    type: 'Соцсети',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'кафе Пушкин',
                data_filter: 'companies',
            },
            PO.oneOrg(),
        );

        await this.browser.assertView('plain', PO.oneOrg.OrgSocialNetworks());
    });
    it('Проверка ссылок', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'кафе Пушкин',
                data_filter: 'companies',
            },
            PO.oneOrg(),
        );

        this.browser.yaCheckLink2({
            selector: PO.oneOrg.OrgSocialNetworks.item(),
            url: {
                href: 'https://www.facebook.com/CafePushkin',
            },
            baobab: { path: '/$page/$main/$result/composite/sites/scroller/"socnet/facebook"' },
        });
    });
});
