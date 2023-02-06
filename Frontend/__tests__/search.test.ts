import { getSearchData, isSearchVisible, getGlobalSearchItemData } from '../search';
import { initialSearchData } from '../../store/search';
import { AppState } from '../../store';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { usersMockFactory, employeesInfo } from '../../store/__tests__/mock/user';
import { conversationsMockFactory } from '../../store/__tests__/mock/conversations';
import { stateMockFactory } from '../../store/__tests__/mock/state';

describe('SearchSelectors', () => {
    const stateMock = stateMockFactory();
    const usersMock = usersMockFactory();
    const employeesInfoMock = employeesInfo();
    const [authUser] = usersMock.createUnlimited()();

    const chatsMock = chatsMockFactory({
        authId: authUser.guid,
    });
    const conversationsMock = conversationsMockFactory();

    describe('#getSearchData', () => {
        it('Should return data from state when item is found', () => {
            const chatId = 'my_chat';
            const item = {};

            const state = {
                search: {
                    [chatId]: item,
                },
            };

            // @ts-ignore
            expect(getSearchData(state, chatId)).toEqual(item);
        });

        it('Should return initialSearchData from state when item is not found', () => {
            const chatId = 'my_chat';

            const state = {
                search: {},
            };

            // @ts-ignore
            expect(getSearchData(state, chatId)).toEqual(initialSearchData);
        });
    });

    describe('#isSearchVisible', () => {
        it('Should return true from state when item found', () => {
            const chatId = 'my_chat';
            const item = {};

            const state = {
                search: {
                    [chatId]: item,
                },
            };

            // @ts-ignore
            expect(isSearchVisible(state, chatId)).toEqual(true);
        });

        it('Should return false from state when item is not found', () => {
            const chatId = 'my_chat';

            const state = {
                search: {},
            };

            // @ts-ignore
            expect(isSearchVisible(state, chatId)).toEqual(false);
        });
    });

    describe('#getSearchItemData', () => {
        it('should get user position in text', () => {
            const state = {
                search: {},
                chats: {},
                users: {},
            };

            expect(getGlobalSearchItemData(state as any, {
                data: {
                    employees_info: employeesInfoMock,
                    display_name: 'test user',
                    guid: '0',
                    version: 0,
                } as Yamb.User,
                entity: 'user',
                type: 'users',
                matches: {},
            }, '0')?.text).toEqual('developer');
        });

        it('should get user position in text if chat exists', () => {
            const [user2] = usersMock.createUnlimited()();

            const state: Partial<AppState> = stateMock.createState({
                authId: authUser.guid,
                search: {},
                chats: chatsMock.createState(chatsMock.createPrivateChatWith(user2.guid)),
                conversations: {},
                users: usersMock.createState(authUser, user2),
                messages: {
                    chats: {},
                } as AppState['messages'],
                typings: {},
                buckets: {
                    chat_mutings: {
                        data: {},
                        version: 0,
                    },
                } as AppState['buckets'],
            });

            expect(getGlobalSearchItemData(state as any, {
                data: {
                    employees_info: employeesInfoMock,
                    display_name: 'test user',
                    guid: user2.guid,
                    version: 0,
                } as Yamb.User,
                entity: 'user',
                type: 'users',
                matches: {},
            }, '0')?.text).toEqual('developer');
        });

        it('should get position in text for chat', () => {
            const [partner] = usersMock.createUnlimited()();
            const testChat = chatsMock.createPrivateChatWith(partner.guid);

            const state = stateMock.createState({
                authId: authUser.guid,
                search: {},
                chats: {
                    [testChat.chat_id]: testChat,
                },
                conversations: conversationsMock.createState(),
                users: {
                    [partner.guid]: {
                        ...authUser,
                        employees_info: employeesInfoMock,
                    },
                },
                typings: {},
            });

            expect(getGlobalSearchItemData(state as any, {
                data: (testChat as unknown as Yamb.Chat),
                entity: 'chat',
                type: 'chats',
                matches: {},
                member_count: 12,
            }, '0')?.text).toEqual('developer');
        });

        it('should not get saved messages position in text', () => {
            const [testChat] = chatsMock.createChatWithSelf()();

            const state = stateMock.createState({
                authId: authUser.guid,
                search: {},
                chats: {
                    [testChat.chat_id]: testChat,
                },
                conversations: conversationsMock.createState(),
                users: {
                    [authUser.guid]: {
                        ...authUser,
                        employees_info: employeesInfoMock,
                    },
                },
                typings: {},
            });

            expect(getGlobalSearchItemData(state as any, {
                data: (testChat as unknown as Yamb.Chat),
                entity: 'chat',
                type: 'chats',
                matches: {},
                member_count: 10,
            }, '0')?.text).toBeUndefined();
        });
    });
});
