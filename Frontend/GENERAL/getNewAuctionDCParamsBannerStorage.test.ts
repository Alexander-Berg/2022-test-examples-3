import { AUTO_VIDEO_URL, CALL_TRACKING_URL_PREFIX } from '../../const';
import { BSMetaVideoAdDCParamsBannerStorage } from '../../typings';
import { AuctionDCParams } from '../../VASTAdCreator/getAuctionDCParams/getAuctionDCParams';
import { getNewAuctionDCParamsBannerStorage } from './getNewAuctionDCParamsBannerStorage';

describe('[VASTAdCreatorBannerStorage] getNewAuctionDCParamsBannerStorage', () => {
    it('should return correct value', () => {
        const auctionDCParams: BSMetaVideoAdDCParamsBannerStorage = {
            creative_params: {
                crypta_user_age: '2',
                crypta_user_gender: '1',
            },
            data_params: {
                '123': {
                    bs_data: {
                        sad: 'sad',
                        targetUrl: '',
                        domain: '',
                        count_links: {
                            empty: 'bs_data.count_links.empty',
                            abuseUrl: 'bs_data.count_links.abuseUrl',
                            tracking: '',
                        },
                        resource_links: {
                            direct_data: {
                                targetUrl: 'bs_data.resource_links.direct_data.targetUrl',
                                assets: {
                                    button: {
                                        href: 'bs_data.resource_links.direct_data.assets.button.href',
                                    },
                                },
                            },
                        },
                        actionButton: 'bs_data.actionButton',
                        bannerFlags: 'bs_data.bannerFlags',
                        bannerLang: 'bs_data.bannerLang',
                        adId: 'bs_data.adId',
                        hitLogId: 'bs_data.hitLogId',
                        impId: 'bs_data.impId',
                    },
                    direct_data: {
                        targetUrl: 'direct_data.targetUrl',
                        domain: 'direct_data.domain',
                        warning: 'direct_data.warning',
                        age: 'direct_data.age',
                        title: 'direct_data.title',
                        body: 'direct_data.body',
                        green_url_text_prefix: 'direct_data.green_url_text_prefix',
                        green_url_text_suffix: 'direct_data.green_url_text_suffix',
                        assets: {
                            button: {
                                key: 'direct_data.assets.button.key',
                                caption: 'direct_data.assets.button.caption',
                            },
                            logo: {
                                format: 'logoFormat',
                                someLogoKey: 'someLogoKey',
                            },
                        },
                        faviconSizes: {
                            w: 100,
                            h: 200,
                        },
                        admetrica: {
                            someAdmetricaKey: 'someAdmetricaKey',
                        },
                        tnsId: 'direct_data.tnsId',
                        trackers: 'direct_data.trackers',
                    },
                },
            },
        };
        const expectedResult: AuctionDCParams = {
            sad: 'sad',
            creative_params: {
                crypta_user_gender: '1',
                crypta_user_age: '2',
            },
            data_params: {
                'bs_data.adId': {
                    target_url: 'direct_data.targetUrl',
                    count: 'bs_data.count_links.empty',
                    click_url: {
                        text_name: 'bs_data.resource_links.direct_data.targetUrl',
                        action_button: 'bs_data.actionButton',
                    },
                    text: {
                        banner_flags: 'bs_data.bannerFlags',
                        domain: 'direct_data.domain',
                        lang: 'bs_data.bannerLang',
                        warning: 'direct_data.warning',
                        age: 'direct_data.age',
                        title: 'direct_data.title',
                        body: 'direct_data.body',
                        green_url_text_prefix: 'direct_data.green_url_text_prefix',
                        green_url_text_suffix: 'direct_data.green_url_text_suffix',
                        dynamic_disclaimer: '1',
                    },
                    assets: {
                        button: {
                            key: 'direct_data.assets.button.key',
                            caption: 'direct_data.assets.button.caption',
                            href: 'bs_data.resource_links.direct_data.assets.button.href',
                        },
                        logo: {
                            logoFormat: {
                                someLogoKey: 'someLogoKey',
                                format: undefined,
                            },
                        },
                    },
                    object_id: 'bs_data.adId',
                    unmoderated: {
                        secondTitle: undefined,
                        geoDistance: undefined,
                        region: undefined,
                        workingTime: undefined,
                        telNum: undefined,
                        metro: undefined,
                        liked: undefined,
                        sitelinks: undefined,
                        images: undefined,
                        addInfo: undefined,
                        targetUrl: undefined,
                        S2SEnabled: undefined,
                        CTUrl: undefined,
                        isCTEnabled: undefined,
                        CTDefaultNumber: undefined,
                        punyDomain: 'direct_data.domain',
                        faviconWidth: '100',
                        faviconHeight: '200',
                        measurers: {
                            admetrica: {
                                someAdmetricaKey: 'someAdmetricaKey',
                                sessionId: 'bs_data.adId:bs_data.hitLogId',
                            },
                            moat: undefined,
                            mediascope: undefined,
                            adloox: undefined,
                            weborama: undefined,

                        },
                        warning: 'direct_data.warning',
                    },
                },
                misc: {
                    target_url: AUTO_VIDEO_URL,
                    unmoderated: {
                        tns_id: 'direct_data.tnsId',
                    },
                    click_url: {
                        abuse: 'bs_data.count_links.abuseUrl',
                    },
                    trackers: ['direct_data.trackers'],
                    object_id: 'bs_data.adId',
                    impId: 'bs_data.impId',
                },
            },
        };

        expect(getNewAuctionDCParamsBannerStorage(auctionDCParams)).toEqual(expectedResult);
    });

    it('should return correct value for autodirect', () => {
        const auctionDCParams: BSMetaVideoAdDCParamsBannerStorage = {
            creative_params: {
                crypta_user_gender: '1',
                crypta_user_age: '2',
            },
            data_params: {
                '123': {
                    bs_data: {
                        sad: 'sad',
                        targetUrl: 'bs_data.targetUrl',
                        domain: '',
                        count_links: {
                            empty: 'bs_data.count_links.empty',
                            abuseUrl: 'bs_data.count_links.abuseUrl',
                            tracking: '',
                            sitelinks: ['sitelink'],
                        },
                        resource_links: {
                            direct_data: {
                                targetUrl: 'bs_data.resource_links.direct_data.targetUrl',
                                assets: {
                                    button: {
                                        href: 'bs_data.resource_links.direct_data.assets.button.href',
                                    },
                                },
                                callTrackingUrl: 'bs_data.resource_links.direct_data.callTrackingUrl',
                            },
                        },
                        actionButton: 'bs_data.actionButton',
                        bannerFlags: 'bs_data.bannerFlags',
                        bannerLang: 'bs_data.bannerLang',
                        adId: 'bs_data.adId',
                        hitLogId: 'bs_data.hitLogId',
                        impId: 'bs_data.impId',
                        geoDistance: 'bs_data.geoDistance',
                        region: 'bs_data.region',
                        workingTime: 'bs_data.workingTime',
                        telNum: 'bs_data.telNum',
                        metro: 'bs_data.metro',
                        liked: 'bs_data.liked',
                        addInfo: {
                            someProp: 'bs_data.addInfo.someProp',
                        },
                        S2SEnabled: true,
                        creativeId: 'C777',
                    },
                    direct_data: {
                        targetUrl: 'direct_data.targetUrl',
                        domain: 'direct_data.domain',
                        warning: 'direct_data.warning',
                        age: 'direct_data.age',
                        title: 'direct_data.title',
                        body: 'direct_data.body',
                        green_url_text_prefix: 'direct_data.green_url_text_prefix',
                        green_url_text_suffix: 'direct_data.green_url_text_suffix',
                        assets: {
                            button: {
                                key: 'direct_data.assets.button.key',
                                caption: 'direct_data.assets.button.caption',
                            },
                            logo: {
                                format: 'logoFormat',
                                someLogoKey: 'someLogoKey',
                            },
                        },
                        faviconSizes: {
                            w: 100,
                            h: 200,
                        },
                        admetrica: {
                            someAdmetricaKey: 'someAdmetricaKey',
                        },
                        tnsId: 'direct_data.tnsId',
                        trackers: 'direct_data.trackers',
                        secondTitle: 'direct_data.secondTitle',
                        TitleExtension: 'direct_data.TitleExtension',
                        img: {
                            '1': {
                                w: 100,
                                h: 150,
                                val: '//avatars.mds.yandex.net/1',
                            },
                        },
                        CTDefaultNumber: 'direct_data.CTDefaultNumber',
                    },
                },
            },
        };
        const expectedResult: AuctionDCParams = {
            sad: 'sad',
            creative_params: {
                crypta_user_gender: '1',
                crypta_user_age: '2',
            },
            data_params: {
                'bs_data.adId': {
                    target_url: 'direct_data.targetUrl',
                    count: 'bs_data.count_links.empty',
                    click_url: {
                        text_name: 'bs_data.resource_links.direct_data.targetUrl',
                        action_button: 'bs_data.actionButton',
                    },
                    text: {
                        banner_flags: 'bs_data.bannerFlags',
                        domain: 'direct_data.domain',
                        lang: 'bs_data.bannerLang',
                        warning: 'direct_data.warning',
                        age: 'direct_data.age',
                        title: 'direct_data.title',
                        body: 'direct_data.body',
                        green_url_text_prefix: 'direct_data.green_url_text_prefix',
                        green_url_text_suffix: 'direct_data.green_url_text_suffix',
                        dynamic_disclaimer: '1',
                    },
                    assets: {
                        button: {
                            key: 'direct_data.assets.button.key',
                            caption: 'direct_data.assets.button.caption',
                            href: 'bs_data.resource_links.direct_data.assets.button.href',
                        },
                        logo: {
                            logoFormat: {
                                someLogoKey: 'someLogoKey',
                                format: undefined,
                            },
                        },
                    },
                    object_id: 'bs_data.adId',
                    unmoderated: {
                        secondTitle: 'direct_data.TitleExtension',
                        geoDistance: 'bs_data.geoDistance',
                        region: 'bs_data.region',
                        workingTime: 'bs_data.workingTime',
                        telNum: 'bs_data.telNum',
                        metro: 'bs_data.metro',
                        liked: 'bs_data.liked',
                        sitelinks: ['sitelink'],
                        images: [[
                            'https://avatars.mds.yandex.net/1',
                            '100',
                            '150',
                        ]],
                        addInfo: {
                            someProp: 'bs_data.addInfo.someProp',
                        },
                        targetUrl: 'bs_data.targetUrl',
                        S2SEnabled: true,
                        CTUrl: `${CALL_TRACKING_URL_PREFIX}bs_data.resource_links.direct_data.callTrackingUrl`,
                        isCTEnabled: '1',
                        CTDefaultNumber: 'direct_data.CTDefaultNumber',
                        punyDomain: 'direct_data.domain',
                        faviconWidth: '100',
                        faviconHeight: '200',
                        measurers: {
                            admetrica: {
                                someAdmetricaKey: 'someAdmetricaKey',
                                sessionId: 'bs_data.adId:bs_data.hitLogId',
                            },
                            moat: undefined,
                            mediascope: undefined,
                            adloox: undefined,
                            weborama: undefined,

                        },
                        warning: 'direct_data.warning',
                    },
                },
                misc: {
                    target_url: AUTO_VIDEO_URL,
                    unmoderated: {
                        tns_id: 'direct_data.tnsId',
                    },
                    click_url: {
                        abuse: 'bs_data.count_links.abuseUrl',
                    },
                    trackers: ['direct_data.trackers'],
                    object_id: 'bs_data.adId',
                    impId: 'bs_data.impId',
                },
            },
        };

        expect(getNewAuctionDCParamsBannerStorage(auctionDCParams)).toEqual(expectedResult);
    });
});
