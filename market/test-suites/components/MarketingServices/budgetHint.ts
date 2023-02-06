'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const TextLevitan = PageObject.get('TextLevitan');

/**
 * Хинт c суммой бюджета кампании
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Хинт c суммой бюджета кампании.', {
    id: 'vendor_auto-1161',
    issue: 'VNDFRONT-3292',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                hint() {
                    return this.createPageObject('DropdownB2bNext');
                },
                price() {
                    return this.createPageObject(
                        'TextLevitan',
                        this.list.getItemByIndex(0),
                        `${TextLevitan.root}:nth-of-type(3)`,
                    ).elem('span');
                },
            });
        },
        'При наведении курсора на сумму бюджета кампании': {
            'всплывает попап с суммой в рублях': makeCase({
                async test() {
                    await this.browser.allure.runStep('Наводим курсор на центр элемента', () =>
                        this.price.moveToObject(),
                    );

                    await this.browser.allure.runStep('Ожидаем отображения подсказки с суммой в рублях', () =>
                        this.hint.waitForVisible(),
                    );

                    await this.hint.getActiveText().should.eventually.be.equal('30 150 ₽');

                    await this.browser.allure.runStep('Убираем курсор с элемента', () =>
                        this.price.moveToObject(100, 100),
                    );

                    return this.browser.waitUntil(
                        async () => {
                            const isVisible = await this.hint.isVisible();

                            return isVisible === false;
                        },
                        this.browser.options.waitforTimeout,
                        'Не удалось дождаться скрытия подсказки с суммой в рублях',
                    );
                },
            }),
        },
    },
});
