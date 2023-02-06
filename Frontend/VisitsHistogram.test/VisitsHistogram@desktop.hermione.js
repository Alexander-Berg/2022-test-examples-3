'use strict';

const POSimilar = require('../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');
const PO = require('./VisitsHistogram.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Гистограмма посещаемости',
}, function() {
    it('Основные проверки', async function() {
        await this.browser.yaOpenSerp(
            { text: 'кафе пушкин', srcskip: 'YABS_DISTR', data_filter: 'companies' },
            PO.oneOrg(),
        );
        await this.browser.yaWaitForVisible(PO.oneOrg.visitsHistogram());

        await this.browser.yaWaitForVisible(PO.oneOrg.visitsHistogram.scroller.wrap(), 'Элемента нет');

        // Ждём пока гистограмма подскролится, чтобы не было миганий скриншота
        await this.browser.pause(500);
        await this.browser.assertView('plain', PO.oneOrg.visitsHistogram());

        await this.browser.yaCheckBaobabCounter(
            () => this.browser.click(PO.oneOrg.visitsHistogram.scroller.arrowRight()),
            {
                path: '/$page/$parallel/$result/composite/tabs/about/visits-histogram/scroller/scroll_right[@direction="right"]',
                behaviour: { type: 'dynamic' },
                service: 'web',
                fast: { wzrd: 'companies', subtype: 'company' },
            },
        );

        await this.browser.yaCheckBaobabCounter(
            () => this.browser.click(PO.oneOrg.visitsHistogram.scroller.arrowLeft()),
            {
                path: '/$page/$parallel/$result/composite/tabs/about/visits-histogram/scroller/scroll_left[@direction="left"]',
                behaviour: { type: 'dynamic' },
                service: 'web',
                fast: { wzrd: 'companies', subtype: 'company' },
            },
        );

        // TODO: разобраться, как проверять баобаб-счётчик скролла SERP-97686
        // await this.browser.yaCheckBaobabCounter(
        //     () => this.browser.yaScrollContainer(PO.oneOrg.visitsHistogram.scroller.wrap(), 9999),
        //     {
        //         path: '/$page/$parallel/$result/composite/tabs/about/visits-histogram/scroller',
        //         service: 'web',
        //         fast: { wzrd: 'companies', subtype: 'company' },
        //     }
        // );
    });

    it('Попап', async function() {
        await this.browser.yaOpenSerp(
            { text: 'кафе пушкин', data_filter: 'companies' },
            PO.oneOrg(),
        );
        await this.browser.click(POSimilar.oneOrg.similarCompanies.scroller.firstItem());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg());
        await this.browser.yaWaitForVisible(
            PO.popup.oneOrg.visitsHistogram(),
            'Гистограмма не отрисовалась',
        );

        await this.browser.yaScroll(PO.popup.oneOrg.visitsHistogram());

        await this.browser.yaCheckBaobabCounter(
            () => this.browser.click(PO.popup.oneOrg.visitsHistogram.scroller.arrowRight()),
            {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/visits-histogram/scroller/scroll_right[@direction="right"]',
                service: 'web',
            },
        );

        await this.browser.yaCheckBaobabCounter(
            () => this.browser.click(PO.popup.oneOrg.visitsHistogram.scroller.arrowLeft()),
            {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/visits-histogram/scroller/scroll_left[@direction="left"]',
                service: 'web',
            },
        );

        // TODO: разобраться, как проверять баобаб-счётчик скролла SERP-97686
        // await this.browser.yaCheckBaobabCounter(
        //     () => this.browser.yaScrollContainer(PO.popup.oneOrg.visitsHistogram.scroller.wrap(), 0),
        //     {
        //         path: '/' + [
        //             '$page',
        //             '$parallel',
        //             '$result',
        //             'modal-popup',
        //             'modal-content-loader',
        //             'content',
        //             'company',
        //             'tabs',
        //             'about',
        //             'visits-histogram',
        //             'scroller',
        //          ].join('/'),
        //         service: 'web',
        //     }
        // );
    });
});
