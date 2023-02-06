/**
 * @expFlag touch_smart-banner_10_21
 * @ticket MARKETFRONT-59009
 *
 * file
 */
import {makeCase, makeSuite} from 'ginny';

import Header from '@self/root/market/platform.touch/spec/page-objects/widgets/core/Header';
import Footer from '@self/root/market/platform.touch/spec/page-objects/Footer';
import {HammerInCut} from '@self/root/src/components/AppDistribution/HammerInCut/__pageObject';

export default makeSuite('Баннер после обратного скролла', {
    feature: 'Баннер после обратного скролла',
    environment: 'kadavr',
    id: 'marketfront-5300',
    issue: 'MARKETFRONT-70648',
    params: {
        prepareState: 'Функция определяющая состояние приложения',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                hammerInCut: () => this.createPageObject(HammerInCut),
            });

            await this.browser.setState('Tarantino.data.result', [bannerAfterReverseScrollMockCmsResponse]);

            await this.params.prepareState.call(this);

            // Виджет появляется после обратного скролла. Поэтому сначала делаем скролл вниз, а потом вверх.
            await this.browser.yaSlowlyScroll(Footer.root);
            await this.browser.yaSlowlyScroll(Header.root);
        },
        'Содержи корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что баннер отображается на странице',
                    () => this.hammerInCut.waitForVisible(1000)
                );

                await this.browser.allure.runStep(
                    'Проверяем текст заголовка',
                    () => this.hammerInCut.getTitleText()
                        .should.eventually.to.be.equal(
                            'Заказывайте в новом\nприложении Маркета\nи получайте бонусы'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст подзаголовка',
                    () => this.hammerInCut.getSubtitleText()
                        .should.eventually.to.be.equal(
                            'Установите приложение'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение',
                    () => this.hammerInCut.getBannerPictureSrc()
                        .should.eventually.to.be.equal(
                            'https://avatars.mds.yandex.net/get-marketcms/1534436/img-a50600b9-7f12-44f9-8641-0317dd4c5016.svg/orig'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст ссылки дистрибуции',
                    () => this.hammerInCut.getDistributionLinkText()
                        .should.eventually.to.be.equal(
                            'Открыть'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку дистрибуции',
                    () => this.hammerInCut.getDistributionLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'mpau249',
                                adj_campaign: 'Up_Scroll',
                                adj_adgroup: 'touch:product',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем текст ссылки на условия',
                    () => this.hammerInCut.getRulesLinkText()
                        .should.eventually.to.be.equal(
                            'Подробнее'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку на условия',
                    () => this.hammerInCut.getRulesLink()
                        .should.eventually.be.link({
                            hostname: 'yandex.ru',
                            pathname: '/legal/market_growing_cashback',
                        }, {
                            skipProtocol: true,
                        })
                );
            },
        }),
        'Закрывается при клике на ссылку дистрибуции': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что баннер отображается на странице',
                    () => this.hammerInCut.waitForVisible(1000)
                );

                // кликаем по ссылки дистрибуции
                await this.hammerInCut.distributionLinkClick();

                await this.browser.allure.runStep('Проверяем, что баннер не отображается на странице',
                    () => this.hammerInCut.waitForVisible(1000, true)
                );
            },
        }),
        'Закрывается при клике на крестик': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что баннер отображается на странице',
                    () => this.hammerInCut.waitForVisible(1000)
                );

                // кликаем по кнопке закрытия баннера
                await this.hammerInCut.clickCloseButton();

                await this.browser.allure.runStep('Проверяем, что баннер не отображается на странице',
                    () => this.hammerInCut.waitForVisible(1000, true)
                );
            },
        }),
    },
});

const bannerAfterReverseScrollMockCmsResponse = {
    content: {
        rows: [
            {
                entity: 'box',
                name: 'Grid24',
                props: {
                    type: 'row',
                    width: 'stretch',
                    layout: true,
                    grid: 1,
                },
                nodes: [
                    {
                        entity: 'box',
                        name: 'Grid24',
                        props: {
                            type: 'column',
                            layout: false,
                            width: 1,
                            position: 'default',
                            sticky: false,
                        },
                        nodes: [
                            {
                                name: 'BannerAfterReverseScroll',
                                wrapperProps: {
                                    shadow: 'none',
                                    background: {
                                        color: '$gray50',
                                    },
                                    paddings: {
                                        top: '5',
                                        left: '5',
                                        right: '5',
                                        bottom: '5',
                                    },
                                },
                                hideForRobots: false,
                                loadMode: 'default',
                                entity: 'widget',
                                props: {
                                    bannerPicture: {
                                        entity: 'picture',
                                        width: '48',
                                        height: '48',
                                        // eslint-disable-next-line max-len
                                        url: '//avatars.mds.yandex.net/get-marketcms/1534436/img-a50600b9-7f12-44f9-8641-0317dd4c5016.svg/orig',
                                        thumbnails: [],
                                    },
                                    vieType: 'hammer',
                                    hasCloseButton: true,
                                    title: {
                                        value: 'Заказывайте в новом<br/>приложении Маркета<br/>и получайте бонусы',
                                    },
                                    subtitle: {
                                        size: '150',
                                        value: 'Установите приложение',
                                    },
                                    rulesLink: {
                                        text: 'Подробнее',
                                        link: 'https://yandex.ru/legal/market_growing_cashback',
                                    },
                                    isCloseAfterDistributionLinkClick: true,
                                    isForceVisible: true,
                                    distributionLinkInfo: {
                                        text: 'Открыть',
                                        width: 'auto',
                                        theme: 'action',
                                        distributionLinkProps: {
                                            useAdjustLink: true,
                                            installId: '170960103216791154',
                                            adjustCampaign: 'Up_Scroll',
                                            adjustInstallId: 'mpau249',
                                        },
                                        size: 'sm',
                                    },
                                    zoneName: 'distribution-mp_distribution_banner_after_reverse_scroll',
                                },
                                epicModeForLazyLoad: 'default',
                                placeholder: 'SnippetScrollbox',
                                id: 111487287,
                            },
                        ],
                    },
                ],
            },
        ],
    },
    type: 'mp_distribution_banner_after_reverse_scroll',
    id: 190920,
};
