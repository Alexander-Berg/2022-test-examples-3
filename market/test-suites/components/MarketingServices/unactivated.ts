'use strict';

import {mergeSuites, importSuite, makeSuite, PageObject} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const InfoPanel = PageObject.get('InfoPanel');

/**
 * Заглушка "Услуги не подключены"
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Сообщение о том, что услуги не подключены.', {
    id: 'vendor_auto-1119',
    issue: 'VNDFRONT-3286',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        importSuite('InfoPanel', {
            suiteName: 'Информационная панель при заходе вендором с неподключенными услугами',
            params: {
                title: 'Услуги не подключены',
                text:
                    'Чтобы подключить маркетинговые услуги, ' +
                    'примите оферту на странице «Настройки» или заключите договор — ' +
                    'для этого обратитесь к менеджеру Маркета.',
            },
            pageObjects: {
                panel() {
                    return this.createPageObject('InfoPanel');
                },
            },
        }),
        importSuite('Link', {
            suiteName:
                'Ссылка в информационном сообщении при заходе вендором с неподключенными маркетинговыми услугами',
            meta: {
                environment: 'kadavr',
            },
            params: {
                user: 'Пользователь',
                caption: 'странице «Настройки»',
                comparison: {
                    skipHostname: true,
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl(ROUTE_NAMES.SETTINGS, {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        vendor: this.params.vendor,
                        tab: 'info',
                    });
                },
            },
            pageObjects: {
                link() {
                    return this.createPageObject('LinkLevitan', InfoPanel.root);
                },
            },
        }),
    ),
});
