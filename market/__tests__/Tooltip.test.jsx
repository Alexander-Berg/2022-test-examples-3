import React from 'react';
import { create } from 'react-test-renderer';

import Tooltip from '../index';

test('tooltip shown', () => {
    const state = { visible: true, withError: false };
    const c = create(<Tooltip state={state} setTooltipState={() => true} />);
    expect(c.toJSON()).toMatchSnapshot();
});

test('tooltip hidden', () => {
    const state = { visible: false, withError: false };
    const c = create(<Tooltip state={state} setTooltipState={() => true} />);
    expect(c.toJSON()).toMatchSnapshot();
});

test('tooltip with error', () => {
    const state = { visible: true, withError: true };
    const c = create(<Tooltip state={state} setTooltipState={() => true} />);
    expect(c.toJSON()).toMatchSnapshot();
});
