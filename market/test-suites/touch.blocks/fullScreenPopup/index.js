/**
 * @expFlag touch_smart-banner_10_21
 * @ticket MARKETFRONT-59009
 *
 * file
 */
import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {FullScreenPopup} from '@self/root/src/widgets/parts/AppDistribution/FullScreenPopup/components/View/__pageObject';

export default makeSuite('Полноэкранный попап дистрибуции', {
    feature: 'Полноэкранный попап дистрибуции',
    environment: 'kadavr',
    issue: 'MARKETFRONT-73197',
    story: {
        async beforeEach() {
            this.setPageObjects({
                fullScreenPopup: () => this.createPageObject(FullScreenPopup),
            });

            await this.browser.setState('Tarantino.data.result', [fullScreenPopupMockCmsResponse]);
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);
        },
        'Содержи корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что попап отображается на странице',
                    () => this.fullScreenPopup.waitForVisible()
                );

                await this.browser.allure.runStep(
                    'Проверяем текст заголовка',
                    () => this.fullScreenPopup.getTitleText()
                        .should.eventually.to.be.equal(
                            'Скидки до 70%\nи кешбэк баллами от 5%'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст подзаголовка',
                    () => this.fullScreenPopup.getSubtitleText()
                        .should.eventually.to.be.equal(
                            'Пользуйтесь выгодой в приложении'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст дисклеймера',
                    () => this.fullScreenPopup.getDisclaimerText()
                        .should.eventually.to.be.equal(
                            'Подробнее market.yandex.ru/special/NEW_500'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение',
                    () => this.fullScreenPopup.getPictureUrl()
                        .should.eventually.to.be.equal(
                            'url("https://avatars.mds.yandex.net/get-marketcms/1357599/img-5916146e-f986-431b-8b57-3b1c5bca1752.jpeg/optimize")'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст ссылки дистрибуции',
                    () => this.fullScreenPopup.getDistributionLinkText()
                        .should.eventually.to.be.equal(
                            'Открыть'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку дистрибуции',
                    () => this.fullScreenPopup.getDistributionLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'puj18z7',
                                adj_campaign: 'Inhouse_YM_Portal_Touch_All_ACQ_FullScreen-White',
                                adj_adgroup: 'touch:index',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем текст кнопки продолжения работы в web',
                    () => this.fullScreenPopup.getContinueButtonText()
                        .should.eventually.to.be.equal(
                            'Не сейчас'
                        )
                );
            },
        }),
        'Закрывается при клике на ссылку дистрибуции': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что попап отображается на странице',
                    () => this.fullScreenPopup.waitForVisible()
                );

                // кликаем по ссылки дистрибуции
                await this.fullScreenPopup.distributionLinkClick();

                await this.browser.allure.runStep('Проверяем, что попап не отображается на странице',
                    () => this.fullScreenPopup.waitForVisible(1000, true)
                );
            },
        }),
        'Закрывается при клике на крестик': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что попап отображается на странице',
                    () => this.fullScreenPopup.waitForVisible()
                );

                // кликаем по кнопке закрытия попапа
                await this.fullScreenPopup.clickCloseButton();

                await this.browser.allure.runStep('Проверяем, что попап не отображается на странице',
                    () => this.fullScreenPopup.waitForVisible(1000, true)
                );
            },
        }),
        'Закрывается при клике на кнопку продолжения работы в web': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что попап отображается на странице',
                    () => this.fullScreenPopup.waitForVisible()
                );

                // кликаем по кнопке закрытия попапа
                await this.fullScreenPopup.continueButtonClick();

                await this.browser.allure.runStep('Проверяем, что попап не отображается на странице',
                    () => this.fullScreenPopup.waitForVisible(1000, true)
                );
            },
        }),
    },
});

const fullScreenPopupMockCmsResponse = {
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
                                id: 112500154,
                                entity: 'widget',
                                name: 'FullScreenPopup',
                                loadMode: 'default',
                                hideForRobots: false,
                                epicModeForLazyLoad: 'default',
                                placeholder: 'SnippetScrollbox',
                                props: {
                                    distributionLinkInfo: {
                                        distributionLinkProps: {
                                            useAdjustLink: true,
                                            adjustInstallId: 'puj18z7',
                                            adjustCampaign: 'Inhouse_YM_Portal_Touch_All_ACQ_FullScreen-White',
                                            installId: '459384576389730510',
                                        },
                                        text: 'Открыть',
                                        theme: 'action',
                                        size: 'm',
                                        width: 'auto',
                                        pin: 'circle-circle',
                                    },
                                    continueButton: {
                                        text: 'Не сейчас',
                                        theme: 'pseudo',
                                        size: 'm',
                                    },
                                    title: {
                                        mediaParams: [
                                            {
                                                styleParams: {
                                                    size: '550',
                                                },
                                                minWidth: '375px',
                                            },
                                            {
                                                styleParams: {
                                                    size: '700',
                                                    margin: '0 0 20px',
                                                },
                                                minWidth: '720px',
                                            },
                                        ],
                                        styleParams: {
                                            margin: '0 0 8px',
                                            textAlign: 'center',
                                        },
                                        value: 'Скидки до 70%<br>и кешбэк баллами от 5%',
                                    },
                                    subtitle: {
                                        mediaParams: [
                                            {
                                                styleParams: {
                                                    size: '500',
                                                },
                                                minWidth: '720px',
                                            },
                                            {
                                                styleParams: {
                                                    size: '400',
                                                },
                                                minWidth: '375px',
                                            },
                                        ],
                                        styleParams: {
                                            margin: '0 0 8px',
                                            textAlign: 'center',
                                        },
                                        value: 'Пользуйтесь выгодой в приложении',
                                    },
                                    disclaimer: {
                                        styleParams: {
                                            margin: '0 0 8px',
                                            color: '$gray600',
                                        },
                                        value: 'Подробнее market.yandex.ru/special/NEW_500',
                                    },
                                    bannerPicture: {
                                        entity: 'picture',
                                        width: '1024',
                                        height: '1024',
                                        url: '//avatars.mds.yandex.net/get-marketcms/1357599/img-5916146e-f986-431b-8b57-3b1c5bca1752.jpeg/optimize',
                                        thumbnails: [],
                                    },
                                    isForceVisible: true,
                                    zoneName: 'promoAppFullscreen',
                                    infoContentWrapperProps: {
                                        shadow: 'none',
                                        background: {
                                            color: '$white',
                                        },
                                        margins: {
                                            top: 'n-6',
                                        },
                                        borders: {
                                            roundedTop: '6',
                                            roundedBottom: '6',
                                        },
                                        paddings: {
                                            top: '6',
                                            bottom: '5',
                                            left: '5',
                                            right: '5',
                                        },
                                    },
                                    infoContentHeight: '40vh',
                                    cookieLifetime: 1,
                                    isRounded: true,
                                },
                            },
                        ],
                    },
                ],
            },
        ],
    },
    type: 'mp_full_screen_distribution_popup',
    id: 199096,
};

