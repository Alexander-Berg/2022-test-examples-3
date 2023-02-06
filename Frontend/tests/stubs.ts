import { createOffersStub } from '@src/storybook/stubs/entitites/createOffersStub';

import { getInternalInitialState } from '@src/store/services/internal/reducer';
import { getUserInitialState } from '@src/store/services/user/reducer';
import type { IAppState } from '@src/store/services/types';
import { getEntitiesInitialState } from '@src/store/services/entities/reducer';
import type { IShopListProps } from '../ShopList.typings';

export const props: IShopListProps = {
    shops: [
        { id: '1', showUid: '1', reqid: '1' },
        { id: '2', showUid: '2', reqid: '2' },
        { id: '3', showUid: '3', reqid: '3' },
        { id: '4', showUid: '4', reqid: '4' },
        { id: '5', showUid: '5', reqid: '5' },
    ],
};

export const state: Partial<IAppState> = {
    entities: {
        ...getEntitiesInitialState(),
        offers: {
            '1': createOffersStub({
                id: '1',
                shop: { title: 'Solana', id: '1', hostname: 'yandex.ru' },
                urls: { direct: 'https://coinmarketcap.com/currencies/solana/' },
                price: { type: 'exact', current: '160', currency: 'RUR' },
            }),
            '2': createOffersStub({
                id: '2',
                shop: { title: 'Raydium', id: '2', hostname: 'yandex.ru' },
                urls: { direct: 'https://coinmarketcap.com/currencies/raydium/' },
                price: { type: 'exact', current: '14', currency: 'RUR' },
            }),
            '3': createOffersStub({
                id: '3',
                shop: { title: 'Serum', id: '3', hostname: 'yandex.ru' },
                urls: { direct: 'https://coinmarketcap.com/currencies/serum/' },
                price: { type: 'exact', current: '10', currency: 'RUR' },
            }),
            '4': createOffersStub({
                id: '4',
                shop: { title: 'Polkadot', id: '4', hostname: 'yandex.ru' },
                urls: { direct: 'https://coinmarketcap.com/currencies/polkadot-new/' },
                price: { type: 'exact', current: '37', currency: 'RUR' },
            }),
            '5': createOffersStub({
                id: '5',
                shop: { title: 'Bitcoin', id: '5', hostname: 'yandex.ru' },
                urls: { direct: 'https://coinmarketcap.com/currencies/bitcoin/' },
                price: { type: 'exact', current: '47500', currency: 'RUR' },
            }),
        },
    },
    internal: getInternalInitialState(),
    user: getUserInitialState(),
};
