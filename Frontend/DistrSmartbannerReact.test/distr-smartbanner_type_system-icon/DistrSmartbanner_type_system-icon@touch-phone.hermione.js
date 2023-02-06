'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn(['searchapp-phone', 'chrome-phone'], 'платформоспецифично');
hermione.also.in('iphone-dark');
specs({
    feature: 'Смартбаннер React',
    type: 'system-icon',
}, () => {
    it('Внешний вид системного смартбаннера с иконкой', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 1789004778,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '72057605739835825:1462081773078370835',
                layout: 'system-icon',
                product: 'browser-app_remarketing',
            },
        });

        await this.browser.yaAssertViewIsolated('smartbanner-system-icon', PO.smartInfo());
    });

    it('Внешний вид системного модального смартбаннера с иконкой', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 1188682494,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '72057605739835826:1462089243013993644',
                layout: 'system-icon',
                product: 'browser-app_remarketing',
            },
        });

        await this.browser.assertView('smartbanner-system-icon-center', PO.page(),
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
