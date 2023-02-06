import React from 'react';
import { create } from 'react-test-renderer';

import Rating from '../index';

describe('Rating', () => {
    test('snapshot: without rating', () => {
        const c = create(<Rating value={0} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: 1 star', () => {
        const c = create(<Rating value={1} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: 2 stars', () => {
        const c = create(<Rating value={2} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: 3 stars', () => {
        const c = create(<Rating value={3} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: 4 stars', () => {
        const c = create(<Rating value={4} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: 5 stars', () => {
        const c = create(<Rating value={4} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
