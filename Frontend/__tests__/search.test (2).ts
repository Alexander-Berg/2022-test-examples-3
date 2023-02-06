import { initialSearchData, finish, fetch, toggle, searchReducer } from '../search';

describe('Search reducer', () => {
    describe('finishSearch', () => {
        it('returns same state when chat is not found', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
            };

            const newState = searchReducer(initialState, finish(chatId, {}));

            expect(newState).toBe(initialState);
        });

        it('returns new state with result', () => {
            const chatId = 'my_chat';
            const initialState = {
                [chatId]: initialSearchData,
            };
            const results: { messages: Client.Search.Messages } = {
                messages: {
                    items: [],
                    total: 0,
                    page: 0,
                    limit: 10,
                    pages: 0,
                },
            };

            const newState = searchReducer(initialState, finish(chatId, results));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [chatId]: {
                    results,
                    isFetching: false,
                    isFetched: true,
                },
            });
        });

        it('returns new state without current', () => {
            const chatId = 'my_chat';
            const initialState = {
                [chatId]: {
                    ...initialSearchData,
                },
            };

            const newState = searchReducer(initialState, finish(chatId, {}));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [chatId]: {
                    results: {},
                    isFetching: false,
                    isFetched: true,
                },
            });
        });

        it('returns new state with isFetching=false', () => {
            const chatId = 'my_chat';
            const initialState = {
                [chatId]: {
                    ...initialSearchData,
                    isFetching: true,
                    isFetched: true,
                },
            };

            const newState = searchReducer(initialState, finish(chatId, {}));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [chatId]: {
                    results: {},
                    isFetching: false,
                    isFetched: true,
                },
            });
        });
    });

    describe('fetchSearch', () => {
        it('returns same state when chat is not found', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
            };

            const newState = searchReducer(initialState, fetch(chatId));

            expect(newState).toBe(initialState);
        });

        it('returns new state with isFetching=true', () => {
            const chatId = 'my_chat';
            const initialState = {
                [chatId]: initialSearchData,
            };

            const newState = searchReducer(initialState, fetch(chatId));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                [chatId]: {
                    results: {},
                    isFetching: true,
                    isFetched: false,
                },
            });
        });
    });

    describe('toggleSearch', () => {
        it('returns new state without item when visible=false', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
                [chatId]: initialSearchData,
            };

            const newState = searchReducer(initialState, toggle(chatId, false));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                another_chat: initialSearchData,
            });
        });

        it('returns new state with init item when item is not found and visible=true', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
            };

            const newState = searchReducer(initialState, toggle(chatId, true));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                another_chat: initialSearchData,
                [chatId]: initialSearchData,
            });
        });

        it('returns same state when item is found and visible=true', () => {
            const chatId = 'my_chat';
            const initialState = {
                [chatId]: initialSearchData,
            };

            const newState = searchReducer(initialState, toggle(chatId, true));

            expect(newState).toBe(initialState);
        });

        it('returns new state with init item when item is not found and visible is not set', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
            };

            const newState = searchReducer(initialState, toggle(chatId));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                another_chat: initialSearchData,
                [chatId]: initialSearchData,
            });
        });

        it('returns new state without item when item is found and visible is not set', () => {
            const chatId = 'my_chat';
            const initialState = {
                another_chat: initialSearchData,
                [chatId]: initialSearchData,
            };

            const newState = searchReducer(initialState, toggle(chatId));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                another_chat: initialSearchData,
            });
        });
    });
});
