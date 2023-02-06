import * as conversationActions from '../conversations';
import { updateSeenMarker, updateHistory, updateMessages } from '../sharedActions';
import { generateGuid } from './mock/common';
import { conversationsMockFactory } from './mock/conversations';
import { historyMockFactory } from './mock/history';
import { messagesMockFactory } from './mock/messages';
import { usersMockFactory } from './mock/user';
import { mutate } from './mock/utils';

describe('conversation', () => {
    const conversationsMock = conversationsMockFactory();
    const historyMock = historyMockFactory();
    const messagesMock = messagesMockFactory();
    const usersMock = usersMockFactory();

    describe('updateSeenMarker', () => {
        const chatId1 = '0';
        const chatId2 = '1';
        const chatId3 = '2';

        it('должен обновить стейт для чата', () => {
            const state = conversationsMock.createState({
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            }, {
                last_seen_timestamp: 2,
                last_seen_by_me_timestamp: 2,
                last_seen_seqno: 2,
                last_seen_by_me_seqno: 2,
                chatId: chatId2,
            });

            const expected = conversationsMock.createState({
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            }, {
                last_seen_timestamp: 3,
                last_seen_by_me_timestamp: 3,
                last_seen_seqno: 3,
                last_seen_by_me_seqno: 3,
                chatId: chatId2,
            });

            expect(conversationActions.conversationsReducer(state,
                updateSeenMarker({
                    chat_id: chatId2,
                    last_seen_timestamp: 3,
                    last_seen_by_me_timestamp: 3,
                    last_seen_seqno: 3,
                    last_seen_by_me_seqno: 3,
                }))).toEqual(expected);
        });

        it('должен обновить стейт для нового чата', () => {
            const state = conversationsMock.createState({
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            }, {
                last_seen_timestamp: 2,
                last_seen_by_me_timestamp: 2,
                last_seen_seqno: 2,
                last_seen_by_me_seqno: 2,
                chatId: chatId2,
            });

            const expected = conversationsMock.createState({
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            }, {
                last_seen_timestamp: 2,
                last_seen_by_me_timestamp: 2,
                last_seen_seqno: 2,
                last_seen_by_me_seqno: 2,
                chatId: chatId2,
            }, {
                last_seen_timestamp: 3,
                last_seen_by_me_timestamp: 3,
                last_seen_seqno: 3,
                last_seen_by_me_seqno: 3,
                chatId: chatId3,
            });

            expect(conversationActions.conversationsReducer(state,
                updateSeenMarker({
                    chat_id: chatId3,
                    last_seen_timestamp: 3,
                    last_seen_by_me_timestamp: 3,
                    last_seen_seqno: 3,
                    last_seen_by_me_seqno: 3,
                }))).toEqual(expected);
        });

        it(`должен сохранять предыдущие значения для
last_seen_by_me_timestamp и last_seen_by_me_seqno, если они не переданы`, () => {
            const state = conversationsMock.createState({
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            });

            const expected = conversationsMock.createState({
                last_seen_timestamp: 2,
                last_seen_by_me_timestamp: 1,
                last_seen_seqno: 2,
                last_seen_by_me_seqno: 1,
                chatId: chatId1,
            });

            expect(conversationActions.conversationsReducer(state,
                updateSeenMarker({
                    chat_id: chatId1,
                    last_seen_timestamp: 2,
                    last_seen_seqno: 2,
                }))).toEqual(expected);
        });
    });

    describe('updateConversations', () => {
        const chatId1 = '0';
        const chatId2 = '1';

        const nonEmptyInitialState = conversationsMock.createState(
            {
                chatId: chatId1,
                anchor: { timestamp: 1 },
                isServiceMetaSent: true,
            },
            {
                chatId: chatId2,
            },
        );

        const [message1, message2] = historyMock.createChatHistory()(
            {
                chat_id: chatId1,
                messages: [],
                anchor: { timestamp: 1 },
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_edit_timestamp: 0,
            },
            {
                chat_id: chatId2,
                messages: [],
                anchor: { timestamp: 2 },
                last_seen_seqno: 1,
                last_seen_by_me_seqno: 1,
                last_seen_timestamp: 1,
                last_seen_by_me_timestamp: 1,
                last_edit_timestamp: 0,
            },
        );

        it('Новый экземпляр conversation создается с правильным last_edit_timestamp', () => {
            expect(conversationActions.conversationsReducer(conversationsMock.createState({ chatId: chatId1 }),
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId1,
                        messages: [],
                        last_seen_seqno: 1,
                        last_seen_by_me_seqno: 1,
                        last_seen_timestamp: 1,
                        last_seen_by_me_timestamp: 1,
                        last_edit_timestamp: 2,
                    }),
                }))[chatId1])
                .toMatchObject({
                    commited_last_edit_timestamp: 2,
                    prev_last_edit_timestamp: 2,
                });
        });

        it('last_edit_timestamp должен обновиться если текущий меньше или равен указанному в данных', () => {
            const state = conversationsMock.createState(
                {
                    commited_last_edit_timestamp: 1,
                    prev_last_edit_timestamp: 1,
                    chatId: chatId1,
                },
                {
                    commited_last_edit_timestamp: 2,
                    prev_last_edit_timestamp: 1,
                    chatId: chatId2,
                },
            );

            expect(conversationActions.conversationsReducer(state,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: historyMock.createChatHistory()(
                        {
                            chat_id: chatId1,
                            messages: [],
                            last_seen_seqno: 1,
                            last_seen_by_me_seqno: 1,
                            last_seen_timestamp: 1,
                            last_seen_by_me_timestamp: 1,
                            last_edit_timestamp: 2,
                        },
                        {
                            chat_id: chatId2,
                            messages: [],
                            last_seen_seqno: 1,
                            last_seen_by_me_seqno: 1,
                            last_seen_timestamp: 1,
                            last_seen_by_me_timestamp: 1,
                            last_edit_timestamp: 2,
                        },
                    ),
                })))
                .toMatchObject({
                    [chatId1]: {
                        commited_last_edit_timestamp: 2,
                        prev_last_edit_timestamp: 1,
                    },
                    [chatId2]: {
                        commited_last_edit_timestamp: 2,
                        prev_last_edit_timestamp: 2,
                    },
                });
        });

        it('last_edit_timestamp не должен обновиться если текущий больше чем указанный в данных', () => {
            const state = conversationsMock.createState(
                {
                    chatId: chatId1,
                    commited_last_edit_timestamp: 3,
                    prev_last_edit_timestamp: 1,
                },
            );

            expect(conversationActions.conversationsReducer(state,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: historyMock.createChatHistory()(
                        {
                            chat_id: chatId1,
                            messages: [],
                            anchor: { timestamp: 1, align: 'bottom' },
                            last_seen_seqno: 1,
                            last_seen_by_me_seqno: 1,
                            last_seen_timestamp: 1,
                            last_seen_by_me_timestamp: 1,
                            last_edit_timestamp: 2,
                        },
                    ),
                }))[chatId1])
                .toMatchObject({
                    commited_last_edit_timestamp: 3,
                    prev_last_edit_timestamp: 1,
                });
        });

        it('Правильно обновляет стейт при первоначальной загрузке приложения', () => {
            const state = conversationActions.conversationsReducer(
                conversationsMock.createState(),
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: historyMock.createChatHistory()(
                        {
                            chat_id: chatId1,
                            messages: [],
                            last_seen_seqno: 1,
                            last_seen_by_me_seqno: 1,
                            last_seen_timestamp: 1,
                            last_seen_by_me_timestamp: 1,
                        },
                        {
                            chat_id: chatId2,
                            messages: [],
                            last_seen_seqno: 1,
                            last_seen_by_me_seqno: 1,
                            last_seen_timestamp: 1,
                            last_seen_by_me_timestamp: 1,
                        },
                    ),
                }),
            );

            expect(state).toEqual(conversationsMock.createState(
                {
                    chatId: chatId1,
                    last_seen_timestamp: 1,
                    last_seen_by_me_timestamp: 1,
                    last_seen_seqno: 1,
                    last_seen_by_me_seqno: 1,
                },
                {
                    chatId: chatId2,
                    last_seen_timestamp: 1,
                    last_seen_by_me_timestamp: 1,
                    last_seen_seqno: 1,
                    last_seen_by_me_seqno: 1,
                },
            ));
        });

        it('Правильно обновляет стейт при восстановлении соединения', () => {
            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: [message1, message2],
                }),
            );

            const expected = conversationsMock.createState(
                {
                    chatId: chatId1,
                    last_seen_timestamp: 1,
                    last_seen_by_me_timestamp: 1,
                    last_seen_seqno: 1,
                    last_seen_by_me_seqno: 1,
                    anchor: { timestamp: 1 },
                    isServiceMetaSent: true,
                    commited_last_edit_timestamp: 0,
                    prev_last_edit_timestamp: 0,
                    approved_by_me: undefined,
                    mentionsTs: undefined,
                    unread: 0,
                },
                {
                    chatId: chatId2,
                    last_seen_timestamp: 1,
                    last_seen_by_me_timestamp: 1,
                    last_seen_seqno: 1,
                    last_seen_by_me_seqno: 1,
                    anchor: { timestamp: 2 },
                    commited_last_edit_timestamp: 0,
                    prev_last_edit_timestamp: 0,
                    approved_by_me: undefined,
                    mentionsTs: undefined,
                    unread: 0,
                },
            );

            expect(actual).toEqual(expected);
        });

        it('Заменяет last_seen_by_me_timestamp на больший', () => {
            const expectedLastSeenByMeTimestamp = 5;
            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: [
                        mutate(message1, { last_seen_by_me_timestamp: expectedLastSeenByMeTimestamp }),
                    ],
                }),
            );

            expect(actual[chatId1].last_seen_by_me_timestamp).toBe(expectedLastSeenByMeTimestamp);
        });

        it('Заменяет last_seen_by_me_timestamp на новый, когда в стейте last_seen_by_me_timestamp отсутствует', () => {
            const expectedLastSeenByMeTimestamp = 5;
            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: [
                        mutate(message2, { last_seen_by_me_timestamp: expectedLastSeenByMeTimestamp }),
                    ],
                }),
            );

            expect(actual[chatId2].last_seen_by_me_timestamp).toBe(expectedLastSeenByMeTimestamp);
        });

        it('Заменяет last_seen_by_me_seqno на больший', () => {
            const expectedLastSeenByMeSeqno = 5;
            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: [
                        mutate(message1, { last_seen_by_me_seqno: expectedLastSeenByMeSeqno }),
                    ],
                }),
            );

            expect(actual[chatId1].last_seen_by_me_seqno).toBe(expectedLastSeenByMeSeqno);
        });

        it('Заменяет last_seen_by_me_seqno на новый, когда в стейте last_seen_by_me_seqno отсутствует', () => {
            const expectedLastSeenByMeSeqno = 5;
            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: generateGuid(),
                    chats: [],
                    users: [],
                    histories: [
                        mutate(message2, { last_seen_by_me_seqno: expectedLastSeenByMeSeqno }),
                    ],
                }),
            );

            expect(actual[chatId2].last_seen_by_me_seqno).toBe(expectedLastSeenByMeSeqno);
        });
    });

    describe('#addTsToMentions', () => {
        it('Should be added if array is empty or undfined', () => {
            expect(conversationActions.addTsToMentions([], 3))
                .toMatchObject([3]);

            expect(conversationActions.addTsToMentions(undefined, 3))
                .toMatchObject([3]);
        });

        it('Should be added befor 4', () => {
            expect(conversationActions.addTsToMentions([0, 1, 2, 4, 5], 3))
                .toMatchObject([0, 1, 2, 3, 4, 5]);
        });

        it('Should be added befor 1', () => {
            expect(conversationActions.addTsToMentions([1, 2, 3, 4, 5], 0))
                .toMatchObject([0, 1, 2, 3, 4, 5]);
        });

        it('Should be add after 5', () => {
            expect(conversationActions.addTsToMentions([0, 1, 2, 3, 4, 5], 7))
                .toMatchObject([0, 1, 2, 3, 4, 5, 7]);
        });

        it('Should be old array', () => {
            const arr = [1, 2, 3, 4, 5];

            expect(conversationActions.filterMentions(arr, 0) === arr).toBeTruthy();
        });
    });

    describe('#filterMentions', () => {
        it('Should be undefined', () => {
            expect(conversationActions.filterMentions(undefined, 2))
                .toBeUndefined();
        });

        it('Should be same empty array', () => {
            const arr = [];

            expect(conversationActions.filterMentions(arr, 2) === arr)
                .toBeTruthy();
        });

        it('Should filter elements less then last seen by me ts', () => {
            expect(conversationActions.filterMentions([0, 1, 2, 3, 4, 5], 2))
                .toMatchObject([3, 4, 5]);
        });

        it('Should filter first element', () => {
            expect(conversationActions.filterMentions([0, 1, 2, 3, 4, 5], 0))
                .toMatchObject([1, 2, 3, 4, 5]);
        });

        it('Should filter all elements', () => {
            expect(conversationActions.filterMentions([0, 1, 2, 3, 4, 5], 7))
                .toMatchObject([]);

            expect(conversationActions.filterMentions([0, 1, 2, 3, 4, 5], 5))
                .toMatchObject([]);
        });

        it('Should be old array', () => {
            const arr = [1, 2, 3, 4, 5];

            expect(conversationActions.filterMentions(arr, 0) === arr).toBeTruthy();
        });
    });

    describe('Обновление меншенов в updateHistory', () => {
        const chatId1 = '0';
        const [currentUser, user2] = usersMock.createUnlimited()(2);

        const nonEmptyInitialState = conversationsMock.createState(
            {
                chatId: chatId1,
                anchor: { timestamp: 1 },
                isServiceMetaSent: true,
            },
        );

        it('Список меншенов должен обновлятьcя из истории', () => {
            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [],
                    anchor: { timestamp: 1 },
                    last_seen_by_me_timestamp: 0,
                    mentionsTs: [1, 2, 3],
                },
            );

            let actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [
                        history1,
                    ],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([1, 2, 3]);

            actual = conversationActions.conversationsReducer(
                actual,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [
                        mutate(history1, {
                            mentionsTs: [3, 4],
                            last_seen_by_me_timestamp: 3,
                        }),
                    ],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([4]);
        });

        it('Пропущенный меншен должен быть добавлен', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })(3);

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message3.timestamp],
                },
            );

            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [
                        history1,
                    ],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp, message3.timestamp]);
        });

        it('Пропущенный меншен должен быть добавлен (текущий список меншенов пуст)', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
            })({}, { mentions: [currentUser, user2] }, {});

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [],
                },
            );

            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [
                        history1,
                    ],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message2.timestamp]);
        });

        it('Меншен из удаленного сообщения не должно быть в списке', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })({ deleted: true, mentions: undefined }, {}, {});

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message2.timestamp, message3.timestamp],
                },
            );

            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message2.timestamp, message3.timestamp]);
        });

        it('Меншен должен быть удален из списка если его удалили при редактировании сообщения', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })({}, {}, { editTimestamp: 123, mentions: [user2] });

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message2.timestamp, message3.timestamp],
                },
            );

            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp]);
        });
    });

    describe('Обновление меншенов в updateMessages', () => {
        const chatId1 = '0';
        const [currentUser, user2] = usersMock.createUnlimited()(2);

        const nonEmptyInitialState = conversationsMock.createState(
            {
                chatId: chatId1,
                anchor: { timestamp: 1 },
                isServiceMetaSent: true,
            },
        );

        it('Пропущенный меншен должен быть добавлен', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })(3);

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message2.timestamp],
                },
            );

            let actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            actual = conversationActions.conversationsReducer(
                actual,
                updateMessages(
                    [message3],
                    currentUser.guid,
                ),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp, message3.timestamp]);
        });

        it('Пропущенный меншен должен быть добавлен (текущий список меншенов пуст)', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })(3);

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [],
                    anchor: { timestamp: 1 },
                    mentionsTs: [],
                },
            );

            let actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            actual = conversationActions.conversationsReducer(
                actual,
                updateMessages(
                    [message1, message3, message2],
                    currentUser.guid,
                ),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp, message3.timestamp]);
        });

        it('Меншен из удаленного сообщения не должно быть в списке', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })(3);

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message2.timestamp, message3.timestamp],
                },
            );

            let actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            actual = conversationActions.conversationsReducer(
                actual,
                updateMessages(
                    [mutate(message3, {
                        editTimestamp: 123,
                        version: 2,
                        mentions: [user2],
                    })],
                    currentUser.guid,
                ),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp]);
        });

        it('Меншен должен быть удален из списка если его удалили при редактировании сообщения', () => {
            const [message1, message2, message3] = messagesMock.createTextMessage({
                chatId: chatId1,
                mentions: [currentUser, user2],
            })({}, {}, { editTimestamp: 123, mentions: [user2] });

            const [history1] = historyMock.createChatHistory()(
                {
                    chat_id: chatId1,
                    messages: [message1, message2, message3],
                    anchor: { timestamp: 1 },
                    mentionsTs: [message1.timestamp, message2.timestamp, message3.timestamp],
                },
            );

            const actual = conversationActions.conversationsReducer(
                nonEmptyInitialState,
                updateHistory({
                    authId: currentUser.guid,
                    chats: [],
                    users: [],
                    histories: [history1],
                }),
            );

            expect(actual[chatId1].mentionsTs)
                .toStrictEqual([message1.timestamp, message2.timestamp]);
        });
    });
});
