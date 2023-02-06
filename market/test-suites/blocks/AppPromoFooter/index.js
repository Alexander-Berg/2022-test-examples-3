import {makeCase, makeSuite} from 'ginny';

import Footer from '@self/platform/spec/page-objects/Footer';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {MiniInCut} from '@self/root/src/components/AppDistribution/MiniInCut/__pageObject';

export default makeSuite('Баннер дистрибуции в футере', {
    feature: 'Баннер дистрибуции в футере',
    issue: 'MARKETFRONT-70040',
    id: 'marketfront-5302',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                footer: () => this.createPageObject(Footer),
                miniInCut: () => this.createPageObject(MiniInCut, {
                    parent: this.footer,
                }),
            });

            await this.browser.setState('Tarantino.data.result', [appPromoFooterMockCmsResponse]);
            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);
        },
        'отображается в футере': makeCase({
            async test() {
                await this.miniInCut.isVisible()
                    .should.eventually.to.be.equal(true, 'Баннер должен быть виден');
            },
        }),
        'содержит корректный заголовок': makeCase({
            async test() {
                await this.miniInCut
                    .getTitleText()
                    .should.eventually.be.equal('Больше заказываете — больше выгода');
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
                    .should.eventually.be.equal('https://avatars.mds.yandex.net/get-marketcms/1779479/img-bf1a981a-42d1-4872-824d-2c15afe2e85f.svg/orig');
            },
        }),
        'ссылка дистрибуции': {
            'содержит корректный текст': makeCase({
                async test() {
                    await this.miniInCut
                        .getDistributionLinkText()
                        .should.eventually.be.equal('Открыть');
                },
            }),
            'содержит корректное значение': makeCase({
                async test() {
                    await this.miniInCut
                        .getDistributionLink()
                        .should.eventually.be.link({
                            hostname: 'nquw.adj.st',
                            pathname: '/',
                            query: {
                                adj_t: 'mpau249',
                                adj_campaign: 'Promo_Footer',
                                adj_adgroup: 'touch:index',
                            },
                        }, {
                            skipProtocol: true,
                        });
                },
            }),
        },
    },
});

const appPromoFooterMockCmsResponse = {
    'content': {
        'rows': [
            {
                'entity': 'box',
                'name': 'Grid24',
                'props': {
                    'type': 'row',
                    'width': 'stretch',
                    'layout': true,
                    'grid': 1,
                },
                'nodes': [
                    {
                        'entity': 'box',
                        'name': 'Grid24',
                        'props': {
                            'type': 'column',
                            'layout': false,
                            'width': 1,
                            'position': 'default',
                            'sticky': false,
                        },
                        'nodes': [
                            {
                                'name': 'DistributionInCut',
                                'wrapperProps': {
                                    'shadow': 'none',
                                    'background': {
                                        'color': '$white',
                                    },
                                    'margins': {
                                        'top': '2',
                                        'bottom': '5',
                                    },
                                    'borders': {
                                        'bottom': true,
                                        'color': '$gray100',
                                    },
                                    'paddings': {
                                        'bottom': '6',
                                    },
                                },
                                'hideForRobots': false,
                                'loadMode': 'default',
                                'entity': 'widget',
                                'props': {
                                    'bannerPicture': {
                                        'entity': 'picture',
                                        'width': '44',
                                        'height': '44',
                                        'url': '//avatars.mds.yandex.net/get-marketcms/1779479/img-bf1a981a-42d1-4872-824d-2c15afe2e85f.svg/orig',
                                        'thumbnails': [],
                                    },
                                    'vieType': 'mini',
                                    'title': {
                                        'value': 'Больше заказываете — больше выгода',
                                    },
                                    'subtitle': {
                                        'value': 'Установите приложение',
                                    },
                                    'isForceVisible': true,
                                    'distributionLinkInfo': {
                                        'text': 'Открыть',
                                        'width': 'auto',
                                        'theme': 'action',
                                        'distributionLinkProps': {
                                            'useAdjustLink': true,
                                            'installId': '675363262035941149',
                                            'adjustCampaign': 'Promo_Footer',
                                            'adjustInstallId': 'mpau249',
                                        },
                                        'size': 's',
                                    },
                                    'zoneName': 'appPromo',
                                },
                                'epicModeForLazyLoad': 'default',
                                'placeholder': 'SnippetScrollbox',
                                'id': 111303146,
                            },
                        ],
                    },
                ],
            },
        ],
    },
    'type': 'mp_footer_distribution_banner',
    'id': 188256,
};
