import * as React from 'react';
import 'jest';
import { shallow } from 'enzyme';
import { EcomOneClickBuyButton } from '../EcomOneClickBuyButton';
import { ICartItem } from '../../CartItem/CartItem';
import { ProductItemStub } from '../../ProductItemStub/ProductItemStub';

const productItemData: ICartItem = {
    product: {
        price: {
            current: 779,
        },
        href: '/turbo?utm_source=turbo_turbo&text=http%3A//ymturbo.t-dir.com/catalog/t-shirts/t-shirt-men-s-fire/',
        description: 'Россия "Grishko" Футболка Мужской Огонь',
        meta: 'CiBA8B6Z9mxJFYsBdkeyIPTyBbQW3G3a9FwO742PmYdVGxL0AQjV7L3zBRLrAQoDMTY3EkHQoNC+0YHRgdC40Y8gIkdyaXNoa28iINCk0YPRgtCx0L7Qu9C60LAg0JzRg9C20YHQutC+0Lkg0J7Qs9C+0L3RjBo+aHR0cHM6Ly95bXR1cmJvLnQtZGlyLmNvbS9jYXRhbG9nL3Qtc2hpcnRzL3Qtc2hpcnQtbWVuLXMtZmlyZS8iVwpPLy9hdmF0YXJzLm1kcy55YW5kZXgubmV0L2dldC10dXJiby8yOTM2MTU1L3J0aGJmZTk1NGViM2NhNTkzMGI2N2M0OTYxNDdmMTliMTVkLxDYAhj+BCoDNzc5OgNSVVI=',
        id: '167',
        thumb: {
            src: '//avatars.mds.yandex.net/get-turbo/2936155/rthbfe954eb3ca5930b67c496147f19b15d/',
            height: 638,
            width: 344,
        },
    },
    count: 1,
    maxCount: 100,
    productImageStub: <ProductItemStub page={1} />,
    meta: 'CiD8e19VFm7ZgargNI+DuBwwqFBgV3fj1deCOwRjy3UUBBKbAQjV7L3zBRKSAQoDMTY3EkHQoNC+0YHRgdC40Y8gIkdyaXNoa28iINCk0YPRgtCx0L7Qu9C60LAg0JzRg9C20YHQutC+0Lkg0J7Qs9C+0L3RjBoDNzc5KgNSVVIyPmh0dHBzOi8veW10dXJiby50LWRpci5jb20vY2F0YWxvZy90LXNoaXJ0cy90LXNoaXJ0LW1lbi1zLWZpcmUv',
    localization: {
        count: 'Кол-во',
        madeByOrder: 'На заказ',
    },
};

describe('Компонент EcomOneClickBuyButton', () => {
    it('рендерится без ошибок', () => {
        shallow(<EcomOneClickBuyButton
            text="Купить в 1 клик"
            modalId="modalId"
            cartItem={productItemData}
        />);
    });
});
