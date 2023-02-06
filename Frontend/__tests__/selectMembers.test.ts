import { clear, update, selectMembersReducer } from '../selectMembers';
import { userFactory } from './mock/user';

describe('Select members reducer', () => {
    const user = userFactory();

    describe('clear select members', () => {
        it('returns new state with clear members', () => {
            const initialState = {
                query: '1',
                suggestedMembers: [user],
                selectedMembers: [user],
                ignoredUsersIds: ['foo'],
            };

            const newState = selectMembersReducer(initialState, clear());

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual({
                query: '',
                suggestedMembers: [],
                selectedMembers: [],
                ignoredUsersIds: [],
            });
        });
    });

    describe('update select members', () => {
        it('returns new state with new selected members', () => {
            const initialState = {
                query: '',
                suggestedMembers: [],
                selectedMembers: [],
                ignoredUsersIds: [],
            };

            const newState = selectMembersReducer(initialState, update({
                query: 'q',
                suggestedMembers: [user],
                selectedMembers: [user],
            }));

            expect(newState).not.toBe(initialState);
            expect(newState).toEqual({
                query: 'q',
                suggestedMembers: [user],
                selectedMembers: [user],
                ignoredUsersIds: [],
            });
        });
    });
});
