import TestMock from './index';
import EmulatedState from '../../state/inMemory';
import {makeRequest} from '../../mockExecutor';

it('should work', async () => {
    const state = new EmulatedState();
    const mock = new TestMock(state);
    await mock.init();
    await expect(mock.action(makeRequest(`/?type=foo`))).resolves.toBe(null);
    await expect(mock.action(makeRequest(`/?type=bar`))).resolves.toBe(123);
    await expect(mock.action(makeRequest(`/?type=baz`))).resolves.toBe(456);
    await state.set('foo', {bar: 777});
    await expect(mock.action(makeRequest(`/?type=bar`))).resolves.toBe(777);
});
