import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getAssetsImages } from './getAssetsImages';

describe('[VASTAdCreator] getImages', () => {
    it('should return undefined if data is empty', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {},
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getAssetsImages(ad)).toEqual(undefined);
    });

    it('should return correct info', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            newImage: {
                                '1': {
                                    w: 100,
                                    h: 150,
                                    val: '//avatars.mds.yandex.net/1',
                                },
                                '2': {
                                    w: 200,
                                    h: 250,
                                    val: '//avatars.mds.yandex.net/2',
                                },
                                '3': undefined,
                            },
                            mdsMetaInfo: {
                                '1': {
                                    ColorWizButton: '#000',
                                },
                                '2': {
                                    ColorWizButton: '#fff',
                                },
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult = [[
            'https://avatars.mds.yandex.net/1',
            '100',
            '150',
            { ColorWizButton: '#000' },
        ], [
            'https://avatars.mds.yandex.net/2',
            '200',
            '250',
            { ColorWizButton: '#fff' },
        ]];

        expect(getAssetsImages(ad)).toEqual(expectedResult);
    });

    it('should not add https if its exist', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            newImage: {
                                '1': {
                                    w: 100,
                                    h: 150,
                                    val: 'https://avatars.mds.yandex.net/1',
                                },
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult = [[
            'https://avatars.mds.yandex.net/1',
            '100',
            '150',
        ]];

        expect(getAssetsImages(ad)).toEqual(expectedResult);
    });
});
