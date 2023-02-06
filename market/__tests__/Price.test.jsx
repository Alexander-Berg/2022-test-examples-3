import React from 'react';
import { create } from 'react-test-renderer';

import Price from '../index';

describe('Price', () => {
    test('with discount', () => {
        const data = {
            value: 20000,
            base: 10000,
            discount: '5.5',
            currencyName: 'руб.',
        };
        const c = create(<Price data={data} />);

        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with only current price', () => {
        const data = {
            value: 20000,
            currencyName: 'руб.',
        };
        const c = create(<Price data={data} />);

        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with discount tile', () => {
        const data = {
            value: 20000,
            base: 10000,
            discount: '5.5',
            currencyName: 'руб.',
        };
        const c = create(<Price data={data} isTile />);

        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with only current price tile', () => {
        const data = {
            value: 20000,
            currencyName: 'руб.',
        };
        const c = create(<Price data={data} isTile />);

        expect(c.toJSON()).toMatchSnapshot();
    });
});
