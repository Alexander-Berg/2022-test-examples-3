'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;
const MARKET_URL = 'https://m.market.yandex.ru';

specs({
    feature: 'Маркет',
    type: 'карточка неявной модели'
}, function() {
    hermione.only.notIn('winphone', `
        yaCheckSnippet не проходит в IE/Edge браузерах.
        Т.к. в IE/Edge ссылки имеют target="_self"
    `);
    it('Обязательные проверки', function() {
        return this.browser
            .yaOpenSerp({ text: 'холодильник океан', foreverdata: 2520238481 })
            .yaWaitForVisible(PO.marketConstr(), 'Колдунщик Маркета должен присутствовать на странице')
            .assertView('plain', PO.marketConstr())
            .yaCheckSnippet(PO.marketConstr, {
                title: {
                    url: {
                        href: MARKET_URL,
                        ignore: ['protocol', 'pathname', 'query']
                    },
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="market_constr"]/title'
                    }
                },
                greenurl: [
                    {
                        url: {
                            href: MARKET_URL,
                            ignore: ['protocol', 'pathname', 'query']
                        },
                        baobab: {
                            path: '/$page/$main/$result[@wizard_name="market_constr"]/path/urlnav'
                        }
                    }
                ]
            });
    });
});
