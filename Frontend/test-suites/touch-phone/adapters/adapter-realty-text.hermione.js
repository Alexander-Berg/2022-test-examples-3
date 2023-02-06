'use strict';

const PO = require('../../../page-objects/touch-phone/index').PO;

const REALTY_URL = 'https://realty.yandex.ru/odintsovskiy_rayon/kupit/kvartira/';

specs('Колдунщик недвижимости', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ text: 'купить квартиру в одинцово вторичное жилье недорого без посредников' })
            .yaWaitForVisible(PO.realty(), 'Колдунщик недвижимости должен быть в выдаче');
    });
    it('Заголовок', function() {
        return this.browser
            .assertView('plain', PO.realty())
            .yaCheckBaobabCounter(PO.realty.title.link(), {
                path: '/$page/$main/$result[@wizard_name="realty/text"]/title'
            })
            .yaCheckLink(PO.realty.title.link()).then(url => this.browser
                .yaCheckURL(url, REALTY_URL, {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            );
    });
    it('Гринурл', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.realty.path.item(), {
                path: '/$page/$main/$result[@wizard_name="realty/text"]/path/urlnav'
            })
            .yaCheckLink(PO.realty.path.item()).then(url => this.browser
                .yaCheckURL(url, REALTY_URL, {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            );
    });
    it('Сайтлинки', function() {
        return this.browser
            .yaCheckBaobabCounter(PO.realty.firstSitelink(), {
                path: '/$page/$main/$result[@wizard_name="realty/text"]/sitelinks/item'
            })
            .yaCheckLink(PO.realty.firstSitelink()).then(url => this.browser
                .yaCheckURL(url, REALTY_URL, {
                    skipProtocol: true,
                    skipPathnameTrail: true,
                    skipQuery: true
                })
            );
    });
});
