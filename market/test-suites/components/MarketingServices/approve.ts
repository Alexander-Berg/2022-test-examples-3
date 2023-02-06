'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Подтверждение кампании
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Кнопка подтверждения кампании.', {
    environment: 'kadavr',
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                toast() {
                    return this.createPageObject('NotificationLevitan');
                },
                listItem() {
                    return this.createPageObject('MarketingServicesListItem', this.list, this.list.getItemByIndex(0));
                },
                approveButton() {
                    return this.createPageObject(
                        'ButtonLevitan',
                        this.browser,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.listItem.elem(ButtonLevitan.getByText('Подтвердить')),
                    );
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.approveButton
                .isExisting()
                .should.eventually.be.equal(true, 'Кнопка [Подтвердить] отображается');
        },
        'После создания новой кампании': {
            'при клике на кнопку [Подтвердить]': {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {isManager} = this.params;

                    if (isManager) {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.currentTest._meta.id = 'vendor_auto-1154';
                    }
                },
                'происходит успешное подтверждение': makeCase({
                    id: 'vendor_auto-1152',
                    issue: 'VNDFRONT-3883',
                    async test() {
                        await this.approveButton.click();

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal('Кампания подтверждена', 'Текст всплывающего сообщения верный');

                        return this.listItem.status
                            .getText()
                            .should.eventually.equal('Запланирована', 'Статус кампании верный');
                    },
                }),
            },
        },
    },
});
