import { recentSearchReducer, add, clear, init, MAX_COUNT_RECENT_SEARCH } from '../recentSearch';
import { userFactory } from './mock/user';

const createRecentSearchChat = (chatId = 'chat_id'): Client.Search.ChatEntity => {
    return {
        entity: 'chat',
        type: 'chats',
        matches: {},
        data: ({
            name: 'chat',
            version: 0,
            chat_id: chatId,
            description: '',
        } as unknown as Yamb.Chat),
        member_count: 3,
    };
};

const createRecentSearchUser = (): Client.Search.UserEntity => {
    return {
        entity: 'user',
        type: 'users',
        matches: {},
        data: (userFactory() as unknown as Yamb.User),
    };
};

const createInitialState = () => ([]);

describe('RecentSearch reducer', () => {
    describe('#Add state', () => {
        it('should add state', () => {
            const initialState = createInitialState();

            const data = createRecentSearchChat();

            const action = add(data);

            const state1 = recentSearchReducer(initialState, action);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject([data]);
        });

        it('should add state and remove end', () => {
            const chats = new Array(MAX_COUNT_RECENT_SEARCH - 2).fill(null).map(
                (_, index) => createRecentSearchChat('chat_id' + index),
            );

            const initialState = [
                createRecentSearchUser(),
                ...chats,
                createRecentSearchUser(),
            ];

            const expectedState = [
                createRecentSearchChat(),
                ...(initialState.slice(0, -1)),
            ];

            const data = createRecentSearchChat();

            const action = add(data);

            const state1 = recentSearchReducer(initialState, action);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject(expectedState);
        });

        it('should add state equals id', () => {
            const initialState = [createRecentSearchChat()];

            const action = add(createRecentSearchChat());

            const state1 = recentSearchReducer(initialState, action);

            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject(initialState);
        });
    });

    describe('#Clear state', () => {
        it('should clear state', () => {
            const initialState = createInitialState();

            const state = [
                createRecentSearchChat(),
                createRecentSearchUser(),
            ];

            const action = clear();

            const state1 = recentSearchReducer(state, action);

            expect(state1).not.toBe(state);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject(initialState);
        });
    });

    describe('#Init state', () => {
        it('should init state', () => {
            const initialState = createInitialState();

            const data = [
                createRecentSearchChat(),
                createRecentSearchUser(),
            ];

            const action = init(data);

            const state1 = recentSearchReducer(initialState, action);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject(data);
        });
    });
});
