import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruSnippet } from '@yandex-turbo/components/BeruSnippet/BeruSnippet';
import { BeruSearchList, IProps } from '../BeruSearchList';

const products: IProps['products'] = [
    {
        title: 'Телефон Sumsung',
        price: 135,
        oldPrice: 150,
        picture: { src: '', srcSet: '' },
        discountPercent: 10,
        opinions: 22,
        rating: 30,
        url: 'https://test.test',
        addToCartButton: {
            yaGoals: {
                '11': ['ADD_TO_CART'],
            },
            url: 'https://test2.test',
        },
        navigateGoal: {
            '11': [{
                id: '11',
                name: 'NAVIGATE',
            }],
        },
        visibilityGoal: [{
            id: '11',
            name: 'VISIBILITY',
        }],
    },
];

describe('BeruSearchList', () => {
    it('должен рендерится без ошибок', () => {
        const instance = shallow(<BeruSearchList products={products} />);

        expect(instance).toHaveLength(1);
    });

    it('должны правильно выставлять входные пропсы сниппета', () => {
        const instance = shallow(<BeruSearchList products={products} />);

        expect(instance.find(BeruSnippet).props()).toEqual({
            showDiscountBadge: true,
            showPrice: true,
            showRating: true,
            showTitle: true,
            suite: 'uno',
            theme: 'siete',
            orders: ['price', 'rating', 'title'],
            discountBadgeTheme: 'default',
            maxTitleLines: 3,
            ...products[0],
        });
    });

    it('должен отрисовывать список сниппетов', () => {
        const instance = shallow(<BeruSearchList products={[products[0], products[0]]} />);

        expect(instance.find(BeruSnippet)).toHaveLength(2);
    });
});
