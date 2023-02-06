import deepFreeze from 'deep-freeze';
import { UPDATE_USERS } from '../../../../../src/store/action-types';
import users from '../../../../../src/store/reducers/users';

describe('users reducer', () => {
    let defaultState;
    beforeEach(() => {
        defaultState = {};
        deepFreeze(defaultState);
    });

    it('состояние по умолчанию', () => {
        expect(users(undefined, {})).toEqual(defaultState);
    });

    it('UPDATE_USERS', () => {
        const newUsers = [
            {
                uid: 'uid-1',
                displayName: 'Первый блин',
                paid: 0
            },
            {
                uid: 'uid-2',
                displayName: 'Фторой',
                paid: 1
            }
        ];
        deepFreeze(newUsers);
        expect(users(defaultState, {
            type: UPDATE_USERS,
            payload: {
                users: newUsers
            }
        })).toEqual({
            'uid-1': newUsers[0],
            'uid-2': newUsers[1]
        });
    });

    it('UPDATE_USERS не перезаписывает инфу пользователя если такой UID уже есть', () => {
        const stateBefore = {
            uid1: {
                uid: 'uid1',
                displayName: 'Первый блин'
            },
            uid2: {
                uid: 'uid2',
                displayName: 'Фторой'
            }
        };
        deepFreeze(stateBefore);
        const newUsers = [
            {
                uid: 'uid2',
                displayName: 'WAT?'
            },
            {
                uid: 'uid3',
                displayName: 'Третий'
            }
        ];
        deepFreeze(newUsers);
        expect(users(stateBefore, {
            type: UPDATE_USERS,
            payload: {
                users: newUsers
            }
        })).toEqual(
            Object.assign({}, stateBefore, {
                uid3: {
                    uid: 'uid3',
                    displayName: 'Третий'
                }
            })
        );
    });
});
