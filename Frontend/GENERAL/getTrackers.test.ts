import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getTrackers } from './getTrackers';

describe('[VASTAdCreator] getTrackers', () => {
    it('should return empty array if trackers are empty', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {},
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getTrackers(ad)).toEqual([]);
    });

    it('should return array if trackers are string', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            trackers: 'https://ad.doubleclick.net/ddm/trackimp/N1280539',
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getTrackers(ad)).toEqual(['https://ad.doubleclick.net/ddm/trackimp/N1280539']);
    });

    it('should return array if trackers are object', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            trackers: {
                                '1': 'https://ad.doubleclick.net/ddm/trackimp/N1280539',
                                '2': 'https://ad.doubleclick.net/ddm/trackimp/N7310841',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getTrackers(ad)).toEqual([
            'https://ad.doubleclick.net/ddm/trackimp/N1280539',
            'https://ad.doubleclick.net/ddm/trackimp/N7310841',
        ]);
    });
});
