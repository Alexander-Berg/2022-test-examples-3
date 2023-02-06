import React from 'react';
import { create } from 'react-test-renderer';

import Blacklist from '../index';

test('snapshot', () => {
    const c = create(
        <Blacklist
            items={[]}
            setItems={() => {
                return true;
            }}
        />,
    );
    expect(c.toJSON()).toMatchSnapshot();
});
