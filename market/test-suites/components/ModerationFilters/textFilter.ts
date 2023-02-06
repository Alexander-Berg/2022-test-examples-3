'use strict';

import {makeSuite, makeCase} from 'ginny';
import {ContextWithParams} from 'ginny-helpers';

const ITEM_NAME = '[Auto]';
const ITEM_NUMBER = '48373';

const getValue = <T>({value}: {value: T}): T => value;

async function checkCompanyNames(ctx: ContextWithParams, searchRequest: string, searchType: string) {
    let elems;
    if (searchType === 'name') {
        // @ts-expect-error (TS2339) найдено в рамках VNDFRONT-4580
        elems = await ctx.moderationList.companyNames;
    } else {
        // @ts-expect-error (TS2339) найдено в рамках VNDFRONT-4580
        elems = await ctx.moderationList.companyIDs;
    }

    const value = await elems.value;
    const res: string[] = [];

    for (const {ELEMENT} of value) {
        // eslint-disable-next-line no-await-in-loop
        res.push(await ctx.browser.elementIdText(ELEMENT).then(getValue));
    }

    await ctx.browser.allure.runStep(`Проверяем, что каждая заявка в списке включает в себя ${searchRequest}`, () =>
        res.every(item => item.includes(searchRequest)).should.to.be.equal(true),
    );
}

export default makeSuite('Поиск.', {
    id: 'vendor_auto-287',
    issue: 'VNDFRONT-2330',
    feature: 'Модерация',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При поиске заявки по номеру/имени показывается результат, соответствующий параметрам поиска': makeCase({
            async test() {
                await this.filters.setText(ITEM_NAME);

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());

                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                // @ts-expect-error (TS2339) найдено в рамках VNDFRONT-4580
                await checkCompanyNames(this, ITEM_NAME, 'name');

                await this.filters.setText('');

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());
                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                await this.browser.allure.runStep('Проверяем, что поиск по заявкам очистился', () =>
                    this.filters.searchInput.getValue().should.eventually.be.equal(''),
                );

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());

                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                await this.filters.setText(ITEM_NUMBER);

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());

                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                // @ts-expect-error (TS2339) найдено в рамках VNDFRONT-4580
                await checkCompanyNames(this, ITEM_NUMBER, 'IDs');
            },
        }),
    },
});
