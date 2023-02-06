import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getMeasurers, GetMeasurersResult } from './getMeasurers';

const BS_DATA_MOCK = {
    adId: '1',
    hitLogId: '2',
    pageId: '3',
    impId: '4',
    sourceDomain: '5',
};

describe('[VASTAdCreator] getMeasurers', () => {
    it('should return undefined if data is empty', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {},
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getMeasurers(ad)).toEqual(undefined);
    });

    it('should return correct admetrica data', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {
                            admetrica: {
                                someKey: 'someValue',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: GetMeasurersResult = {
            admetrica: {
                someKey: 'someValue',
                sessionId: '1:2',
            },
        };

        expect(getMeasurers(ad)).toEqual(expectedResult);
    });

    it('should return correct moat data', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {
                            moat: {
                                someKey: 'someValue',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: GetMeasurersResult = {
            moat: {
                someKey: 'someValue',
                moatClientSlicer1: '3',
                moatClientSlicer2: '4',
                zMoatHitlogID: '2',
            },
        };

        expect(getMeasurers(ad)).toEqual(expectedResult);
    });

    it('should return correct mediascope data', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {
                            mediascope: {
                                someKey: 'someValue',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: GetMeasurersResult = {
            mediascope: {
                someKey: 'someValue',
                hitlogid: '2',
            },
        };

        expect(getMeasurers(ad)).toEqual(expectedResult);
    });

    it('should return correct adloox data', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {
                            adloox: {
                                someKey: 'someValue',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: GetMeasurersResult = {
            adloox: {
                someKey: 'someValue',
                hitlogid: '2',
                id9: '2',
                id1: '5',
            },
        };

        expect(getMeasurers(ad)).toEqual(expectedResult);
    });

    it('should return correct weborama data', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: BS_DATA_MOCK,
                        direct_data: {
                            weborama: {
                                someKey: 'someValue',
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult: GetMeasurersResult = {
            weborama: {
                someKey: 'someValue',
                hitlogid: '2',
            },
        };

        expect(getMeasurers(ad)).toEqual(expectedResult);
    });
});
