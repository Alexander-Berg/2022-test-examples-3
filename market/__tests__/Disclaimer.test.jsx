import React from 'react';
import { create } from 'react-test-renderer';

import Disclaimer from '../index';

describe('Disclaimer', () => {
    test('without message', () => {
        const c = create(<Disclaimer info="" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with message', () => {
        const c = create(<Disclaimer info="some message" />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
