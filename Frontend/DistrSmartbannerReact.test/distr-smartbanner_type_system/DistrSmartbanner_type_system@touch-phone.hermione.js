'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn(['iphone', 'searchapp-phone', 'chrome-phone'], 'фича таргетированная');
hermione.also.in('safari13');
specs({
    feature: 'Смартбаннер React',
    type: 'system',
}, () => {
    hermione.also.in('iphone-dark');
    it('Внешний вид смартбаннера', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 1992922841,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-system', PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic" and @layout="system"]',
        });
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Системный смартбаннер не закрылся');
    });

    it('Внешний вид центрированного смартбаннера', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 2681850791,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.assertView('smartbanner-system-center', PO.page(),
            {
                allowViewportOverflow: true, captureElementFromTop: true, compositeImage: false,
                hideElements: [
                    PO.header(),
                    PO.main(),
                    PO.footer(),
                ],
            },
        );

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic" and @layout="system" and @theme="modal"]',
        });
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Системный смартбаннер не закрылся');
    });

    it('Внешний вид нижнего баннера с кнопкой согласия слева', async function() {
        const { browser } = this;
        const PO = this.PO;

        await browser.yaOpenSerp({
            text: 'test',
            foreverdata: 2151686246,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await browser.yaWaitUntilSerpLoaded();

        await this.browser.assertView('smartbanner-bottom-left', PO.page(),
            {
                allowViewportOverflow: true, captureElementFromTop: true, compositeImage: false,
                hideElements: [
                    PO.header(),
                    PO.main(),
                    PO.footer(),
                ],
            },
        );
    });

    it('Внешний вид нижнего баннера с кнопкой согласия справа', async function() {
        const { browser } = this;
        const PO = this.PO;

        await browser.yaOpenSerp({
            text: 'test',
            foreverdata: 44057023,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await browser.yaWaitUntilSerpLoaded();

        await this.browser.assertView('smartbanner-bottom-right', PO.page(),
            {
                allowViewportOverflow: true, captureElementFromTop: true, compositeImage: false,
                hideElements: [
                    PO.header(),
                    PO.main(),
                    PO.footer(),
                ],
            },
        );
    });

    it('Внешний вид баннера по центру с кнопкой согласия слева', async function() {
        const { browser } = this;
        const PO = this.PO;

        await browser.yaOpenSerp({
            text: 'test',
            foreverdata: 957529940,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await browser.yaWaitUntilSerpLoaded();

        await this.browser.assertView('smartbanner-center-left', PO.page(),
            {
                allowViewportOverflow: true, captureElementFromTop: true, compositeImage: false,
                hideElements: [
                    PO.header(),
                    PO.main(),
                    PO.footer(),
                ],
            },
        );
    });

    it('Внешний вид баннера по центру с кнопкой согласия справа', async function() {
        const { browser } = this;
        const PO = this.PO;

        await browser.yaOpenSerp({
            text: 'test',
            foreverdata: 408614183,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await browser.yaWaitUntilSerpLoaded();

        await this.browser.assertView('smartbanner-center-right', PO.page(),
            {
                allowViewportOverflow: true, captureElementFromTop: true, compositeImage: false,
                hideElements: [
                    PO.header(),
                    PO.main(),
                    PO.footer(),
                ],
            },
        );
    });
});
