'use strict';

import {makeCase, makeSuite, importSuite, mergeSuites} from 'ginny';

/**
 * Тест на вкладку "Уведомления".
 * @param {PageObject.Notifications} notifications
 */
export default makeSuite('Уведомления.', {
    environment: 'testing',
    feature: 'Уведомления',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления списка подписок.', () =>
                    this.notifications.waitForExist(),
                );
            },
        },
        /*
         * Прогоняем тесты только на первый блок подписки, так все остальные идентичны ему
         * и выполнены одним параметризованным компонентом.
         */
        importSuite('SubscribersGroup', {
            pageObjects: {
                subscribersGroup() {
                    return this.createPageObject(
                        'SubscribersGroup',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.notifications,
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.notifications.getSubscribersGroupByIndex(0),
                    );
                },
            },
        }),
        {
            'При смене вендора на странице': {
                'значения блоков подписки обновляются': makeCase({
                    issue: 'VNDFRONT-3404',
                    id: 'vendor_auto-765',

                    async test() {
                        this.setPageObjects({
                            subscribersGroup() {
                                return this.createPageObject(
                                    'SubscribersGroup',
                                    this.notifications,
                                    this.notifications.getSubscribersGroupByIndex(0),
                                );
                            },
                            subscribeByEmail() {
                                return this.createPageObject(
                                    'Subscribe',
                                    this.subscribersGroup,
                                    this.subscribersGroup.subscribeByEmail,
                                );
                            },
                            tagsByEmail() {
                                return this.createPageObject('Tags', this.subscribeByEmail);
                            },
                            subscribeByLogin() {
                                return this.createPageObject(
                                    'Subscribe',
                                    this.subscribersGroup,
                                    this.subscribersGroup.subscribeByLogin,
                                );
                            },
                            tagsByLogin() {
                                return this.createPageObject('Tags', this.subscribeByLogin);
                            },
                            vendorsSearch() {
                                return this.createPageObject('Search');
                            },
                            searchInput() {
                                return this.createPageObject('InputB2b');
                            },
                            searchSpinner() {
                                return this.createPageObject('SpinnerLevitan', this.searchInput);
                            },
                        });

                        const emails = await this.tagsByEmail.getItemsValues();

                        const logins = await this.tagsByLogin.getItemsValues();

                        await this.searchInput.setValue('3300');

                        await this.allure.runStep('Ожидаем завершения поиска', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const isVisible = await this.searchSpinner.isVisible();

                                    return isVisible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Спиннер не скрылся',
                            ),
                        );

                        await this.allure.runStep('Кликаем по первому найденному вендору из списка', () =>
                            this.vendorsSearch.getVendorsItemByIndex(0).click(),
                        );

                        await this.allure.runStep('Дожидаемся появления списка подписок', () =>
                            this.subscribersGroup.waitForVisible(),
                        );

                        await this.tagsByEmail.getItemsValues().should.eventually.not.have.same.members(emails);

                        await this.tagsByLogin.getItemsValues().should.eventually.not.have.same.members(logins);
                    },
                }),
            },
        },
    ),
});
