/* eslint-disable import/first */
const getGlobalParamMock = jest.fn();

jest.mock('../../../../../shared/lib/globalParams', () => ({
    getGlobalParam: getGlobalParamMock,
}));

const origGetGlobalParam = jest.requireActual('../../../../../shared/lib/globalParams').getGlobalParam;
import { combineActions } from '@yandex-chats/redux-glaze';
import { Role } from '../../../../constants/relation';
import { bucketsReducer, update as updateBuckets } from '../../../buckets';
import { chatsReducer, updateChat, updateRelations } from '../../../chats';
import conversationsReducer from '../../../conversations';
import { removeChat, updateHistory } from '../../../sharedActions';
import { bucketsMockFactory } from '../../../__tests__/mock/buckets';
import { chatsMockFactory } from '../../../__tests__/mock/chat';
import { generateGuid } from '../../../__tests__/mock/common';
import { historyMockFactory } from '../../../__tests__/mock/history';
import { messagesMockFactory } from '../../../__tests__/mock/messages';
import { stateMockFactory } from '../../../__tests__/mock/state';
import { usersMockFactory } from '../../../__tests__/mock/user';
import { mutate } from '../../../__tests__/mock/utils';
import { unreadCounterMiddlewareFactory } from '..';
import { configReducer, update as updateConfig } from '../../../config';

describe('#unreadCounterCache', () => {
    const authId = generateGuid();
    const stateMock = stateMockFactory();
    const chatsMock = chatsMockFactory({ authId });
    const usersMock = usersMockFactory();
    const bucketsMock = bucketsMockFactory();
    const messagesMock = messagesMockFactory();
    const historyMock = historyMockFactory();

    const [currentUser] = usersMock.createUnlimited()(authId);

    function baseStateFactory(params: {
        chatMutings?: string[],
        blacklisted?: string[],
        hiddenPrivateChats?: Record<string, number>,
        groupChatsIds?: string[],
        privateChatsIds?: string[],
        groupChats?: APIv3.Chat[],
        unreadCounterMiddleware?: ReturnType<typeof unreadCounterMiddlewareFactory>,
    } = {}) {
        const unreadCounterMiddleware = params.unreadCounterMiddleware ||
            unreadCounterMiddlewareFactory({ nsFilter: undefined });
        const groupChatsIds = params.groupChatsIds || chatsMock.generateGroupsIds(4, 0);
        const privateChatsIds = params.privateChatsIds || chatsMock.generatePrivateId(4);
        const groupChats = params.groupChats || chatsMock.createGroupChat({
            relations: chatsMock.createRelation([], 1, Role.MEMBER),
        })(...groupChatsIds);

        const privateChats = chatsMock.createPrivateChat()(...privateChatsIds);

        const state = stateMock.createState({
            authId,
            buckets: bucketsMock.createState({
                hidden_private_chats: bucketsMock.createHiddenPrivateChats(params?.hiddenPrivateChats || {}),
                restrictions: bucketsMock.createRestrictionsBucket({
                    blacklist: params?.blacklisted || [],
                }),
                chat_mutings: bucketsMock.createChatMutings(params?.chatMutings || []),
            }),
            metastore: {
                unreadCounters: {
                    byChatId: {},
                    byOrg: {},
                    updateToken: {},
                },
            },
        });

        unreadCounterMiddleware({ type: 'init' }, state, state);

        return {
            groupChatsIds,
            privateChatsIds,
            groupChats,
            privateChats,
            state,
            unreadCounterMiddleware,
        };
    }

    function scenarioBaseUpdateHistory(data: ReturnType<typeof baseStateFactory>) {
        const {
            groupChatsIds,
            privateChatsIds,
            groupChats,
            privateChats,
            state,
            unreadCounterMiddleware,
        } = data;

        const lastSeenByMeTs = messagesMock.nextTimestamp();
        const lastTimestamp = messagesMock.nextTimestamp();
        const chatIds = [...groupChatsIds, ...privateChatsIds];

        const action = updateHistory({
            authId,
            users: [currentUser],
            chats: [...groupChats, ...privateChats],
            histories: historyMock.createChatHistory({
                last_seqno: 2,
                last_timestamp: lastTimestamp,
                last_seen_by_me_seqno: 1,
                last_seen_by_me_timestamp: lastSeenByMeTs,
            })(...chatIds),
        });

        const nextState = {
            ...state,
            chats: chatsReducer(state.chats, action),
            conversations: conversationsReducer(state.conversations, action),
        };

        const { unreadCounters: prevUnreadCounters } = state.metastore;

        unreadCounterMiddleware(action, nextState, state);

        return {
            ...data,
            chatIds,
            action,
            nextState,
            prevUnreadCounters,
            lastSeenByMeTs,
            lastTimestamp,
            unreadCounterMiddleware,
        };
    }

    describe('updateHistory', () => {
        it('should be updated', () => {
            const {
                action,
                nextState,
                prevUnreadCounters,
                chatIds,
            } = scenarioBaseUpdateHistory(baseStateFactory());

            expect(action.$feedback.changedChatIds.size).toBe(8);

            const { unreadCounters } = nextState.metastore;

            chatIds.forEach((chatId) => {
                expect(unreadCounters.byChatId[chatId]).toBe(1);
                expect(action.$feedback.changedChatIds.has(chatId)).toBeTruthy();
            });

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 8,
                chatsCount: 8,
            });
        });

        it('should be updated with unmuted', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                action,
                nextState,
                prevUnreadCounters,
                chatIds,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            expect(action.$feedback.changedChatIds.size).toBe(8);

            const { unreadCounters } = nextState.metastore;

            chatIds.forEach((chatId) => {
                expect(unreadCounters.byChatId[chatId]).toBe(1);
                expect(action.$feedback.changedChatIds.has(chatId)).toBeTruthy();
            });

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
            });
        });

        it('should be updated if seenmarker increased', () => {
            const {
                nextState: state,
                chatIds,
                lastTimestamp,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory());

            const action = updateHistory({
                authId,
                users: [],
                chats: [],
                histories: historyMock.createChatHistory({
                    last_seqno: 2,
                    last_timestamp: lastTimestamp,
                    last_seen_by_me_seqno: 2,
                    last_seen_by_me_timestamp: lastTimestamp,
                })(chatIds[0]),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(1);
            expect(action.$feedback.changedChatIds.has(chatIds[0])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[chatIds[0]]).toBe(0);

            chatIds.slice(1).forEach((chatId) => {
                expect(unreadCounters.byChatId[chatId]).toBe(1);
            });

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });
        });

        it('should be updated if messages count was increased (muted change)', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                nextState: state,
                chatIds,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            const action = updateHistory({
                authId,
                users: [],
                chats: [],
                histories: historyMock.createChatHistory({
                    last_seqno: 5,
                    last_timestamp: messagesMock.nextTimestamp(),
                })(chatIds[0]),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(1);
            expect(action.$feedback.changedChatIds.has(chatIds[0])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[chatIds[0]]).toBe(4);

            chatIds.slice(1).forEach((chatId) => {
                expect(unreadCounters.byChatId[chatId]).toBe(1);
            });

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 11,
                unmuted: 7,
                chatsCount: 7,
            });
        });

        it('should be updated if messages count was increased', () => {
            const {
                nextState: state,
                chatIds,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory());

            const action = updateHistory({
                authId,
                users: [],
                chats: [],
                histories: historyMock.createChatHistory({
                    last_seqno: 5,
                    last_timestamp: messagesMock.nextTimestamp(),
                })(chatIds[0]),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(1);
            expect(action.$feedback.changedChatIds.has(chatIds[0])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[chatIds[0]]).toBe(4);

            chatIds.slice(1).forEach((chatId) => {
                expect(unreadCounters.byChatId[chatId]).toBe(1);
            });

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 11,
                unmuted: 11,
                chatsCount: 8,
            });
        });
    });

    describe('combined actions', () => {
        it('updateHistory + updateBuckets', () => {
            const groupChatsIds = [
                ...chatsMock.generateGroupsIds(1, 0),
                ...chatsMock.generateGroupsIds(2, 2),
                ...chatsMock.generateGroupsIds(1, 3),
            ];

            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[2]],
                unreadCounterMiddleware: unreadCounterMiddlewareFactory({ nsFilter: [2, 3] }),
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
                filteredByNS: {
                    total: 3,
                    unmuted: 2,
                    chatsCount: 2,
                },
            });

            const action1 = updateHistory({
                authId,
                users: [],
                chats: [
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[0]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                    //Чат добавляется в 1 орг
                    mutate(state.chats[groupChatsIds[1]], {
                        version: 2,
                        organization_ids: { '1': null, '0': null },
                    }),
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[2]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                ],
                histories: historyMock.createChatHistory()(
                    {
                        chat_id: groupChatsIds[0],
                        last_seqno: 3,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[1],
                        last_seqno: 5,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[2],
                        last_seqno: 4,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                ),
            });

            const action2 = updateBuckets([{
                bucket_name: 'chat_mutings',
                bucket_value: {
                    [groupChatsIds[1]]: {
                        mute: true,
                    },
                },
                version: (state.buckets.chat_mutings?.version || 0) + 1,
            }]);

            const action = combineActions(action1, action2);

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action1, nextState, state);

            expect(action1.$feedback.changedChatIds.size).toBe(3);
            expect(action1.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();
            expect(action1.$feedback.changedChatIds.has(groupChatsIds[1])).toBeTruthy();
            expect(action1.$feedback.changedChatIds.has(groupChatsIds[2])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[groupChatsIds[0]]).toBe(2);
            expect(unreadCounters.byChatId[groupChatsIds[1]]).toBe(4);
            expect(unreadCounters.byChatId[groupChatsIds[2]]).toBe(3);

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 9,
                unmuted: 5,
                chatsCount: 5,
                filteredByNS: {
                    total: 5,
                    unmuted: 1,
                    chatsCount: 1,
                },
            });
            expect(unreadCounters.byOrg[1]).toMatchObject({
                total: 9,
                unmuted: 5,
                chatsCount: 2,
                filteredByNS: {
                    total: 7,
                    unmuted: 3,
                    chatsCount: 1,
                },
            });
        });
    });

    describe('change chat organization', () => {
        it('updateHistory', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[2]],
            }));

            const action = updateHistory({
                authId,
                users: [],
                chats: [
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[0]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                    //Чат добавляется в 1 орг
                    mutate(state.chats[groupChatsIds[1]], {
                        version: 2,
                        organization_ids: { '1': null, '0': null },
                    }),
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[2]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                ],
                histories: historyMock.createChatHistory()(
                    {
                        chat_id: groupChatsIds[0],
                        last_seqno: 3,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[1],
                        last_seqno: 5,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[2],
                        last_seqno: 4,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                ),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(3);
            expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();
            expect(action.$feedback.changedChatIds.has(groupChatsIds[1])).toBeTruthy();
            expect(action.$feedback.changedChatIds.has(groupChatsIds[2])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[groupChatsIds[0]]).toBe(2);
            expect(unreadCounters.byChatId[groupChatsIds[1]]).toBe(4);
            expect(unreadCounters.byChatId[groupChatsIds[2]]).toBe(3);

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 9,
                unmuted: 9,
                chatsCount: 6,
            });
            expect(unreadCounters.byOrg[1]).toMatchObject({
                total: 9,
                unmuted: 6,
                chatsCount: 2,
            });
        });
    });

    describe('nsFilter', () => {
        it('updateHistory', () => {
            const groupChatsIds = [
                ...chatsMock.generateGroupsIds(1, 0),
                ...chatsMock.generateGroupsIds(2, 2),
                ...chatsMock.generateGroupsIds(1, 3),
            ];

            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[2]],
                unreadCounterMiddleware: unreadCounterMiddlewareFactory({ nsFilter: [2, 3] }),
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
                filteredByNS: {
                    total: 3,
                    unmuted: 2,
                    chatsCount: 2,
                },
            });

            const action = updateHistory({
                authId,
                users: [],
                chats: [
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[0]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                    //Чат добавляется в 1 орг
                    mutate(state.chats[groupChatsIds[1]], {
                        version: 2,
                        organization_ids: { '1': null, '0': null },
                    }),
                    //Чат удаляется из 0 и переходит в 1 орг
                    mutate(state.chats[groupChatsIds[2]], {
                        version: 2,
                        organization_ids: { '1': null },
                    }),
                ],
                histories: historyMock.createChatHistory()(
                    {
                        chat_id: groupChatsIds[0],
                        last_seqno: 3,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[1],
                        last_seqno: 5,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                    {
                        chat_id: groupChatsIds[2],
                        last_seqno: 4,
                        last_timestamp: messagesMock.nextTimestamp(),
                    },
                ),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(3);
            expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();
            expect(action.$feedback.changedChatIds.has(groupChatsIds[1])).toBeTruthy();
            expect(action.$feedback.changedChatIds.has(groupChatsIds[2])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[groupChatsIds[0]]).toBe(2);
            expect(unreadCounters.byChatId[groupChatsIds[1]]).toBe(4);
            expect(unreadCounters.byChatId[groupChatsIds[2]]).toBe(3);

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 9,
                unmuted: 9,
                chatsCount: 6,
                filteredByNS: {
                    total: 5,
                    unmuted: 5,
                    chatsCount: 2,
                },
            });
            expect(unreadCounters.byOrg[1]).toMatchObject({
                total: 9,
                unmuted: 6,
                chatsCount: 2,
                filteredByNS: {
                    total: 7,
                    unmuted: 4,
                    chatsCount: 1,
                },
            });
        });
    });

    describe('removeChat', () => {
        describe('updateChat', () => {
            it('all counters should be decreased when user gone', () => {
                const groupChatsIds = chatsMock.generateGroupsIds(4, 0);

                const {
                    nextState: state,
                    unreadCounterMiddleware,
                } = scenarioBaseUpdateHistory(baseStateFactory({
                    groupChatsIds,
                }));

                const chatId = groupChatsIds[0];

                const action = updateChat(
                    mutate(state.chats[chatId], {
                        relations: mutate(state.chats[chatId].relations, {
                            role: Role.GONE,
                            version: 2,
                        }),
                    }),
                );

                const nextState = {
                    ...state,
                    chats: chatsReducer(state.chats, action),
                };

                unreadCounterMiddleware(action, nextState, state);

                expect(action.$feedback.changedChatIds.size).toBe(1);
                expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();

                const { unreadCounters } = nextState.metastore;

                expect(unreadCounters.byOrg[0]).toMatchObject({
                    total: 7,
                    unmuted: 7,
                    chatsCount: 7,
                });
            });

            it('all counters should be increased when user become a member', () => {
                const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
                const groupChats = chatsMock.createGroupChat({
                    relations: chatsMock.createRelation([], 1, Role.MEMBER),
                })({
                    chat_id: groupChatsIds[0],
                    relations: chatsMock.createRelation([], 1, Role.GONE),
                }, ...groupChatsIds.slice(1));

                const {
                    nextState: state,
                    unreadCounterMiddleware,
                } = scenarioBaseUpdateHistory(baseStateFactory({
                    groupChatsIds,
                    groupChats,
                }));

                expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                    total: 7,
                    unmuted: 7,
                    chatsCount: 7,
                });

                const chatId = groupChatsIds[0];

                const action = updateChat(
                    mutate(state.chats[chatId], {
                        relations: mutate(state.chats[chatId].relations, {
                            role: Role.MEMBER,
                            version: 2,
                        }),
                    }),
                );

                const nextState = {
                    ...state,
                    chats: chatsReducer(state.chats, action),
                };

                unreadCounterMiddleware(action, nextState, state);

                expect(action.$feedback.changedChatIds.size).toBe(1);
                expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();

                const { unreadCounters } = nextState.metastore;

                expect(unreadCounters.byOrg[0]).toMatchObject({
                    total: 8,
                    unmuted: 8,
                    chatsCount: 8,
                });
            });
        });

        it('all counters should be decreased when chat was removed', () => {
            const groupChatsIds = [
                ...chatsMock.generateGroupsIds(2, 1),
                ...chatsMock.generateGroupsIds(2, 0),
            ];

            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                unreadCounterMiddleware: unreadCounterMiddlewareFactory({ nsFilter: [1] }),
                groupChatsIds,
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 8,
                chatsCount: 8,
                filteredByNS: {
                    total: 2,
                    unmuted: 2,
                    chatsCount: 2,
                },
            });

            const action = removeChat(groupChatsIds[0]);

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            expect(action.$feedback.changedChatIds.size).toBe(1);
            expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters.byChatId[groupChatsIds[0]]).toBe(0);

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
                filteredByNS: {
                    total: 1,
                    unmuted: 1,
                    chatsCount: 1,
                },
            });
        });
    });

    describe('role changed', () => {
        describe('updateRelations', () => {
            it('all counters should be decreased', () => {
                const groupChatsIds = chatsMock.generateGroupsIds(4, 0);

                const {
                    nextState: state,
                    unreadCounterMiddleware,
                } = scenarioBaseUpdateHistory(baseStateFactory({
                    groupChatsIds,
                }));

                const chatId = groupChatsIds[0];

                const action = updateRelations(
                    chatId,
                    mutate(state.chats[chatId].relations, {
                        role: Role.GONE,
                        version: 2,
                    }),
                );

                const nextState = {
                    ...state,
                    chats: chatsReducer(state.chats, action),
                };

                unreadCounterMiddleware(action, nextState, state);

                expect(action.$feedback.changedChatIds.size).toBe(1);
                expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();

                const { unreadCounters } = nextState.metastore;

                expect(unreadCounters.byOrg[0]).toMatchObject({
                    total: 7,
                    unmuted: 7,
                    chatsCount: 7,
                });
            });

            it('all counters should be increased', () => {
                const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
                const groupChats = chatsMock.createGroupChat({
                    relations: chatsMock.createRelation([], 1, Role.MEMBER),
                })({
                    chat_id: groupChatsIds[0],
                    relations: chatsMock.createRelation([], 1, Role.GONE),
                }, ...groupChatsIds.slice(1));

                const {
                    nextState: state,
                    unreadCounterMiddleware,
                } = scenarioBaseUpdateHistory(baseStateFactory({
                    groupChatsIds,
                    groupChats,
                }));

                expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                    total: 7,
                    unmuted: 7,
                    chatsCount: 7,
                });

                const chatId = groupChatsIds[0];

                const action = updateRelations(
                    chatId,
                    mutate(state.chats[chatId].relations, {
                        role: Role.MEMBER,
                        version: 2,
                    }),
                );

                const nextState = {
                    ...state,
                    chats: chatsReducer(state.chats, action),
                };

                unreadCounterMiddleware(action, nextState, state);

                expect(action.$feedback.changedChatIds.size).toBe(1);
                expect(action.$feedback.changedChatIds.has(groupChatsIds[0])).toBeTruthy();

                const { unreadCounters } = nextState.metastore;

                expect(unreadCounters.byOrg[0]).toMatchObject({
                    total: 8,
                    unmuted: 8,
                    chatsCount: 8,
                });
            });
        });
    });

    describe('blacklist', () => {
        it('all counters should be decreased if some user was blacklisted', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
            }));

            const action = updateBuckets([{
                bucket_name: 'restrictions',
                bucket_value: {
                    blacklist: [usersIds[0]],
                },
                version: (state.buckets.restrictions?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byChatId[privateChatsIds[0]]).toBe(0);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });
        });

        it('all counters should be increased if some user was unblacklisted', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
                blacklisted: [usersIds[0]],
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });

            const action = updateBuckets([{
                bucket_name: 'restrictions',
                bucket_value: {
                    blacklist: [],
                },
                version: (state.buckets.restrictions?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 8,
                chatsCount: 8,
            });
        });

        it('only total should be increased if some muted chat was unhidden', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
                blacklisted: [usersIds[0]],
                chatMutings: [privateChatsIds[0]],
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });

            const action = updateBuckets([{
                bucket_name: 'restrictions',
                bucket_value: {
                    blacklist: [],
                },
                version: (state.buckets.restrictions?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
            });
        });
    });

    describe('hidden', () => {
        it('all counters should be decreased if some chats was hidden', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                lastTimestamp,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
            }));

            const action = updateBuckets([{
                bucket_name: 'hidden_private_chats',
                bucket_value: {
                    [usersIds[0]]: lastTimestamp,
                },
                version: (state.buckets.hidden_private_chats?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byChatId[privateChatsIds[0]]).toBe(0);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });
        });

        it('all counters should be increased if some chats was unhidden', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
                hiddenPrivateChats: {
                    [usersIds[0]]: (messagesMock.currentTimestamp() + 1000) * 1000,
                },
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });

            const action = updateBuckets([{
                bucket_name: 'hidden_private_chats',
                bucket_value: {},
                version: (state.buckets.hidden_private_chats?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 8,
                chatsCount: 8,
            });
        });

        it('only total should be increased if some muted chat was unhidden', () => {
            const usersIds = usersMock.generateGuids(4);
            const privateChatsIds = chatsMock.generatePrivateIdWith(...usersIds);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                privateChatsIds,
                hiddenPrivateChats: {
                    [usersIds[0]]: (messagesMock.currentTimestamp() + 1000) * 1000,
                },
                chatMutings: [privateChatsIds[0]],
            }));

            expect(state.metastore.unreadCounters.byOrg[0]).toMatchObject({
                total: 7,
                unmuted: 7,
                chatsCount: 7,
            });

            const action = updateBuckets([{
                bucket_name: 'hidden_private_chats',
                bucket_value: {},
                version: (state.buckets.hidden_private_chats?.version || 0) + 1,
            }]);

            const nextState = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState, state);

            const { unreadCounters } = nextState.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
            });
        });
    });

    describe('hidden namespaces', () => {
        it('counters should be changed when hidden namepsaces was changed', () => {
            const groupChatsIds = [
                ...chatsMock.generateGroupsIds(1, 0),
                ...chatsMock.generateGroupsIds(1, 1),
                ...chatsMock.generateGroupsIds(1, 2),
                ...chatsMock.generateGroupsIds(1, 3),
            ];

            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            getGlobalParamMock.mockImplementation((param: string) => {
                if (param === 'backendConfig') {
                    return {
                        hidden_namespaces: [1, 2],
                    };
                }

                return origGetGlobalParam(param);
            });

            const action1 = updateConfig({
                ...state.config,
                hidden_namespaces: [1, 2],
            });

            const nextState = {
                ...state,
                config: configReducer(state.config, action1),
            };

            unreadCounterMiddleware(action1, nextState, state);

            expect(nextState.metastore.unreadCounters.byOrg[0]).toMatchObject({
                unmuted: 5,
                total: 6,
                chatsCount: 5,
            });

            getGlobalParamMock.mockImplementation((param: string) => {
                if (param === 'backendConfig') {
                    return {
                        hidden_namespaces: [0, 2, 3],
                    };
                }

                return origGetGlobalParam(param);
            });

            const action2 = updateConfig({
                ...nextState.config,
                hidden_namespaces: [0, 2, 3],
            });

            const nextState2 = {
                ...nextState,
                config: configReducer(state.config, action2),
            };

            unreadCounterMiddleware(action2, nextState2, nextState);

            expect(nextState2.metastore.unreadCounters.byOrg[0]).toMatchObject({
                unmuted: 5,
                total: 5,
                chatsCount: 5,
            });

            getGlobalParamMock.mockReset();
        });
    });

    describe('muttings', () => {
        it('unmuted chats count should be increased if some chats was unmuted', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            const action = updateBuckets([{
                bucket_name: 'chat_mutings',
                bucket_value: {},
                version: (state.buckets.chat_mutings?.version || 0) + 1,
            }]);

            const nextState2 = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState2, state);

            const { unreadCounters } = nextState2.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 8,
                chatsCount: 8,
            });
        });

        it('unmuted chats count should be decreased if some chats was muted', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            const action = updateBuckets([{
                bucket_name: 'chat_mutings',
                bucket_value: bucketsMock.createChatMutings([groupChatsIds[0], groupChatsIds[1]]).data,
                version: (state.buckets.chat_mutings?.version || 0) + 1,
            }]);

            const nextState2 = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState2, state);

            const { unreadCounters } = nextState2.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 6,
                chatsCount: 6,
            });
        });

        it('unmuted chats count should state same', () => {
            const groupChatsIds = chatsMock.generateGroupsIds(4, 0);
            const {
                nextState: state,
                unreadCounterMiddleware,
            } = scenarioBaseUpdateHistory(baseStateFactory({
                groupChatsIds,
                chatMutings: [groupChatsIds[0]],
            }));

            const action = updateBuckets([{
                bucket_name: 'chat_mutings',
                bucket_value: bucketsMock.createChatMutings([groupChatsIds[1]]).data,
                version: (state.buckets.chat_mutings?.version || 0) + 1,
            }]);

            const nextState2 = {
                ...state,
                buckets: bucketsReducer(state.buckets, action),
            };

            const { unreadCounters: prevUnreadCounters } = state.metastore;

            unreadCounterMiddleware(action, nextState2, state);

            const { unreadCounters } = nextState2.metastore;

            expect(unreadCounters).not.toBe(prevUnreadCounters);
            expect(unreadCounters.byOrg).not.toBe(prevUnreadCounters.byOrg);
            expect(unreadCounters.byChatId).toBe(prevUnreadCounters.byChatId);
            expect(unreadCounters.byOrg[0]).toMatchObject({
                total: 8,
                unmuted: 7,
                chatsCount: 7,
            });
        });
    });
});
