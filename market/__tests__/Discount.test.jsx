import React from 'react';
import { create } from 'react-test-renderer';

import Discount from '../index';

describe('Delivery', () => {
    test('snapshot: with value `-3.5%`', () => {
        const c = create(<Discount value="3.5" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: with value `-5%`', () => {
        const c = create(<Discount value="5" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: with value `-3.5%`(tile)', () => {
        const c = create(<Discount value="3.5" isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: with value `-5%`(tile)', () => {
        const c = create(<Discount value="5" isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
