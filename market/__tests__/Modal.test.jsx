import React from 'react';
import { create } from 'react-test-renderer';

import Modal from '../index';

const items = ['example.com', 'xn----8sb1bezcm.xn--p1ai', 'some.com'];

test('snapshot', () => {
    const c = create(<Modal setFormVisible={() => true} items={items} setItems={() => true} />);
    expect(c.toJSON()).toMatchSnapshot();
});
