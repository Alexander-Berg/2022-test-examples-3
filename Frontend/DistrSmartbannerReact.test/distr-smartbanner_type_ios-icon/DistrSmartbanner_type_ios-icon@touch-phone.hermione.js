'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn(['searchapp-phone', 'chrome-phone'], 'платформоспецифично');
hermione.also.in('iphone-dark');
specs({
    feature: 'Смартбаннер React',
    type: 'ios-icon',
}, () => {
    it('Внешний вид смартбаннера с иконкой', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 3844328659,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '72057605739835823:1462067888935983660',
                layout: 'ios-icon',
                product: 'browser-app_remarketing',
            },
        });

        await this.browser.yaAssertViewIsolated('smartbanner-ios-icon', PO.smartInfo());
    });

    it('Внешний вид модального смартбаннера с иконкой', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 809707025,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '72057605739835824:1462074914928120998',
                layout: 'ios-icon',
                product: 'browser-app_remarketing',
            },
        });

        await this.browser.assertView('smartbanner-ios-icon-center', PO.page(),
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
