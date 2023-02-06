'use strict';

import {makeSuite, mergeSuites, importSuite, makeCase, PageObject} from 'ginny';

const Entry = PageObject.get('Entry');

function createCheckboxFilterCase(text: string, countKey: string) {
    return makeCase({
        id: 'vendor_auto-280',
        defaultParams: {
            filter: text,
        },
        params: {
            filter: 'Фильтр',
        },
        async test() {
            const n = this.params[countKey];

            await this.filters.checkFilterByText(text);
            await this.list.waitForLoading();

            return this.list.getItemsCount(Entry.root).should.eventually.equal(n, `Кол-во отфильтрованных заявок ${n}`);
        },
    });
}

/**
 * Тест на блок EntryContainer.
 * @param {PageObject.ListContainer} list
 * @param {string} params.searchText - текст поиска
 * @param {number} params.searchItemsCount - кол-во найденных заявок
 * @param {number} params.acceptedItemsCount - кол-во обработанных заявок
 */
export default makeSuite('Список заявок.', {
    issue: 'VNDFRONT-1610',
    environment: 'kadavr',
    feature: 'Заявки',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    filters: this.createPageObject('EntryFilters'),
                });

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());
                await this.list.waitForLoading();
            },
        },
        importSuite('ListContainer', {
            suiteName: 'Список заявок.',
            meta: {
                id: 'vendor_auto-250',
                issue: 'VNDFRONT-1610',
                environment: 'kadavr',
                feature: 'Заявки',
            },
            params: {
                lazy: true,
            },
        }),
        {
            Фильтрует: {
                'по тексту': makeCase({
                    id: 'vendor_auto-239',
                    async test() {
                        const n = this.params.searchItemsCount;
                        const text = this.params.searchText;

                        await this.filters.setText(text);
                        await this.list.waitForLoading();

                        return this.list
                            .getItemsCount(Entry.root)
                            .should.eventually.equal(n, `Кол-во отфильтрованных заявок ${n}`);
                    },
                }),
                '"Обработанные"': createCheckboxFilterCase('Обработанные', 'acceptedItemsCount'),
                '"Отказ"': createCheckboxFilterCase('Отказ', 'cancelledItemsCount'),
                '"В работе"': createCheckboxFilterCase('В работе', 'inWorkItemsCount'),
                '"Активные"': createCheckboxFilterCase('Активные', 'newItemsCount'),
                '"Только мои"': createCheckboxFilterCase('Только мои', 'onlyMyItemsCount'),
            },
        },
    ),
});
