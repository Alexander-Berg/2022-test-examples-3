import React from 'react';
import { create } from 'react-test-renderer';

import Delivery from '../index';

describe('Delivery', () => {
    test('snapshot: without price value', () => {
        const c = create(<Delivery info="Бесплатно" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: with price value', () => {
        const c = create(<Delivery info="400 руб." />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
