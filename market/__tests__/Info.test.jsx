import React from 'react';
import { create } from 'react-test-renderer';

import Info from '../index';

describe('Info', () => {
    test('model found', () => {
        const price = { value: 75000, currencyName: 'руб.' };
        const c = create(<Info data={() => ({ price })} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('model not found', () => {
        const price = { offersCount: 10 };
        const c = create(<Info data={() => ({ price })} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
