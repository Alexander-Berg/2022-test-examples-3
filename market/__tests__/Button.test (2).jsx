import React from 'react';
import { create } from 'react-test-renderer';

import Button from '../index';

describe('Button', () => {
    test('with type "button"', () => {
        const c = create(<Button buttonType="button" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with type "submit"', () => {
        const c = create(<Button buttonType="submit" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with type "reset"', () => {
        const c = create(<Button buttonType="reset" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('primary', () => {
        const c = create(<Button primary buttonType="button" />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('large', () => {
        const c = create(<Button isLarge buttonType="button" />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
