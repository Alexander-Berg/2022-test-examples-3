import React from 'react';
import { create } from 'react-test-renderer';

import Button from '../index';

describe('Button', () => {
    test('button', () => {
        const c = create(<Button />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
