import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Header} header
 * @param {PageObject.SideMenu} sideMenu
 * @param {PageObject.compareTumbler} compareTumbler
 * @param {PageObject.Notification} notification
 */
export default makeSuite('Кнопка «Добавить в сравнение»', {
    story: {
        'По умолчанию': {
            'присутствует на сниппете': makeCase({
                id: 'm-touch-3427',
                issue: 'MARKETFRONT-18932',
                async test() {
                    const isExists = this.compareTumbler.isExisting();

                    return this.expect(isExists).to.be.equal(true, 'Кнопка присутствует на сниппете');
                },
            }),
        },
        'При клике': {
            'добавляет в сравнение': makeCase({
                id: 'm-touch-3427',
                issue: 'MARKETFRONT-18932',
                async test() {
                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitForVisible();

                    const initialValue = await this.sideMenu.getCompareCounterValue();
                    await this.sideMenu.closeMenu();
                    await this.browser.allure.runStep(
                        'Кликаем по значку сравнения на сниппете',
                        () => this.compareTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.header.clickMenuTrigger()
                        .then(() => this.sideMenu.waitComparisonCounterVisible());

                    const updatedValue = await this.sideMenu.getCompareCounterValue();

                    return this.expect(updatedValue - initialValue).to.be.equal(1, 'Товар добавлен в сравнение');
                },
            }),
            'уведомление ведёт в сравнение': makeCase({
                id: 'm-touch-3427',
                issue: 'MARKETFRONT-18932',
                async test() {
                    await this.browser.allure.runStep(
                        'Кликаем по значку сравнения на сниппете',
                        () => this.compareTumbler.click()
                    );

                    await this.notification.waitForText('Товар добавлен в сравнение. Нажмите, чтобы сравнить');
                    await this.notification.clickText();

                    return this.browser.allure.runStep(
                        'Проверяем URL после перехода',
                        () => this.browser.yaParseUrl().should.eventually.be.link({
                            pathname: '/compare-lists',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
        'При повторном клике': {
            'удаляет из сравнения': makeCase({
                id: 'm-touch-3427',
                issue: 'MARKETFRONT-18932',
                async test() {
                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitForVisible();

                    const initialValue = await this.sideMenu.getCompareCounterValue();
                    await this.sideMenu.closeMenu();

                    await this.browser.allure.runStep(
                        'Кликаем по значку сравнения на сниппете',
                        () => this.compareTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.browser.allure.runStep(
                        'Повторно кликаем по значку сравнения на сниппете',
                        () => this.compareTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitComparisonCounterVisible();

                    const finalValue = await this.sideMenu.getCompareCounterValue();

                    return this.expect(finalValue - initialValue).to.be.equal(0, 'Товар удалён из сравнения');
                },
            }),
        },
    },
});
