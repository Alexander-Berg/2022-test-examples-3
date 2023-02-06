import {makeSuite, mergeSuites, makeCase} from 'ginny';

// pageObject
import AppPromoOnboardingMenuItem from '@self/root/src/components/AppPromoOnboardingMenuItem/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import WelcomeCashbackPopup from '@self/root/src/widgets/content/WelcomeCashbackPopup/components/View/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import Title from '@self/root/src/uikit/components/Title/__pageObject';
import {welcomeCashbackPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

async function preparePage() {
    await this.browser.setState('Loyalty.collections.perks', [welcomeCashbackPerk]);
    await this.browser.yaOpenPage('market:index');
    await this.headerNav.clickOpen();
    await this.menuItemLink.click();
    await this.welcomeCashbackPopup.waitForVisible(3000);
    await this.welcomeCashbackPopup.isVisible()
        .should.eventually.to.be.equal(
            true,
            'Попап с информации об акции должен быть открытым'
        );
}

export default makeSuite('Попап "500 баллов Плюса".', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-45299',
    feature: 'РК-500',
    id: 'marketfront-4462',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                welcomeCashbackMenuItem: () => this.createPageObject(SpecialMenuItem, {
                    root: AppPromoOnboardingMenuItem.root,
                }),
                menuItemLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackMenuItem,
                }),
                welcomeCashbackPopup: () => this.createPageObject(PopupBase, {
                    root: WelcomeCashbackPopup.root,
                }),
                popupTitle: () => this.createPageObject(Title, {
                    parent: this.welcomeCashbackPopup.content,
                }),
                popupPlusLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackPopup.content,
                    root: '[data-auto="plus-link"]',
                }),
                popupRulesLink: () => this.createPageObject(Link, {
                    parent: this.welcomeCashbackPopup.content,
                    root: '[data-auto="rules-link"]',
                }),
            });
            await preparePage.call(this);
        },
        'По умолчанию': {
            'текст заголовка попапа корректный': makeCase({
                test() {
                    return this.popupTitle.getTitle()
                        .should.eventually.to.be.equal(
                            '500 баллов Плюса за первый заказ в приложении от 3500 ₽',
                            'Текст должен содержать 500 баллов Плюса за первый заказ в приложении от 3500 ₽'
                        );
                },
            }),
            'кнопка "Читать про Плюс" содержит корректную ссылку': makeCase({
                test() {
                    return this.popupPlusLink.getHref()
                        .should.eventually.to.be.link({
                            hostname: 'plus.yandex.ru',
                            pathname: '/',
                            query: {
                                utm_source: 'market',
                                utm_medium: 'banner',
                                utm_campaign: 'MSCAMP-77',
                                utm_term: 'src_market',
                                utm_content: '500_cb_promo',
                                message: 'market',
                            },
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
            'кнопка "Условия акции" содержит корректную ссылку': makeCase({
                test() {
                    return this.popupRulesLink.getHref()
                        .should.eventually.to.be.link({
                            hostname: 'yandex.ru',
                            pathname: '/legal/market_welcome_cashback',
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    }),
});
