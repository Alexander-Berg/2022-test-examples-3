import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getImages } from './getImages';

describe('[VASTAdCreator] getImages', () => {
    it('should return empty array if data is empty', () => {
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

        expect(getImages(ad)).toEqual([]);
    });

    it('should return correct basic info', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            img: {
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
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;
        const expectedResult = [[
            'https://avatars.mds.yandex.net/1',
            '100',
            '150',
        ], [
            'https://avatars.mds.yandex.net/2',
            '200',
            '250',
        ]];

        expect(getImages(ad)).toEqual(expectedResult);
    });

    it('should return correct value with mdsMetaInfo and bg_color', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            img: {
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
                                '3': {
                                    w: 300,
                                    h: 350,
                                    val: '//avatars.mds.yandex.net/3',
                                },
                            },
                            mdsMetaInfo: {
                                '1': {
                                    ColorWizButton: '#000',
                                },
                                '2': {
                                    ColorWizButton: '#fff',
                                },
                            },
                            bg_color: {
                                '0': '#ff0',
                                '1': '#ff1',
                                '2': '#ff2',
                                '3': '#ff3',
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
            { ColorWizButton: '#000', backgroundColors: { bottom: '#ff0', left: '#ff1', right: '#ff2', top: '#ff3' } },
        ], [
            'https://avatars.mds.yandex.net/2',
            '200',
            '250',
            { ColorWizButton: '#fff', backgroundColors: { bottom: '#ff0', left: '#ff1', right: '#ff2', top: '#ff3' } },
        ], [
            'https://avatars.mds.yandex.net/3',
            '300',
            '350',
            { backgroundColors: { bottom: '#ff0', left: '#ff1', right: '#ff2', top: '#ff3' } },
        ]];

        expect(getImages(ad)).toEqual(expectedResult);
    });

    it('should not add https if its exist', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        direct_data: {
                            img: {
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

        expect(getImages(ad)).toEqual(expectedResult);
    });
});
