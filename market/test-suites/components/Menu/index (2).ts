'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
import Menu from 'spec/page-objects/Menu';

/**
 * @param {PageObject.Menu} menu - боковое меню
 * @param {PageObject.Link} link - кнопка добавления бренда/партнёра
 */
export default makeSuite('Боковое меню.', {
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    menu: this.createPageObject('Menu'),
                });
            },
        },
        importSuite('AddCampaignButton', {
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.menu, Menu.addCampaignLink);
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка на справку',
            meta: {
                id: 'vendor_auto-640',
                issue: 'VNDFRONT-3582',
                environment: 'testing',
            },
            params: {
                caption: 'Справка',
                url: buildUrl('external:help:access'),
                external: true,
                target: '_blank',
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.menu, Menu.helpItem);
                },
            },
        }),
    ),
});
