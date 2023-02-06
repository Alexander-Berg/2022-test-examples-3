'use strict';

import {makeSuite, mergeSuites, makeCase} from 'ginny';

/**
 * Тест на блок ListContainer.
 * @param {PageObject.ListContainer} list
 * @param {Object} params
 * @param {boolean} [params.lazy] - флаг наличия автоматической подгрузки страниц при прокручивании
 */
export default makeSuite('Список.', {
    issue: 'VNDFRONT-1256',
    environment: 'testing',
    params: {
        user: 'Пользователь',
        lazy: 'Автоподгрузка',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.list.waitForLoading();
            },
        },
        {
            'Загружает следующую страницу': makeCase({
                async test() {
                    const n1 = await this.list.getItemsCount();

                    await this.list.loadNextPage(this.params.lazy);

                    const n2 = await this.list.getItemsCount();

                    await this.expect(n2 > n1).to.equal(true, 'Загрузилась следующая страница');
                },
            }),
        },
    ),
});
