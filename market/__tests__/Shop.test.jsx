import React from 'react';
import { create } from 'react-test-renderer';

import Shop from '../index';

describe('Shop', () => {
    test('with all data', () => {
        const isOnMarket = true;
        const rating = 4.3;
        const name = 'Яндекс.Маркет';

        const c = create(<Shop rating={rating} name={name} isOnMarket={isOnMarket} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('no on Market', () => {
        const isOnMarket = false;
        const rating = 2.9;
        const name = 'Электрозон';

        const c = create(<Shop rating={rating} name={name} isOnMarket={isOnMarket} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with only name', () => {
        const name = 'Яндекс.Маркет';

        const c = create(<Shop name={name} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with only rating', () => {
        const rating = 3.5;

        const c = create(<Shop rating={rating} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
