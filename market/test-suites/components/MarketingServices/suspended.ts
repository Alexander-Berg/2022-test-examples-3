'use strict';

import {mergeSuites, importSuite, makeSuite, PageObject} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

const CutoffsPanelCutoff = PageObject.get('CutoffsPanelCutoff');

/**
 * Заглушка "Услуга приостановлена"
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Сообщение о том, что услуга приостановлена.', {
    id: 'vendor_auto-1120',
    issue: 'VNDFRONT-3286',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        importSuite('InfoPanel', {
            suiteName: 'Информационная панель при заходе вендором с истекшим сроком документов',
            params: {
                title: 'Услуга приостановлена',
                text:
                    'Закончился срок действия документа, ' +
                    'который подтверждает ваше право на товарный знак. ' +
                    'Обновите документ на странице «Настройки».',
            },
            pageObjects: {
                panel() {
                    return this.createPageObject('CutoffsPanelCutoff');
                },
            },
        }),
        importSuite('Link', {
            suiteName: 'Ссылка в информационном сообщении при заходе вендором с истекшим сроком документов оферты',
            meta: {
                environment: 'kadavr',
            },
            params: {
                user: 'Пользователь',
                caption: '«Настройки»',
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
                    return this.createPageObject('LinkLevitan', CutoffsPanelCutoff.root);
                },
            },
        }),
    ),
});
