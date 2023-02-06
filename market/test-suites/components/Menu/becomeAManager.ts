'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на назначение менеджера для карточки вендора
 * @param {PageObject.ManagerCard} managerCard - карточка менеджера
 */
export default makeSuite('Боковое меню. Блок менеджера. Назначение менеджера карточки.', {
    issue: 'VNDFRONT-4333',
    id: 'vendor_auto-746',
    environment: 'kadavr',
    feature: 'Меню',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                toasts() {
                    return this.createPageObject('NotificationGroupLevitan');
                },
                toast() {
                    return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                },
                link() {
                    return this.createPageObject('Link', this.managerCard.becomeManagerLink);
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.browser.allure.runStep('Ожидаем появления карточки менеджера', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.managerCard.waitForVisible(),
            );
        },
        'При клике на ссылку': {
            'устанавливает текущего пользователя менеджером карточки': makeCase({
                async test() {
                    await this.link.isVisible().should.eventually.be.equal(true, 'Ссылка отображается');

                    await this.browser.allure.runStep('Проверяем текст ссылки', () =>
                        this.link.getText().should.eventually.be.equal('Стать менеджером', 'Текст ссылки корректный'),
                    );

                    await this.browser.allure.runStep('Кликаем по ссылке', () => this.link.root.click());

                    await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                        this.toasts.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Ожидаем появления всплывающего сообщения', () =>
                        this.toast.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем содержимое всплывающего сообщения', () =>
                        this.toast
                            .getText()
                            .should.eventually.equal(
                                'Вы стали менеджером карточки',
                                'Текст всплывающего сообщения корректный',
                            ),
                    );

                    await this.browser.allure.runStep('Проверяем имя менеджера', () =>
                        this.managerCard.managerName
                            .getText()
                            .should.eventually.be.equal('Vendors Manager', 'Имя менеджера корректное'),
                    );

                    await this.browser.allure.runStep('Проверяем аватар менеджера', () =>
                        this.managerCard.managerAvatar
                            .getAttribute('src')
                            .should.eventually.be.equal(
                                'https://s3.mdst.yandex.net/vendors-public/manager-avatars/robot-vendorsmanager.jpg',
                                'Аватар корректный',
                            ),
                    );
                },
            }),
        },
    },
});
