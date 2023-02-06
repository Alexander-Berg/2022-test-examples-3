import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Header} header
 * @param {PageObject.SideMenu} sideMenu
 * @param {PageObject.WishlistEntrypoint} wishlistEntrypoint
 * @param {PageObject.WishlistTumbler} wishlistTumbler
 * @param {PageObject.Notification} notification
 */
export default makeSuite('Кнопка «Добавить в избранное»', {
    story: {
        'По умолчанию': {
            'присутствует на сниппете': makeCase({
                id: 'm-touch-3009',
                issue: 'MOBMARKET-13382',
                async test() {
                    const isExists = this.wishlistTumbler.isExisting();

                    return this.expect(isExists).to.be.equal(true, 'Кнопка присутствует на сниппете');
                },
            }),
        },
        'При клике': {
            'добавляет в избранное': makeCase({
                id: 'm-touch-3010',
                issue: 'MOBMARKET-13382',
                async test() {
                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitForVisible();

                    const initialValue = await this.wishlistEntrypoint.getCounterValue();
                    await this.sideMenu.closeMenu();

                    await this.browser.allure.runStep(
                        'Кликаем по значку вишлиста на сниппете',
                        () => this.wishlistTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.header.clickMenuTrigger()
                        .then(() => this.sideMenu.waitForVisible());

                    const updatedValue = await this.wishlistEntrypoint.getCounterValue();

                    return this.expect(updatedValue - initialValue).to.be.equal(1, 'Товар добавлен в избранное');
                },
            }),
            'уведомление ведёт в избранное': makeCase({
                id: 'm-touch-3014',
                issue: 'MOBMARKET-13382',
                async test() {
                    await this.browser.allure.runStep(
                        'Кликаем по значку вишлиста на сниппете',
                        () => this.wishlistTumbler.click()
                    );

                    await this.notification.waitForText('Товар добавлен в избранное. Нажмите, чтобы перейти к списку.');
                    await this.notification.clickText();

                    return this.browser.allure.runStep(
                        'Проверяем URL после перехода',
                        () => this.browser.yaParseUrl().should.eventually.be.link({
                            pathname: '/my/wishlist',
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
            'удаляет из избранного': makeCase({
                id: 'm-touch-3013',
                issue: 'MOBMARKET-13382',
                async test() {
                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitForVisible();

                    const initialValue = await this.wishlistEntrypoint.getCounterValue();
                    await this.sideMenu.closeMenu();

                    await this.browser.allure.runStep(
                        'Кликаем по значку вишлиста на сниппете',
                        () => this.wishlistTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.browser.allure.runStep(
                        'Повторно кликаем по значку вишлиста на сниппете',
                        () => this.wishlistTumbler.click()
                    );
                    await this.notification.waitForNotificationVisible();
                    await this.notification.closeNotification();
                    await this.notification.waitForNotificationHidden();

                    await this.header.clickMenuTrigger();
                    await this.sideMenu.waitForVisible();

                    const finalValue = await this.wishlistEntrypoint.getCounterValue();

                    return this.expect(finalValue - initialValue).to.be.equal(0, 'Товар удалён из избранного');
                },
            }),
        },
    },
});
