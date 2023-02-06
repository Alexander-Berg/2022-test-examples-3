import test from 'ava';
import reducer from '../../src/redux/reducers';
import { APP_TEST } from '../../src/redux/actions/types';

test.beforeEach(() => {
    global.window = {};
});

test('Reducer', t => {
    const action = { type: APP_TEST, payload: true };
    let state = { test: false };

    state = reducer(state, action);

    t.true(state.test);
});
