import * as AuthIdStore from '../authId';
import { generateGuid } from './mock/common';

describe('AuthId reducer', () => {
    describe('#setAuth', () => {
        it('set new authId', () => {
            const initialState = '';
            const guid = generateGuid();

            const newState = AuthIdStore.authIdReducer(initialState, AuthIdStore.set(guid));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual(guid);
        });
    });
});
