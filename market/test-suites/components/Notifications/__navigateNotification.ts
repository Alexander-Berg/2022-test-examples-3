'use strict';

import url from 'url';

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на переход на страницу уведомления
 * @param {PageObject.item} bell – колокольчик
 * @param {PageObject.PagedList} list – список уведомлений
 * @param {Object} params
 * @param {string} params.pageUrl - ссылка на страницу уведомления
 * @param {number} params.expectedUnreadCount - ожидаемое количество непрочитанных уведомлений
 * @param {number} [params.itemIndex] - позиция уведомления в списке
 */
export default makeSuite('Переход на непрочитанное уведомление.', {
    story: {
        'При клике на элемент списка': {
            'открывается страница уведомления': makeCase({
                async test() {
                    const {pageUrl, expectedUnreadCount, itemIndex = 0} = this.params;
                    const parsedUrl = url.parse(pageUrl, true, true);

                    await this.allure.runStep(`Кликаем по элементу в списке под номером "${itemIndex + 1}"`, () =>
                        this.browser
                            .vndWaitForChangeUrl(() => this.list.getItemByIndex(itemIndex).click())
                            .should.eventually.be.link(parsedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            }),
                    );
                    await this.bell.waitForCounterVisible();

                    await this.browser.waitUntil(
                        async () => {
                            const actualUnreadCount = await this.bell.getUnreadCount();

                            return actualUnreadCount === expectedUnreadCount;
                        },
                        this.browser.options.waitforTimeout,
                        'Счетчик уменьшился на единицу',
                    );
                },
            }),
        },
    },
});
