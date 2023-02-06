import * as React from 'react';
import { ImageSimple } from '@yandex-turbo/components/ImageSimple/ImageSimple';
import { Fragment } from '@yandex-turbo/components/Fragment/Fragment';
import { Button } from '@yandex-turbo/components/Button/Button';
import { IAdapterContext } from 'platform/types/AdapterContext';
import { IProps } from '../ProductItem.types';
import { IScheme } from '../ProductItem.types';
import { CurrencyAvailable } from '../../Cost/Currency/Currency';

import '@yandex-turbo/components/Button/_theme/Button_theme_blue.scss';
import '@yandex-turbo/components/Button/_size/Button_size_s.scss';
import { ListType } from '../../Products/Products.adapter';

export const productItemPropsMock: IProps = {
    offerId: '6596',
    product: {
        id: 'p6596',
        description: 'Кольца колодезные',
        href: 'https://yandex.ru/turbo?utm_source=turbo_turbo&text=https%3A%2F%2Fwww.betonproject.ru%2Fproducts%2Fks-10-9%3Fvrid%3D6596',
        price: {
            current: 9990,
            currencyId: 'RUR' as CurrencyAvailable,
        },
    },
    page: '0',
    innerFooter: true,
    description: (<Fragment>Кольца колодезные</Fragment>),
    footer: (<Button text="Выбрать" theme="blue" width="max" size="s" />),
    image: (
        <ImageSimple
            src="//avatars.mds.yandex.net/get-turbo/1347011/2a00000166353d933920d5e6808ef0b14200/max_g480_c12_r16x9_pd20"
            fit="contain"
            ratio="1x1"
        />
    ),
};

export const getProductItemDataMock = typeof jest !== 'undefined' && jest.fn<IScheme, [ListType | undefined]>(listType => {
    return {
        block: 'product-item',
        price: 118999999.04,
        href: 'https://yandex.ru/turbo?utm_source=turbo_turbo&text=https%3A//www.betonproject.ru/products/ks-10-9%3Fvrid%3D6596',
        offerId: '6596',
        description: 'Электроакустические кольца колодезные',
        currencyId: 'RUR',
        thumb: '//avatars.mds.yandex.net/get-turbo/1347011/2a00000166353d933920d5e6808ef0b14200/',
        listType,
        openCardButton: {
            theme: 'blue',
            count: 1,
        },
        meta: 'Электроакустические кольца колодезные',
    } as IScheme;
});

type ExpFlags = Record<string, string | number>;

export const getAdapterContextMock = typeof jest !== 'undefined' && jest.fn<IAdapterContext, [ExpFlags, boolean | undefined]>((expFlags, redesign) => {
    return {
        expFlags,
        data: {
            doc: {
                redesign,
            },
        },
    } as IAdapterContext;
});
