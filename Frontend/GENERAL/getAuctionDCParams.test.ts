import { AUTO_VIDEO_URL, CALL_TRACKING_URL_PREFIX } from '../../const';
import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { AuctionDCParams, getAuctionDCParams } from './getAuctionDCParams';

describe('[VASTAdCreator] getAuctionDCParams', () => {
    it('should return correct value for old format', () => {
        const ad = {
            settings: {},
            dc_params: {
                creative_params: {
                    crypta_user_gender: '1',
                    crypta_user_age: '2',
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
                            iconUrl: 'bs_data.iconUrl',
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
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
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
                        iconUrl: 'bs_data.iconUrl',
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

        expect(getAuctionDCParams(ad)).toEqual(expectedResult);
    });

    it('should return correct value for new format', () => {
        const ad = {
            settings: {},
            dc_params: {
                creative_params: {
                    crypta_user_gender: '1',
                    crypta_user_age: '2',
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
                                falseClick: 'bs_data.count_links.falseClick',
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
                            pageId: 'bs_data.pageId',
                            bidreqid: 'bs_data.bidreqid',
                            campaignid: 'bs_data.campaignid',
                            vmap_request_id: 'bs_data.vmap_request_id',
                            vcardUrl: 'bs_data.vcardUrl',
                        },
                        direct_data: {
                            targetUrl: 'direct_data.targetUrl',
                            Href: 'direct_data.Href',
                            domain: 'direct_data.domain',
                            warning: 'direct_data.warning',
                            age: 'direct_data.age',
                            DirectBannersLogFields: {
                                Age: 'direct_data.DirectBannersLogFields.Age',
                            },
                            title: 'direct_data.title',
                            Title: 'direct_data.Title',
                            body: 'direct_data.body',
                            Body: 'direct_data.Body',
                            showTitleAndBody: true,
                            EssGreenUrlTextPrefix: {
                                Value: 'direct_data.EssGreenUrlTextPrefix.Value',
                            },
                            green_url_text_prefix: 'direct_data.green_url_text_prefix',
                            EssGreenUrlTextSuffix: {
                                Value: 'direct_data.EssGreenUrlTextSuffix.Value',
                            },
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
                            EssButton: {
                                Value: {
                                    button_key: 'direct_data.EssButton.Value.button_key',
                                    button_caption: 'direct_data.EssButton.Value.button_caption',
                                },
                            },
                            faviconSizes: {
                                w: 100,
                                h: 200,
                            },
                            tnsId: 'direct_data.tnsId',
                            trackers: 'direct_data.trackers',
                            multicards: 'direct_data.multicards',
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: AuctionDCParams = {
            sad: 'sad',
            creative_params: {
                crypta_user_gender: '1',
                crypta_user_age: '2',
            },
            data_params: {
                'bs_data.adId': {
                    target_url: 'direct_data.Href',
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
                        age: 'direct_data.DirectBannersLogFields.Age',
                        title: 'direct_data.Title',
                        body: 'direct_data.Body',
                        green_url_text_prefix: 'direct_data.EssGreenUrlTextPrefix.Value',
                        green_url_text_suffix: 'direct_data.EssGreenUrlTextSuffix.Value',
                        dynamic_disclaimer: '1',
                    },
                    assets: {
                        button: {
                            key: 'direct_data.EssButton.Value.button_key',
                            caption: 'direct_data.EssButton.Value.button_caption',
                            href: 'bs_data.resource_links.direct_data.assets.button.href',
                        },
                        logo: {
                            logoFormat: {
                                someLogoKey: 'someLogoKey',
                                format: undefined,
                            },
                        },
                        multicards: 'direct_data.multicards',
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
                        measurers: undefined,
                        falseClick: 'bs_data.count_links.falseClick',
                        warning: 'direct_data.warning',
                        showTitleAndBody: true,
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
                    pageId: 'bs_data.pageId',
                    bidreqid: 'bs_data.bidreqid',
                    campaignid: 'bs_data.campaignid',
                    vmap_request_id: 'bs_data.vmap_request_id',
                },
            },
        };

        expect(getAuctionDCParams(ad)).toEqual(expectedResult);
    });

    it('should return correct value for autodirect', () => {
        const ad = {
            settings: {},
            dc_params: {
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
                            S2SEnabled: true,
                            vcardUrl: 'bs_data.vcardUrl',
                            productType: 'bs_data.productType',
                            addInfo: {
                                type: 'mobile-app',
                                name: 'appName',
                                store_name: 'appStoreName',
                                store_app_id: 'appStoreAppId',
                                store_content_id: 'appStoreContentId',
                                download_count: 123,
                                review_count: '987',
                                rating: '8',
                                age_label: '99',
                                free: 1,
                                price: '8765',
                                price_currency_code: 'RUB',
                                price_currency_symbol: '₽',
                                call_to_action: 'callToAction',
                                otherProp: 'otherInfoProp',
                            },
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
                            priceInfo: {
                                currency: 'RUB',
                                old: '660.5',
                                price: '599.99',
                                prefix: 'prefix',
                                discount: 'discount',
                            },
                        },
                        constructor_data: {
                            CreativeId: '777',
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
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
                        price_info: {
                            currency: 'RUB',
                            old: '660.5',
                            price: '599.99',
                            prefix: 'prefix',
                            discount: 'discount',
                        },
                        app: {
                            name: 'appName',
                            store_name: 'appStoreName',
                            store_app_id: 'appStoreAppId',
                            store_content_id: 'appStoreContentId',
                            download_count: 123,
                            review_count: 987,
                            rating: 8,
                            age: 99,
                            free: true,
                            price: 8765,
                            price_currency_code: 'RUB',
                            price_currency_symbol: '₽',
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
                            type: 'mobile-app',
                            name: 'appName',
                            store_name: 'appStoreName',
                            store_app_id: 'appStoreAppId',
                            store_content_id: 'appStoreContentId',
                            download_count: 123,
                            review_count: '987',
                            rating: '8',
                            age_label: '99',
                            free: 1,
                            price: '8765',
                            price_currency_code: 'RUB',
                            price_currency_symbol: '₽',
                            call_to_action: 'callToAction',
                            otherProp: 'otherInfoProp',
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
                        vcardUrl: 'bs_data.vcardUrl',
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
                    productType: 'bs_data.productType',
                },
            },
        };

        expect(getAuctionDCParams(ad)).toEqual(expectedResult);
    });

    it('should return correct value for video in tga', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            position: 1,
                            targetUrl: '',
                            domain: '',
                            count_links: {
                                tracking: '',
                                url: 'bs_data.count_links.url',
                            },
                            bannerFlags: 'bs_data.bannerFlags',
                            bannerLang: 'bs_data.bannerLang',
                            adId: 'bs_data.adId',
                            impId: 'bs_data.impId',
                            pageId: 'bs_data.pageId',
                            bidreqid: 'bs_data.bidreqid',
                            vmap_request_id: 'bs_data.vmap_request_id',
                        },
                        direct_data: {},
                    } as BSMetaVideoAdDataParams,
                },
                video_in_tga_params: {
                    campaign_id: '0',
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: AuctionDCParams = {
            data_params: {
                '1': {
                    click_url: {
                        action_button: 'bs_data.count_links.url',
                    },
                },
                misc: {
                    layout_type: 'clickable_video',
                    object_id: '1',
                    impId: 'bs_data.impId',
                    pageId: 'bs_data.pageId',
                    bidreqid: 'bs_data.bidreqid',
                    campaignid: '0',
                    vmap_request_id: 'bs_data.vmap_request_id',
                    socialAdvertising: false,
                },
            },
        };

        expect(getAuctionDCParams(ad)).toEqual(expectedResult);
    });
});
