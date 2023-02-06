'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Кнопка создания кампании
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Кнопка создания кампании.', {
    issue: 'VNDFRONT-3292',
    id: 'vendor_auto-1137',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                button() {
                    return this.createPageObject('MarketingServicesCreateCampaignButton');
                },
            });
        },
        'При открытии страницы': {
            присутствует: makeCase({
                test() {
                    return this.button
                        .isExisting()
                        .should.eventually.be.equal(true, 'Кнопка создания кампании присутствует');
                },
            }),
        },
    },
});
