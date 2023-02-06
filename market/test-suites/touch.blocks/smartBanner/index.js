/**
 * @expFlag touch_smart-banner_10_21
 * @ticket MARKETFRONT-59009
 *
 * file
 */
import {makeCase, makeSuite} from 'ginny';

import Header from '@self/root/market/platform.touch/spec/page-objects/widgets/core/Header';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {MiniInCut} from '@self/root/src/components/AppDistribution/MiniInCut/__pageObject';

export default makeSuite('Смартбаннер дистрибуции приложения', {
    feature: 'Смартбаннер',
    issue: 'MARKETFRONT-60686',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                miniInCut: () => this.createPageObject(MiniInCut),
            });

            await this.browser.setState('Tarantino.data.result', [smartBannerMockCmsResponse]);
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);
            await this.browser.execute(() => {
                // Страница должна иметь достаточную высоту чтобы мы могли её проскроллить
                // до появления баннера. Вручную выставляем высоту, чтобы не зависеть
                // от других виджетов.
                document.querySelector('body').style.height = '1000px';
            });

            // Виджет появляется по скроллу вниз.
            await this.browser.yaSlowlyScroll();
        },

        'появляется при скролле вниз': makeCase({
            id: 'marketfront-5248',
            // Скролл происходит в `beforeEach`.
            async test() {
                await this.miniInCut.waitForVisible(1000);
            },
        }),
        'скрывается при скролле наверх': makeCase({
            id: 'marketfront-5248',
            async test() {
                await this.browser.yaSlowlyScroll(Header.root);
                await this.miniInCut.waitForVisible(1000, true);
            },
        }),
        'содержит корректный заголовок': makeCase({
            id: 'marketfront-5233',
            async test() {
                await this.miniInCut
                    .getTitleText()
                    .should.eventually.be.equal('В приложении еще выгоднее');
            },
        }),
        'содержит корректный подзаголовок': makeCase({
            async test() {
                await this.miniInCut
                    .getSubtitleText()
                    .should.eventually.be.equal('Установите приложение');
            },
        }),
        'содержит корректное изображение': makeCase({
            async test() {
                await this.miniInCut
                    .getBannerPictureSrc()
                    .should.eventually.be.equal('https://avatars.mds.yandex.net/get-marketcms/879900/img-4526fb75-5cef-4acf-9798-6af7ab25040d.svg/orig');
            },
        }),
        'закрывается по клику на кнопку закрытия': makeCase({
            id: 'marketfront-5246',
            async test() {
                await this.miniInCut.clickCloseButton();
                await this.miniInCut
                    .isVisible()
                    .should.eventually.be.equal(false, 'Баннер скрыт');
            },
        }),
        'ссылка дистрибуции': {
            'содержит корректный текст': makeCase({
                id: 'marketfront-5247',
                async test() {
                    await this.miniInCut
                        .getDistributionLinkText()
                        .should.eventually.be.equal('Открыть');
                },
            }),
            'содержит корректное значение': makeCase({
                id: 'marketfront-5247',
                async test() {
                    await this.miniInCut
                        .getDistributionLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: '1crfd5y',
                                adj_campaign: 'Smart_Baner2',
                                adj_adgroup: 'touch:index',
                            },
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
            'закрывается при клике': makeCase({
                async test() {
                    // кликаем по ссылки дистрибуции
                    await this.miniInCut.distributionLinkClick();

                    await this.browser.allure.runStep('Проверяем, что баннер не отображается на странице',
                        () => this.miniInCut.waitForVisible(1000, true)
                    );
                },
            }),
        },
    },
});

const smartBannerMockCmsResponse = {
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
                                id: 110822740,
                                entity: 'widget',
                                name: 'SmartBanner',
                                loadMode: 'default',
                                hideForRobots: false,
                                epicModeForLazyLoad: 'default',
                                placeholder: 'SnippetScrollbox',
                                wrapperProps: {
                                    shadow: 'none',
                                    background: {
                                        color: '$white',
                                    },
                                    borders: {
                                        bottom: true,
                                        color: '$gray400',
                                    },
                                    paddings: {
                                        top: '2',
                                        left: '3',
                                        right: '3',
                                        bottom: '2',
                                    },
                                },
                                props: {
                                    distributionLinkInfo: {
                                        distributionLinkProps: {
                                            useAdjustLink: true,
                                            adjustInstallId: '1crfd5y',
                                            adjustCampaign: 'Smart_Baner2',
                                            installId: '27039001273718615',
                                        },
                                        text: 'Открыть',
                                        theme: 'action',
                                        size: 's',
                                        width: 'auto',
                                    },
                                    title: {
                                        size: '150',
                                        value: 'В приложении еще выгоднее',
                                    },
                                    subtitle: {
                                        size: '150',
                                        value: 'Установите приложение',
                                    },
                                    bannerPicture: {
                                        entity: 'picture',
                                        width: '30',
                                        height: '30',
                                        // eslint-disable-next-line max-len
                                        url: '//avatars.mds.yandex.net/get-marketcms/879900/img-4526fb75-5cef-4acf-9798-6af7ab25040d.svg/orig',
                                        thumbnails: [],
                                    },
                                    isForceVisible: true,
                                    vieType: 'mini',
                                    zoneName: 'smartBanner',
                                    hasCloseButton: true,
                                    isCloseAfterDistributionLinkClick: true,
                                },
                            },
                        ],
                    },
                ],
            },
        ],
    },
    type: 'mp_header_distribution_smart_banner',
    id: 183210,
};
