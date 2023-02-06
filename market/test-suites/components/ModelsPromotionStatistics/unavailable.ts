'use strict';

import {mergeSuites, importSuite, makeSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

/**
 * Заглушка "Статистика недоступна"
 * @param {PageObject.InfoPanel} panel
 * @param {Object} params
 * @param {string} [params.title] Заголовок информационной панели
 * @param {string} [params.text] Текст информационной панели
 */
export default makeSuite('Сообщение о недоступности услуги.', {
    issue: 'VNDFRONT-3225',
    id: 'vendor_auto-1008',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        importSuite('InfoPanel', {
            suiteName: 'При отключенной услуге',
        }),
        importSuite('Link', {
            suiteName: 'Ссылка на справку',
            params: {
                url: buildUrl('external:help:promotion-model'),
                caption: 'Начать продвижение',
                target: '_blank',
                external: true,
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('LinkLevitan', this.panel);
                },
            },
        }),
    ),
});
