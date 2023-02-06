import { SET_AUTH } from '../../../../../src/store/types';
import userReducer from '../../../../../src/store/reducers/user';

const getMockedState = ({ auth = true }) => ({
    id: '123',
    auth
});

describe('store/reducers/user', () => {
    it('should return default state if action`s type is unknown', () => {
        expect(userReducer(undefined, { type: 'default' })).toEqual({});
    });

    it('SET_AUTH', () => {
        const state = getMockedState({});
        const newState = userReducer(state, { type: SET_AUTH, payload: false });

        expect(newState).toEqual(Object.assign({}, state, { auth: false }));
        expect(newState).not.toBe(state);
    });
});
