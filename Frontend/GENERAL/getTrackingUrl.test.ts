import { getTrackingUrl } from './getTrackingUrl';
import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';

describe('[VASTAdCreator] getTrackingUrl', () => {
    it('should return correct value', () => {
        const url = 'https://an.yandex.ru/tracking/WKyejI';
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: url,
                            },
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getTrackingUrl(ad, 'start')).toEqual(`${url}?action-id=0`);
    });

    it('should prioritise links from trackingMap', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            count_links: {
                                tracking: 'tracking',
                                trackingMap: {
                                    start: 'trackingMap.start',
                                },
                            },
                        },
                    } as unknown as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getTrackingUrl(ad, 'start')).toEqual('trackingMap.start');
    });
});
