'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на фильтрацию количества отображаемых элементов в списке.
 * @param {PageObject.PageSize} pageSize - фильтр по количеству элементов на странице
 * @param {PageObject.PagedList} list - список
 * @param {Object} params
 * @param {number} params.size - новое значение фильтра
 * @param {number} params.initialCount - начальное количество элементов в списке
 * @param {string} params.queryParamName - имя query-параметра фильтра
 */
export default makeSuite('Количество элементов.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При выборе значения': {
            'ограничивает число видимых элементов списка': makeCase({
                async test() {
                    const {queryParamName, initialCount, size} = this.params;

                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.to.be.equal(initialCount, `Отображается элементов: "${initialCount}"`);
                    await this.browser
                        .vndWaitForChangeUrl(() => this.pageSize.setSize(size))
                        .should.eventually.be.link(
                            {
                                query: {
                                    [queryParamName]: size,
                                },
                            },
                            {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            },
                        );
                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.to.be.equal(size, `Отображается элементов: "${size}"`);
                },
            }),
        },
    },
});
