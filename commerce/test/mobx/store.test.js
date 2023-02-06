import test from 'ava';

import getInitialStore from '../../src/mobx/state';
import configureStore from '../../src/mobx/configure-stores';

test('Configure mobx stores', t => {
    const initialState = getInitialStore();
    const stores = configureStore(initialState);

    t.is(typeof stores, 'object');
});
