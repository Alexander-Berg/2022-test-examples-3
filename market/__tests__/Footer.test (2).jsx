import React from 'react';
import { create } from 'react-test-renderer';

import Footer from '../index';

test('snapshot', () => {
    const c = create(<Footer />);
    expect(c.toJSON()).toMatchSnapshot();
});
