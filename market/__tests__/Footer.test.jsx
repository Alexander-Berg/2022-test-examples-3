import React from 'react';
import { create } from 'react-test-renderer';

import Footer from '../index';

const links = {
    shops: 'url',
    market: 'url',
    feedback: 'url',
};

describe('Footer', () => {
    test('snapshot', () => {
        const c = create(<Footer links={links} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
