import React from 'react';
import { create } from 'react-test-renderer';

import Image from '../index';

describe('Image', () => {
    test('snapshot: with an `imageUrl` and `altName` attribute', () => {
        const c = create(<Image imageUrl="https://example.com" altName="some name" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: without `altName` attribute', () => {
        const c = create(<Image imageUrl="https://example.com" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('snapshot: with fallback image', () => {
        const c = create(<Image imageUrl="" />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
