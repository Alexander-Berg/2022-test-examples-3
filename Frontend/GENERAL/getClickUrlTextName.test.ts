import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getClickUrlTextName } from './getClickUrlTextName';

describe('[VASTAdCreator] getClickUrlTextName', () => {
    it('should return correct value', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            resource_links: {
                                direct_data: {
                                    targetUrl: 'http://an.yandex.ru/count/1',
                                    textName: 'http://an.yandex.ru/count/3',
                                },
                            },
                            actionButton: 'http://an.yandex.ru/count/2',
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getClickUrlTextName(ad)).toEqual('http://an.yandex.ru/count/1');

        delete ad.dc_params.data_params['123'].bs_data.resource_links?.direct_data?.targetUrl;

        expect(getClickUrlTextName(ad)).toEqual('http://an.yandex.ru/count/2');

        delete ad.dc_params.data_params['123'].bs_data.actionButton;

        expect(getClickUrlTextName(ad)).toEqual('http://an.yandex.ru/count/3');
    });
});
