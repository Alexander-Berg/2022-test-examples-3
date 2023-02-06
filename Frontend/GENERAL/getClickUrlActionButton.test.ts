import { BSMetaVideoAd, BSMetaVideoAdDataParams } from '../../typings';
import { getClickUrlActionButton } from './getClickUrlActionButton';

describe('[VASTAdCreator] getClickUrlActionButton', () => {
    it('should return correct value', () => {
        const ad = {
            settings: {},
            dc_params: {
                data_params: {
                    '123': {
                        bs_data: {
                            resource_links: {
                                direct_data: {
                                    textName: 'http://an.yandex.ru/count/2',
                                },
                            },
                            actionButton: 'http://an.yandex.ru/count/1',
                        },
                    } as BSMetaVideoAdDataParams,
                },
            },
        } as BSMetaVideoAd;

        expect(getClickUrlActionButton(ad)).toEqual('http://an.yandex.ru/count/1');

        delete ad.dc_params.data_params['123'].bs_data.actionButton;

        expect(getClickUrlActionButton(ad)).toEqual('http://an.yandex.ru/count/2');
    });
});
