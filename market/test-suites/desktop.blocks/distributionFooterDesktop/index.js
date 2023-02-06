import {makeCase, makeSuite} from 'ginny';

import Footer from '@self/root/market/platform.desktop/spec/page-objects/footer-market';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    DistributionFooterDesktop,
} from '@self/root/src/widgets/parts/AppDistribution/DistributionFooterDesktop/component/View/__pageObject';
import {GeneratedQrCode} from '@self/root/src/widgets/content/GeneratedQrCode/components/View/__pageObject';
import {APPSTORE, GOOGLEPLAY, APPGALLERY} from '@self/root/src/constants/imageUrls';

export default makeSuite('Баннер дистрибуции в футере', {
    feature: 'Баннер дистрибуции в футере',
    issue: 'MARKETFRONT-70803',
    environment: 'kadavr',
    id: 'marketfront-5297',
    story: {
        async beforeEach() {
            this.setPageObjects({
                footer: () => this.createPageObject(Footer),
                distributionFooter: () => this.createPageObject(DistributionFooterDesktop, {
                    parent: this.footer,
                }),
                qrCode: () => this.createPageObject(GeneratedQrCode, {
                    parent: this.distributionFooter,
                }),
            });

            await this.browser.setState('Tarantino.data.result', [appPromoFooterMockCmsResponse]);
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);
        },
        'Содержит корректный контент': makeCase({
            async test() {
                await this.browser.allure.runStep('Проверяем, что баннер отображается на странице',
                    () => this.distributionFooter.isVisible()
                );

                await this.browser.allure.runStep(
                    'Проверяем текст заголовка',
                    () => this.distributionFooter.getTitleText()
                        .should.eventually.to.be.equal(
                            'Заказывайте в новом приложении\nМаркета и получайте бонусы'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст подзаголовка',
                    () => this.distributionFooter.getSubtitleText()
                        .should.eventually.to.be.equal(
                            'Установите приложение'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст ссылки на условия',
                    () => this.distributionFooter.getRulesLinkText()
                        .should.eventually.to.be.equal(
                            'Подробнее'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку на условия',
                    () => this.distributionFooter.getRulesLink()
                        .should.eventually.be.link({
                            hostname: 'yandex.ru',
                            pathname: '/legal/market_growing_cashback',
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение AppStore',
                    () => this.distributionFooter.getAppStoreImageSrc()
                        .should.eventually.to.be.equal(`https:${APPSTORE}`)
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку в AppStore',
                    () => this.distributionFooter.getAppStoreLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'mpau249',
                                adj_campaign: 'app_promo_footer_desktop',
                                adj_fallback: 'https://apps.apple.com/ru/app/id1369890634',
                                adj_redirect: 'https://apps.apple.com/ru/app/id1369890634',
                                adj_redirect_macos: 'https://apps.apple.com/ru/app/id1369890634',
                                adj_adgroup: 'market:index',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение GooglePlay',
                    () => this.distributionFooter.getGooglePlayImageSrc()
                        .should.eventually.to.be.equal(`https:${GOOGLEPLAY}`)
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку в GooglePlay',
                    () => this.distributionFooter.getGooglePlayLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'mpau249',
                                adj_campaign: 'app_promo_footer_desktop',
                                adj_fallback: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                                adj_redirect: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                                adj_adgroup: 'GP',
                                adj_redirect_macos: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение AppGallery',
                    () => this.distributionFooter.getAppGalleryImageSrc()
                        .should.eventually.to.be.equal(`https:${APPGALLERY}`)
                );

                await this.browser.allure.runStep(
                    'Проверяем ссылку в AppGallery',
                    () => this.distributionFooter.getAppGalleryLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'mpau249',
                                adj_campaign: 'app_promo_footer_desktop',
                                adj_fallback: 'https://appgallery.huawei.com/app/C101134157',
                                adj_redirect: 'https://appgallery.huawei.com/app/C101134157',
                                adj_adgroup: 'AG',
                                adj_redirect_macos: 'https://appgallery.huawei.com/app/C101134157',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );

                await this.browser.allure.runStep(
                    'Проверяем изображение в баннере',
                    () => this.distributionFooter.getBannerPictureSrc()
                        .should.eventually.to.be.equal(
                            'https://avatars.mds.yandex.net/get-marketcms/475644/img-0bc7026d-fa47-4568-a894-b193cc3fe370.png/optimize'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем текст возле QR кода',
                    () => this.qrCode.getText()
                        .should.eventually.to.be.equal(
                            'Наведите камеру\nна QR-код, чтобы\nскачать'
                        )
                );

                await this.browser.allure.runStep(
                    'Проверяем QR код',
                    () => this.qrCode.getQrCodePictureSrc()
                        .should.eventually.be.link({
                            hostname: 'disk.yandex.net',
                            pathname: '/qr/',
                            query: {
                                clean: '1',
                                text: 'https://nquw.adj.st/?adj_t=mpau249',
                                adj_campaign: 'app_promo_footer_desktop_qr_code',
                                adj_adgroup: 'market:index',
                                format: 'svg',
                            },
                        }, {
                            skipProtocol: true,
                        })
                );
            },
        }),
    },
});

/* eslint-disable max-len */
const appPromoFooterMockCmsResponse = {
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
                                id: 111454189,
                                entity: 'widget',
                                name: 'DistributionFooterDesktop',
                                loadMode: 'default',
                                hideForRobots: false,
                                epicModeForLazyLoad: 'default',
                                placeholder: 'SnippetScrollbox',
                                wrapperProps: {
                                    borders: {
                                        bottom: true,
                                        color: '$gray500',
                                    },
                                    background: {
                                        color: '$gray50',
                                    },
                                    shadow: 'none',
                                },
                                props: {
                                    qrCodeInfo: {
                                        distributionLinkProps: {
                                            useAdjustLink: true,
                                            adjustInstallId: 'mpau249',
                                            adjustCampaign: 'app_promo_footer_desktop_qr_code',
                                        },
                                        size: {
                                            height: 100,
                                            width: 100,
                                        },
                                        qrCodeBackground: {
                                            color: '$white',
                                            padding: '5px 5px 2px',
                                        },
                                        qrCodeTextParams: {
                                            color: '$gray700',
                                            size: '300',
                                            value: 'Наведите камеру<br/>на QR-код, чтобы<br/>скачать',
                                        },
                                        direction: 'column',
                                    },
                                    appStoreDistributionLinkProps: {
                                        useAdjustLink: true,
                                        adjustInstallId: 'mpau249',
                                        adjustCampaign: 'app_promo_footer_desktop',
                                        adjustFallback: 'https://apps.apple.com/ru/app/id1369890634',
                                        adjustRedirect: 'https://apps.apple.com/ru/app/id1369890634',
                                        adjustRedirectMacos: 'https://apps.apple.com/ru/app/id1369890634',
                                    },
                                    googlePlayDistributionLinkProps: {
                                        useAdjustLink: true,
                                        adjustInstallId: 'mpau249',
                                        adjustCampaign: 'app_promo_footer_desktop',
                                        adjustAdgroup: 'GP',
                                        adjustFallback: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                                        adjustRedirect: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                                        adjustRedirectMacos: 'https://play.google.com/store/apps/details?id=ru.beru.android',
                                    },
                                    appGalleryDistributionLinkProps: {
                                        useAdjustLink: true,
                                        adjustInstallId: 'mpau249',
                                        adjustCampaign: 'app_promo_footer_desktop',
                                        adjustAdgroup: 'AG',
                                        adjustFallback: 'https://appgallery.huawei.com/app/C101134157',
                                        adjustRedirect: 'https://appgallery.huawei.com/app/C101134157',
                                        adjustRedirectMacos: 'https://appgallery.huawei.com/app/C101134157',
                                    },
                                    title: {
                                        value: 'Заказывайте в новом приложении<br>Маркета и получайте бонусы',
                                    },
                                    subtitle: {
                                        size: '150',
                                        value: 'Установите приложение',
                                    },
                                    rulesLink: {
                                        text: 'Подробнее',
                                        link: 'https://yandex.ru/legal/market_growing_cashback',
                                    },
                                    bannerPicture: {
                                        entity: 'picture',
                                        width: '180',
                                        height: '209',
                                        url: '//avatars.mds.yandex.net/get-marketcms/475644/img-0bc7026d-fa47-4568-a894-b193cc3fe370.png/optimize',
                                        thumbnails: [],
                                    },
                                    isForceVisible: true,
                                    zoneName: 'appPromoDesktop',
                                    bannerPictureWrapperProps: {
                                        margins: {
                                            top: '5',
                                        },
                                        shadow: 'none',
                                    },
                                    leftColumnWrapperProps: {
                                        margins: {
                                            right: '11',
                                        },
                                        shadow: 'none',
                                    },
                                    qrCodeWrapperProps: {
                                        margins: {
                                            top: '6',
                                            left: '9',
                                        },
                                        shadow: 'none',
                                    },
                                },
                            },
                        ],
                    },
                ],
            },
        ],
    },
    type: 'mp_footer_distribution_banner',
    id: 190460,
};
/* eslint-enable max-len */
