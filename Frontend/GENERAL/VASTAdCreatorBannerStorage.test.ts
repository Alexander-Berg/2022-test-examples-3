import { BSMetaVideoAdBannerStorage } from '../typings';
import { normalizeVast } from '../utils/normalizeVast';
import { VASTAdCreatorBannerStorage } from './VASTAdCreatorBannerStorage';

describe('VASTAdCreatorBannerStorage', () => {
    it('should create correct VAST Ad', () => {
        const ad: BSMetaVideoAdBannerStorage = {
            vast: `
            <?xml version="1.0" encoding="UTF-8"?>
            <VAST version="3.0">
                <Ad id="a34sdf">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdParameters>
                        <![CDATA[
                            {
                                "HAS_BUTTON": true,
                                "theme": "video-banner_interactive-viewer",
                                "duration": 15.0,
                                "mediaFiles": [
                                    {
                                        "bitrate": null,
                                        "delivery": "progressive",
                                        "height": null,
                                        "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                        "type": "application/vnd.apple.mpegurl",
                                        "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                        "width": null
                                    }
                                ],
                                "socialAdvertising": false,
                                "AUCTION_DC_PARAMS": {
                                    "creative_params": {
                                        "crypta_user_age": "2",
                                        "crypta_user_gender": "1"
                                    },
                                    "data_params": {
                                        "123": {
                                            "bs_data": {
                                                "sad": "sad",
                                                "targetUrl": "",
                                                "domain": "",
                                                "count_links": {
                                                    "empty": "bs_data.count_links.empty",
                                                    "abuseUrl": "bs_data.count_links.abuseUrl",
                                                    "tracking": ""
                                                },
                                                "resource_links": {
                                                    "direct_data": {
                                                        "targetUrl": "bs_data.resource_links.direct_data.targetUrl",
                                                        "assets": {
                                                            "button": {
                                                                "href": "bs_data.resource_links.direct_data.assets.button.href"
                                                            }
                                                        }
                                                    }
                                                },
                                                "actionButton": "bs_data.actionButton",
                                                "bannerFlags": "bs_data.bannerFlags",
                                                "bannerLang": "bs_data.bannerLang",
                                                "adId": "bs_data.adId",
                                                "hitLogId": "bs_data.hitLogId",
                                                "impId": "bs_data.impId"
                                            },
                                            "direct_data": {
                                                "targetUrl": "direct_data.targetUrl",
                                                "domain": "direct_data.domain",
                                                "warning": "direct_data.warning",
                                                "age": "direct_data.age",
                                                "title": "direct_data.title",
                                                "body": "direct_data.body",
                                                "green_url_text_prefix": "direct_data.green_url_text_prefix",
                                                "green_url_text_suffix": "direct_data.green_url_text_suffix",
                                                "assets": {
                                                    "button": {
                                                        "key": "direct_data.assets.button.key",
                                                        "caption": "direct_data.assets.button.caption"
                                                    },
                                                    "logo": {
                                                        "format": "logoFormat",
                                                        "someLogoKey": "someLogoKey"
                                                    }
                                                },
                                                "faviconSizes": {
                                                    "w": 100,
                                                    "h": 200
                                                },
                                                "admetrica": {
                                                    "someAdmetricaKey": "someAdmetricaKey"
                                                },
                                                "tnsId": "direct_data.tnsId",
                                                "trackers": "direct_data.trackers"
                                            }
                                        }
                                    }
                                }
                            }
                            ]]>
                        </AdParameters>
                    </InLine>
                </Ad>
                <SomeExtraTag></SomeExtraTag>
            </VAST>
            `,
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0"?>
            <Ad id="a34sdf" sequence="2">
                <InLine>
                    <AdSystem>Yabs Ad CPC Server</AdSystem>
                    <AdParameters>
                    <![CDATA[
                        {
                            "HAS_BUTTON": true,
                            "theme": "video-banner_interactive-viewer",
                            "duration": 15.0,
                            "mediaFiles": [
                                {
                                    "bitrate": null,
                                    "delivery": "progressive",
                                    "height": null,
                                    "id": "https:/strm.yandex.ru/vh-canvas-converted/get-canvas",
                                    "type": "application/vnd.apple.mpegurl",
                                    "url": "https://strm.yandex.ru/vh-canvas-converted/get-canvas/video_5f982fb15cffc68d5e867ca0.m3u8",
                                    "width": null
                                }
                            ],
                            "socialAdvertising": false,
                            "AUCTION_DC_PARAMS": {
                                "sad": "sad",
                                "creative_params": {
                                    "crypta_user_gender": "1",
                                    "crypta_user_age": "2"
                                },
                                "data_params": {
                                    "bs_data.adId": {
                                        "target_url": "direct_data.targetUrl",
                                        "count": "bs_data.count_links.empty",
                                        "click_url": {
                                            "text_name": "bs_data.resource_links.direct_data.targetUrl",
                                            "action_button": "bs_data.actionButton"
                                        },
                                        "text": {
                                            "banner_flags": "bs_data.bannerFlags",
                                            "domain": "direct_data.domain",
                                            "lang": "bs_data.bannerLang",
                                            "warning": "direct_data.warning",
                                            "age": "direct_data.age",
                                            "title": "direct_data.title",
                                            "body": "direct_data.body",
                                            "green_url_text_prefix": "direct_data.green_url_text_prefix",
                                            "green_url_text_suffix": "direct_data.green_url_text_suffix",
                                            "dynamic_disclaimer": "1"
                                        },
                                        "assets": {
                                            "button": {
                                                "key": "direct_data.assets.button.key",
                                                "caption": "direct_data.assets.button.caption",
                                                "href": "bs_data.resource_links.direct_data.assets.button.href"
                                            },
                                            "logo": {
                                                "logoFormat": {
                                                    "someLogoKey": "someLogoKey"
                                                }
                                            }
                                        },
                                        "object_id": "bs_data.adId",
                                        "unmoderated": {
                                            "punyDomain": "direct_data.domain",
                                            "faviconWidth": "100",
                                            "faviconHeight": "200",
                                            "measurers": {
                                                "admetrica": {
                                                    "someAdmetricaKey": "someAdmetricaKey",
                                                    "sessionId": "bs_data.adId:bs_data.hitLogId"
                                                }
                                            },
                                            "warning": "direct_data.warning"
                                        }
                                    },
                                    "misc": {
                                        "target_url": "http://ru.yandex.auto-video",
                                        "unmoderated": {
                                            "tns_id": "direct_data.tnsId"
                                        },
                                        "click_url": {
                                            "abuse": "bs_data.count_links.abuseUrl"
                                        },
                                        "trackers": [
                                            "direct_data.trackers"
                                        ],
                                        "object_id": "bs_data.adId",
                                        "impId": "bs_data.impId"
                                    }
                                }
                            }
                        }
                        ]]>
                    </AdParameters>
                </InLine>
            </Ad>
        `);

        const vastAdCreatorBannerStorage = new VASTAdCreatorBannerStorage(ad, { sequence: 2 });
        const resultVast = normalizeVast(vastAdCreatorBannerStorage.getXMLString());

        expect(resultVast).toEqual(expectedVast);
        expect(vastAdCreatorBannerStorage.getIsAuctionDCParamsSuccessfullyUpdated()).toEqual(true);
    });

    it('should create correct VAST Ad without AdParameters', () => {
        const ad: BSMetaVideoAdBannerStorage = {
            vast: `
            <?xml version="1.0" encoding="UTF-8"?>
            <VAST version="3.0">
                <Ad id="a34sdf">
                    <Wrapper>
                        <VASTAdTagURI>
                            <![CDATA[https://an.yandex.ru/meta/280612?imp-id=1&target-ref=https://music.yandex.ru&page-ref=https://music.yandex.ru]]>
                        </VASTAdTagURI>
                    </Wrapper>
                </Ad>
            </VAST>
            `,
        };
        const expectedVast = normalizeVast(`
            <?xml version="1.0"?>
            <Ad id="a34sdf" sequence="2">
                <Wrapper>
                    <VASTAdTagURI>
                        <![CDATA[https://an.yandex.ru/meta/280612?imp-id=1&target-ref=https://music.yandex.ru&page-ref=https://music.yandex.ru]]>
                    </VASTAdTagURI>
                </Wrapper>
            </Ad>
        `);

        const vastAdCreatorBannerStorage = new VASTAdCreatorBannerStorage(ad, { sequence: 2 });
        const resultVast = normalizeVast(vastAdCreatorBannerStorage.getXMLString());

        expect(resultVast).toEqual(expectedVast);
        expect(vastAdCreatorBannerStorage.getIsAuctionDCParamsSuccessfullyUpdated()).toEqual(true);
    });

    it('getIsAuctionDCParamsSuccessfullyUpdated() should return false if no ad', () => {
        const ad: BSMetaVideoAdBannerStorage = {
            vast: `
            <?xml version="1.0" encoding="UTF-8"?>
            <VAST version="3.0">
                <SomeExtraTag></SomeExtraTag>
            </VAST>
            `,
        };
        const vastAdCreatorBannerStorage = new VASTAdCreatorBannerStorage(ad, { sequence: undefined });

        expect(vastAdCreatorBannerStorage.getXMLString()).toEqual('');
        expect(vastAdCreatorBannerStorage.getIsAuctionDCParamsSuccessfullyUpdated()).toEqual(false);
    });

    it('getIsAuctionDCParamsSuccessfullyUpdated() should return false if AUCTION_DC_PARAMS is invalid', () => {
        const ad: BSMetaVideoAdBannerStorage = {
            vast: `
            <?xml version="1.0" encoding="UTF-8"?>
            <VAST version="3.0">
                <Ad id="a34sdf">
                    <InLine>
                        <AdSystem>Yabs Ad CPC Server</AdSystem>
                        <AdParameters>
                        <![CDATA[
                            {
                                "HAS_BUTTON": true,
                                "theme": "video-banner_interactive-viewer",
                                "duration": 15.0,
                                "socialAdvertising": false,
                                "AUCTION_DC_PARAMS": {
                                    "creative_params": {
                                        "crypta_user_age": "2",
                                        "crypta_user_gender": "1"
                                    },
                                    "data_params": {
                                        "123": {
                                            "bs_data": {
                                                "sad": "sad",
                                                "actionButton": "bs_data.actionButton",
                                                "bannerFlags": "bs_data.bannerFlags",
                                                "bannerLang": "bs_data.bannerLang",
                                                "adId": "bs_data.adId",
                                                "hitLogId": "bs_data.hitLogId",
                                                "impId": "bs_data.impId"
                                            },
                                            "direct_data": {}
                                        }
                                    }
                                }
                            }
                            ]]>
                        </AdParameters>
                    </InLine>
                </Ad>
                <SomeExtraTag></SomeExtraTag>
            </VAST>
            `,
        };
        const vastAdCreatorBannerStorage = new VASTAdCreatorBannerStorage(ad, { sequence: undefined });

        expect(vastAdCreatorBannerStorage.getIsAuctionDCParamsSuccessfullyUpdated()).toEqual(false);
    });
});
