import React from 'react';
import { create } from 'react-test-renderer';

import Menu from '../index';

describe('Menu', () => {
    test('snapshot', () => {
        const c = create(<Menu setAboutVisible={() => {}} setMenuVisible={() => {}} data={() => {}} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
