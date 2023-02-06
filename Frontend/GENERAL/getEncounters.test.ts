import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getTrackingUrl } from '../getTrackingUrl/getTrackingUrl';
import { getEncounters } from './getEncounters';

describe('[VASTAdCreator] getEncounters', () => {
    it('should return only first encounter if UseVpaidImpressions', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'https://an.yandex.ru/tracking/WKyejI',
                            },
                        },
                        constructor_data: {
                            UseVpaidImpressions: true,
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getEncounters(ad)).toEqual([getTrackingUrl(ad, 'firstEncounter')]);
    });

    it('should return only first encounter if video in tga', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'https://an.yandex.ru/tracking/WKyejI',
                            },
                        },
                    } as BSMetaVideoAdDataParams,
                },
                video_in_tga_params: {},
            },
        } as BSMetaVideoAd;

        expect(getEncounters(ad)).toEqual([getTrackingUrl(ad, 'firstEncounter')]);
    });

    it('should add linkTail and first viewNotice if UseVpaidImpressions = false', () => {
        const ad = {
            settings: {
                '123': {
                    linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj',
                    viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539', 'https://ad.doubleclick.net/ddm/trackimp/N98676'],
                },
            },
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'https://an.yandex.ru/tracking/WKyejI',
                            },
                            impId: '123',
                        },
                        constructor_data: {
                            UseVpaidImpressions: false,
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getEncounters(ad)).toEqual([
            getTrackingUrl(ad, 'firstEncounter'),
            ad.settings['123'].linkTail,
            ad.settings['123'].viewNotices[0],
        ]);
    });

    it('should add linkTail and all viewNotices if AddPixelImpression', () => {
        const ad = {
            settings: {
                '123': {
                    linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj',
                    viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539', 'https://ad.doubleclick.net/ddm/trackimp/N98676'],
                },
            },
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'https://an.yandex.ru/tracking/WKyejI',
                            },
                            impId: '123',
                        },
                        constructor_data: {
                            UseVpaidImpressions: false,
                            AddPixelImpression: true,
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getEncounters(ad)).toEqual([
            getTrackingUrl(ad, 'firstEncounter'),
            ad.settings['123'].linkTail,
            ...ad.settings['123'].viewNotices,
        ]);
    });

    it('should not add linkTail in ComboBlock', () => {
        const ad = {
            settings: {
                '123': {
                    linkTail: 'https://an.yandex.ru/rtbcount/1K-WJkUj',
                    viewNotices: ['https://ad.doubleclick.net/ddm/trackimp/N1280539', 'https://ad.doubleclick.net/ddm/trackimp/N98676'],
                    isComboBlock: true,
                },
            },
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'https://an.yandex.ru/tracking/WKyejI',
                            },
                            impId: '123',
                        },
                        constructor_data: {
                            UseVpaidImpressions: false,
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getEncounters(ad)).toEqual([
            getTrackingUrl(ad, 'firstEncounter'),
            ad.settings['123'].viewNotices[0],
        ]);
    });
});
