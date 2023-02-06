import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// pageObject
import AppPromoOnboardingMenuItem from '@self/root/src/components/AppPromoOnboardingMenuItem/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

const userMenuSuite = makeSuite('Плашка "500 баллов Плюса"', {
    params: {
        shouldBeShown: 'Должна ли показваться плашка',
    },
    story: {
        async test() {
            const {shouldBeShown} = this.params;
            // shouldBeShoown - false
            if (!shouldBeShown) {
                return this.browser.allure.runStep('Проверяем наличие плашки "500 баллов Плюс" в меню пользователя',
                    () => this.welcomeCashbackMenuItem.isVisible()
                        .should.eventually.to.be.equal(
                            shouldBeShown,
                            'Плашка "500 баллов Плюса" не должна отображаться'
                        )
                );
            }
            // shouldBeShoown - true
            await this.browser.allure.runStep(
                'Проверяем наличие плашки "500 баллов Плюс" в меню пользователя',
                () => this.welcomeCashbackMenuItem.isVisible()
                    .should.eventually.to.be.equal(
                        shouldBeShown,
                        'Плашка "500 баллов Плюса" должна отображаться'
                    )
            );
            await this.browser.allure.runStep(
                'Проверяем текст заголовка',
                () => this.welcomeCashbackMenuItem.getPrimaryText()
                    .should.eventually.to.be.equal(
                        '500 баллов Плюса',
                        'Текст должен содержать "500 баллов Плюса"'
                    )
            );
            return this.browser.allure.runStep(
                'При нажатии ссылка ведет в корректное место',
                async () => this.menuItemLink.getHref()
                    .should.eventually.to.be.link({
                        hostname: 'redirect.appmetrica.yandex.com',
                        pathname: '/serve/963953261184838755',
                    }, {
                        skipProtocol: true,
                    })
            );
        },
    },
});

async function prepareState({isPromoAvailable}) {
    await this.browser.setState('Loyalty.collections.perks', isPromoAvailable ? [{
        freeDelivery: false,
        purchased: true,
        type: 'WELCOME_CASHBACK',
        cashback: 500,
        threshold: 3500,
    }] : []);

    await this.browser.yaOpenPage('touch:index');

    return this.header.clickMenuTrigger()
        .then(() => this.sideMenu.waitForVisible());
}

export default makeSuite('Плашка "500 баллов Плюса"', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-37778',
    feature: 'РК-500',
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                welcomeCashbackMenuItem: () => this.createPageObject(SpecialMenuItem, {
                    root: AppPromoOnboardingMenuItem.root,
                }),
                menuItemLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackMenuItem,
                }),
            });
        },
        'Акция доступна': prepareSuite(userMenuSuite, {
            meta: {
                id: 'marketfront-4458',
            },
            params: {
                shouldBeShown: true,
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {isPromoAvailable: true});
                },
            },
        }),
        'Акция не доступна': prepareSuite(userMenuSuite, {
            meta: {
                id: 'marketfront-4765',
            },
            params: {
                shouldBeShown: false,
            },
            hooks: {
                async beforeEach() {
                    await prepareState.call(this, {isPromoAvailable: false});
                },
            },
        }),
    }),
});
