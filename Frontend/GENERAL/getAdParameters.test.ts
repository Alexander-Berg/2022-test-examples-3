import { VPAID_THEME_MOTION } from '../../const';
import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { AdParameters, getAdParameters } from './getAdParameters';

describe('[VASTAdCreator] getAdParameters', () => {
    it('should return correct value', () => {
        const ad = {
            settings: {
                '123': {
                    linkTail: '',
                    viewNotices: [],
                    renderLinkTail: 'renderLinkTail',
                },
            },
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            surveyConfig: {
                                some_field: 'one',
                                some_field2: 2
                            } as unknown,
                            count_links: {
                                tracking: 'bs_data.count_links.tracking',
                            },
                            resource_links: {
                                direct_data: {
                                    targetUrl: 'bs_data.resource_links.direct_data.targetUrl',
                                },
                            },
                            impId: '123',
                        },
                        direct_data: {},
                        constructor_data: {
                            Theme: VPAID_THEME_MOTION,
                            Packshot: {
                                ImageUrl: 'constructor_data.Packshot.ImageUrl',
                                Duration: 3,
                            },
                            MediaFiles: [{
                                Id: '1',
                                Delivery: 'progressive',
                                Width: '720',
                                Height: '480',
                                Url: 'url_1',
                                MimeType: 'webm',
                                Bitrate: '111',
                                Codec: 'vp9',
                                Framerate: '25',
                                FileSize: '1111',
                            }, {
                                Id: '2',
                                Url: 'url_2',
                            }],
                            Duration: 14.7,
                            SocialAdvertisement: true,
                            PlaybackParameters: {
                                ShowSkipButton: true,
                                SkipDelay: '5',
                            },
                            UseVpaidImpressions: true,
                            IsStock: true,
                            PythiaParams: {
                                Slug: 'PythiaParams.Slug',
                                BasePath: 'PythiaParams.BasePath',
                                Extra: 'PythiaParams.Extra',
                            },
                            FirstFrameParameters: [{
                                Width: '1920',
                                Height: '720',
                                Url: 'firstFrameUrl',
                                Type: 'firstFrameType',
                            }],
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        // @ts-ignore: не учитываем AUCTION_DC_PARAMS
        const expectedResult: AdParameters = {
            HAS_AGE: false,
            HAS_BODY: false,
            HAS_BUTTON: false,
            HAS_DOMAIN: false,
            HAS_TITLE: false,
            PACKSHOT_IMAGE_URL: 'constructor_data.Packshot.ImageUrl',
            PACKSHOT_START_NOTICE_URL: 'bs_data.count_links.tracking?action-id=10',
            theme: VPAID_THEME_MOTION,
            duration: 15,
            mediaFiles: [{
                id: '1',
                delivery: 'progressive',
                width: 720,
                height: 480,
                url: 'url_1',
                type: 'webm',
                bitrate: 111,
                codec: 'vp9',
                framerate: 25,
                fileSize: 1111,
            }, {
                id: '2',
                delivery: undefined,
                width: null,
                height: null,
                url: 'url_2',
                type: undefined,
                bitrate: null,
                codec: undefined,
                framerate: undefined,
                fileSize: undefined,
            }],
            socialAdvertising: true,
            packshot_duration: 3,
            playbackParameters: {
                showSkipButton: true,
                skipDelay: 5,
            },
            encounters: ['bs_data.count_links.tracking?action-id=14'],
            pythia: {
                slug: 'PythiaParams.Slug',
                basePath: 'PythiaParams.BasePath',
                extra: 'PythiaParams.Extra',
                'survey-config': {
                    some_field: 'one',
                    some_field2: 2
                }
            },
            trackingEvents: {
                start: ['bs_data.count_links.tracking?action-id=11', 'renderLinkTail'],
                trueView: ['bs_data.count_links.tracking?action-id=19'],
                returnAfterClickThrough: ['bs_data.resource_links.direct_data.targetUrl?test-tag=136'],
                showHp: ['bs_data.count_links.tracking?action-id=21'],
                clickHp: ['bs_data.count_links.tracking?action-id=22'],
                adsFinish: ['bs_data.count_links.tracking?action-id=24'],
            },
            isStock: true,
            firstFrame: {
                images: [{
                    width: 1920,
                    height: 720,
                    url: 'firstFrameUrl',
                    type: 'firstFrameType',
                }],
            },
        };
        const result = getAdParameters(ad);

        // Игнорируем AUCTION_DC_PARAMS. Для них есть отдельные тесты.
        // @ts-ignore
        delete result.AUCTION_DC_PARAMS;

        expect(result).toEqual(expectedResult);
    });
});
