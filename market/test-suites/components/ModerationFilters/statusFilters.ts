'use strict';

import {makeSuite, makeCase} from 'ginny';

// @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
const getValue = ({value}) => value;

export default makeSuite('Фильтр по статусу.', {
    id: 'vendor_auto-286',
    issue: 'VNDFRONT-2330',
    feature: 'Модерация',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При выборе фильтра показывается результат, соответствующий параметрам фильтрации': makeCase({
            id: 'vendor_auto-286',
            async test() {
                await this.filters.checkFilterByText(this.params.checkboxName);

                await this.filters.isCheckedByText(this.params.checkboxName);

                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());

                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());

                const elems = await this.moderationList.statuses;
                const value = await elems.value;
                // @ts-expect-error(TS7034) найдено в рамках VNDFRONT-4580
                const res = [];

                for (const {ELEMENT} of value) {
                    // eslint-disable-next-line no-await-in-loop
                    res.push(await this.browser.elementIdText(ELEMENT).then(getValue));
                }

                await this.browser.allure.runStep(
                    `Проверяем, что все заявки в списке с правильным статусом ${this.params.checkboxName}`,
                    () =>
                        // @ts-expect-error(TS7005) найдено в рамках VNDFRONT-4580
                        res.every(item => item === this.params.status).should.to.be.equal(true),
                );
            },
        }),
    },
});
