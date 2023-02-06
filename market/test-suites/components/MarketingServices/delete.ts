'use strict';

import {PageObject, mergeSuites, makeSuite, makeCase} from 'ginny';

const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Удаление кампаний менеджером
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Модальное окно с подтверждением удаления кампании.', {
    issue: 'VNDFRONT-3315',
    environment: 'kadavr',
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                modal() {
                    return this.createPageObject('Modal');
                },
                toast() {
                    return this.createPageObject('NotificationLevitan');
                },
                popup() {
                    return this.createPageObject('PopupB2b');
                },
                contextMenu() {
                    const listItem = this.createPageObject(
                        'MarketingServicesListItem',
                        this.list,
                        this.list.getItemByIndex(0),
                    );

                    return this.createPageObject('SelectAdvanced', this.browser, listItem.contextMenu);
                },
                deleteButton() {
                    return this.createPageObject(
                        'ButtonLevitan',
                        this.browser,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.modal.elem(ButtonLevitan.getByText('Удалить')),
                    );
                },
                cancelButton() {
                    return this.createPageObject(
                        'ButtonLevitan',
                        this.browser,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.modal.elem(ButtonLevitan.getByText('Отмена')),
                    );
                },
            });

            return this.browser.allure.runStep('Открываем модальное окно', async () => {
                await this.contextMenu.click();
                await this.popup.waitForPopupShown();
                await this.contextMenu.selectItem('Удалить');

                await this.deleteButton.isExisting().should.eventually.be.equal(true, 'Кнопка «Удалить» отображается');

                await this.cancelButton.isExisting().should.eventually.be.equal(true, 'Кнопка «Отмена» отображается');

                return this.modal.waitForVisible();
            });
        },
        'При клике на кнопку [Удалить]': {
            'модальное окно закрывается, кампания удаляется из списка': makeCase({
                id: 'vendor_auto-1158',
                async test() {
                    await this.deleteButton.click();

                    await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения с подтверждением', () =>
                        this.toast.waitForVisible(),
                    );

                    await this.toast
                        .getText()
                        .should.eventually.equal('Кампания удалена', 'Текст всплывающего сообщения верный');

                    await this.browser.allure.runStep('Ожидаем закрытия модального окна', () =>
                        this.browser.waitUntil(
                            async () => {
                                const isVisible = await this.modal.isVisible();

                                return isVisible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Модальное окно не закрылось',
                        ),
                    );

                    await this.list.waitForLoading();

                    return this.list.getItemsCount().should.eventually.equal(0, 'Кампания удалилась из списка');
                },
            }),
        },
        'При клике на кнопку [Отмена]': {
            'модальное окно закрывается, кампания остается в списке': makeCase({
                id: 'vendor_auto-1159',
                async test() {
                    await this.cancelButton.click();

                    await this.list.waitForLoading();

                    await this.browser.allure.runStep('Ожидаем закрытия модального окна', () =>
                        this.browser.waitUntil(
                            async () => {
                                const isVisible = await this.modal.isVisible();

                                return isVisible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Модальное окно не закрылось',
                        ),
                    );

                    return this.list.getItemsCount().should.eventually.equal(1, 'Кампания осталась в списке');
                },
            }),
        },
    }),
});
