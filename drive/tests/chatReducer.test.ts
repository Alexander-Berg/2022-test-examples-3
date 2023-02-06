import { _CHATS } from '../../components/ChatsCommon/types';
import { chatReducer, ChatReducerType, initialState } from '../chatReducer';
import {
    getChatsSelector,
    getIsFeedLoading,
    getLinesListData,
    getMyChats,
    getSelectedChatItem,
    getSelectedMenu,
    getSelectedMenuItem,
} from '../chatReducer/getters';
import {
    MENU_NAME,
    MENU_TYPE,
    MOCKED_CHAT,
    NEW_CHAT_NAME,
    STORE_FOR_LINES_LIST,
    STORE_FOR_TAG_UPDATE,
    TAG_DATA,
} from './chatReducer.mock';

describe('chatReducer', () => {
    describe('SET_CHATS works correct', () => {
        it('set my chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.MY,
                    },
                    error: null,
                }),
            };

            expect(getMyChats(store)?.chats?.length).toBe(1);
        });

        it('set all chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.ALL,
                    },
                    error: null,
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.ALL]?.chats;

            expect(chats?.length).toBe(1);
        });

        it('set new chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.NEW,
                    },
                    error: null,
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.NEW]?.chats;

            expect(chats?.length).toBe(1);
        });

        it('set custom chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.CUSTOM,
                    },
                    error: null,
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.CUSTOM]?.chats;

            expect(chats?.length).toBe(1);
        });

        it('set deferred chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.DEFFER,
                    },
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.DEFFER]?.chats;

            expect(chats?.length).toBe(1);
        });

        it('set archive chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        response: {
                            chats: [MOCKED_CHAT],
                        },
                        type: _CHATS.ARCHIVE,
                    },
                    error: null,
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.ARCHIVE]?.chats;

            expect(chats?.length).toBe(1);
        });

        it('set no chats', () => {
            const store = {
                Chats: chatReducer(initialState, {
                    type: ChatReducerType.SET_CHATS,
                    payload: {
                        type: _CHATS.DEFFER,
                        response: null,
                    },
                    error: null,
                }),
            };

            const chats = getChatsSelector(store)?.[_CHATS.DEFFER];

            expect(chats).toBeNull();
        });
    });

    describe('SET_SELECT_LIST works correct', () => {
        const store = {
            Chats: chatReducer(initialState, {
                type: ChatReducerType.SET_SELECT_LIST,
                payload: {
                    menu: MENU_NAME,
                    type: MENU_TYPE,
                },
            }),
        };

        it('correct menu type', () => {
            expect(getSelectedMenu(store)).toBe(MENU_TYPE);
        });

        it('correct menu name', () => {
            expect(getSelectedMenuItem(store)).toBe(MENU_NAME);
        });

        it('chat item was reset', () => {
            expect(getSelectedChatItem(store)).toBeNull();
        });
    });

    it('SELECT_CHAT_ITEM works correct', () => {
        const store = {
            Chats: chatReducer(initialState, {
                type: ChatReducerType.SELECT_CHAT_ITEM,
                payload: MOCKED_CHAT,
            }),
        };

        expect(getSelectedChatItem(store)?.id).toBe(MOCKED_CHAT.id);
    });

    it('SET_FEED_IS_LOADING works correct', () => {
        const store = {
            Chats: chatReducer(initialState, {
                type: ChatReducerType.SET_FEED_IS_LOADING,
                payload: true,
            }),
        };

        expect(getIsFeedLoading(store)).toBeTruthy();
    });

    it('CANCEL_CHAT_CALLBACK works correct', () => {
        const store = {
            Chats: chatReducer(initialState, {
                type: ChatReducerType.CANCEL_CHAT_CALLBACK,
                payload: null,
            }),
        };

        expect(getSelectedChatItem(store)).toBeNull();
    });

    it('UPDATE_CHAT_ITEM_TAG works correct', () => {
        const store = {
            Chats: chatReducer(STORE_FOR_TAG_UPDATE, {
                type: ChatReducerType.UPDATE_CHAT_ITEM_TAG,
                payload: {
                    chat: MOCKED_CHAT,
                    tag: TAG_DATA,
                },
            }),
        };

        expect(getSelectedChatItem(store)?.tag_data.name).toBe(NEW_CHAT_NAME);
    });

    it('getLinesListData works correct', () => {
        const startCount = STORE_FOR_LINES_LIST.Chats[_CHATS.MY]?.chats?.urgent?.length;
        const listData = getLinesListData(STORE_FOR_LINES_LIST);

        expect(listData[_CHATS.MY]?.info.totalItems).toBe(startCount);
    });
});
