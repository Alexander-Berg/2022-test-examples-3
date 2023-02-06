import {makeSuite, makeCase} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    growingCashbackPerk,
    growingCashbackFullRewardPerk,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';
// pageObject
import GrowingCashbackMenuItem
    from '@self/root/src/widgets/content/GrowingCashbackMenuItem/components/View/__pageObject';
import SpecialMenuItem from '@self/root/src/components/SpecialMenuItem/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';

export const userMenuSuite = makeSuite('Плашка "Растущий кешбэк"', {
    params: {
        shouldBeShown: 'Должна ли показываться плашка',
        isPromoAvailable: 'Доступна ли акция для пользователя',
        isGotFullReward: 'Набрал ли пользователь максимально количество баллов',
        prepareState: 'Функция определяющая состояние приложения под конкретный кейс',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                growingCashbackMenuItem: () => this.createPageObject(SpecialMenuItem, {
                    root: GrowingCashbackMenuItem.root,
                }),
                menuItemLink: () => this.createPageObject(Link, {
                    parent: this.growingCashbackMenuItem,
                }),
            });

            let perks = [];

            if (this.params.isPromoAvailable) {
                perks = this.params.isGotFullReward ? [growingCashbackFullRewardPerk] : [growingCashbackPerk];
            }

            await this.browser.setState('Loyalty.collections.perks', perks);
            await this.browser.setState('S3Mds.files', {
                '/toggles/all_growing-cashback-web.json': {
                    id: 'all_growing-cashback-web',
                    value: true,
                },
            });

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);

            await this.params.prepareState.call(this);
        },
        'Пункт меню': makeCase({
            async test() {
                const {shouldBeShown} = this.params;

                await this.browser.allure.runStep('Проверяем наличие плашки "Растущий кешбэк" в меню пользователя',
                    () => this.growingCashbackMenuItem.isVisible()
                        .should.eventually.to.be.equal(
                            shouldBeShown,
                            `Плашка "Растущий кешбэк" ${shouldBeShown ? '' : 'не'} должна отображаться`
                        )
                );

                if (shouldBeShown) {
                    await this.browser.allure.runStep(
                        'Проверяем содержание текста заголовка',
                        () => this.growingCashbackMenuItem.isPrimaryTextVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Заголовок должен содержаться в плашке'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст заголовка',
                        () => this.growingCashbackMenuItem.getPrimaryText()
                            .should.eventually.to.be.equal(
                                '1 550 баллов Плюса',
                                'Заголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем содержание текста подзаголовка',
                        () => this.growingCashbackMenuItem.isSecondaryTextVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Подзаголовок должен содержаться в плашке'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем текст подзаголовка',
                        () => this.growingCashbackMenuItem.getSecondaryText()
                            .should.eventually.to.be.equal(
                                'За первые 3 заказа\nв приложении от 3 500 ₽',
                                'Подзаголовок должен содержать корректный текст'
                            )
                    );

                    await this.browser.allure.runStep(
                        'Проверяем ссылку на лэндинг',
                        () => this.menuItemLink.getHref()
                            .should.eventually.to.be.link({
                                pathname: '/special/growing-cashback',
                            }, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                    );
                }
            },
        }),
    },
});
