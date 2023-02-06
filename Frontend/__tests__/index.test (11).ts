/* eslint-disable import/first */
jest.mock('../../services/History', () => {});

const services = {
    other: { id: 0, ns: 0, chatId: '0/0/123', channelId: '1/0/123' },
    first: { id: 1, ns: 1, chatId: '0/1/123', channelId: '1/1/123' },
    second: { id: 2, ns: 2, chatId: '0/2/123', channelId: '1/2/123' },
};
const mockGetGlobalParam = jest.fn();

jest.mock('../../../shared/lib/globalParams', () => ({
    getGlobalParam: mockGetGlobalParam,
}));
jest.mock('../../../configs/separatedServices', () => (
    {
        default: {
            [services.first.id]: [services.first.ns],
            [services.second.id]: [services.second.ns],
        },
    }));

function mockServiceId(serviceId: number) {
    mockGetGlobalParam.mockImplementation((param) => {
        switch (param) {
            case 'serviceId':
                return serviceId;
            case 'backendConfig':
                return {
                    hidden_namespaces: serviceId !== -1 ? [
                        services.first.ns,
                        services.second.ns,
                    ] : [],
                };
            default:
                return undefined;
        }
    });
}

import {
    getChatUnreadCount,
    getLastMessage,
    isVerifiedUser,
    isVerifiedChat,
    getOrganizationNearWorkDay,
    getTotalUnreadCount,
    getPartnerGuid,
} from '../index';
import { AppState } from '../../store';
import { normalizeSchedule } from '../../lib/compat';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';
import { generateGuid } from '../../store/__tests__/mock/common';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { Role } from '../../constants/relation';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import conversationsReducer from '../../store/conversations';
import { updateHistory } from '../../store/sharedActions';
import { historyMockFactory } from '../../store/__tests__/mock/history';
import { chatsReducer } from '../../store/chats';
import { unreadCounterMiddlewareFactory } from '../../store/middlewares/unreadCounterMiddleware';

describe('Selectors', () => {
    const authId = generateGuid();
    const messagesMock = messagesMockFactory();
    const usersMock = usersMockFactory();
    const chatsMock = chatsMockFactory({ authId });
    const stateMock = stateMockFactory();
    const historyMock = historyMockFactory();

    afterEach(() => {
        mockServiceId(-1);
    });

    describe('#getPartnerGuid', () => {
        it('Should return partner guid if chat is private', () => {
            const partnerGuid = generateGuid();
            const chat = chatsMock.createPrivateChatWith(partnerGuid);
            const state = stateMock.createState({
                authId,
                chats: chatsMock.createState(chat),
                users: usersMock.createState(...usersMock.createFrom()(authId, partnerGuid)),
            });

            expect(getPartnerGuid(state, chat.chat_id)).toBe(partnerGuid);
        });

        it('Should return partner guid by chatId if chat not exists', () => {
            const partnerGuid = generateGuid();
            const chat = chatsMock.createPrivateChatWith(partnerGuid);
            const state = stateMock.createState({
                authId,
                users: usersMock.createState(...usersMock.createFrom()(authId, partnerGuid)),
            });

            expect(getPartnerGuid(state, chat.chat_id)).toBe(partnerGuid);
        });

        it('Should return partner guid for business chats', () => {
            const partnerGuid = generateGuid();
            const [businessChatId] = chatsMock.generateBussinessIds(1, 0);
            const state = stateMock.createState({
                authId,
                chats: chatsMock.createState(...chatsMock.createGroupChat()({
                    chat_id: businessChatId,
                    partner_guid: partnerGuid,
                    members: [authId, partnerGuid],
                })),
                users: usersMock.createState(...usersMock.createFrom()(authId, partnerGuid)),
            });

            expect(getPartnerGuid(state, businessChatId)).toBe(partnerGuid);
        });

        it('Should return undefined for business chats if excludeBusiness = false', () => {
            const partnerGuid = generateGuid();
            const [businessChatId] = chatsMock.generateBussinessIds(1, 0);
            const state = stateMock.createState({
                authId,
                chats: chatsMock.createState(...chatsMock.createGroupChat()({
                    chat_id: businessChatId,
                    partner_guid: partnerGuid,
                    members: [authId, partnerGuid],
                })),
                users: usersMock.createState(...usersMock.createFrom()(authId, partnerGuid)),
            });

            expect(getPartnerGuid(state, businessChatId, { excludeBusiness: true })).toBeFalsy();
        });

        it('Should return undefined for existed group chat or channel', () => {
            const [groupChatId] = chatsMock.generateGroupsIds(1, 0);
            const [channelId] = chatsMock.generateChannelsIds(1, 0);
            const [bussinessChatId] = chatsMock.generateBussinessIds(1, 0);
            const state = stateMock.createState({
                authId,
                chats: chatsMock.createState(...chatsMock.createGroupChat()(groupChatId, channelId)),
                users: usersMock.createState(...usersMock.createFrom()(authId)),
            });

            expect(getPartnerGuid(state, groupChatId)).toBeUndefined();
            expect(getPartnerGuid(state, channelId)).toBeUndefined();
            expect(getPartnerGuid(state, bussinessChatId)).toBeUndefined();
        });

        it('Should return undefined for not existed group chat or channel', () => {
            const [groupChatId] = chatsMock.generateGroupsIds(1, 0);
            const [channelId] = chatsMock.generateChannelsIds(1, 0);
            const state = stateMock.createState({
                authId,
                users: usersMock.createState(...usersMock.createFrom()(authId)),
            });

            expect(getPartnerGuid(state, groupChatId)).toBeUndefined();
            expect(getPartnerGuid(state, channelId)).toBeUndefined();
        });
    });

    describe('#getChatUnreadCount', () => {
        const chatId = chatsMock.generateGroupsIds(1, 0)[0];
        const unreadCounterMiddleware = unreadCounterMiddlewareFactory({ nsFilter: undefined });

        // функция ниже генерирует стейт ситуации, когда у нас есть одно непрочитанное сообщение в чате
        const getInitialState = (customChatId: string) => {
            const state = stateMock.createState({
                authId,
                messages: messagesMock.createState([]),
                buckets: {
                    maxVersion: 0,
                },
            });

            const chats = chatsMock.createGroupChat()({
                chat_id: customChatId,
                relations: chatsMock.createRelation([], 1, Role.MEMBER),
            });

            const action = updateHistory({
                authId,
                chats,
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: customChatId,
                    last_seen_by_me_seqno: 1,
                    last_seen_by_me_timestamp: messagesMock.currentTimestamp(),
                    last_timestamp: messagesMock.nextTimestamp(),
                    last_seqno: 2,
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            return nextState;
        };

        it('Should return 0 when last message timestamp equals last seen by me timestamp', () => {
            const state = getInitialState(chatId);

            const action = updateHistory({
                authId,
                chats: [],
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: chatId,
                    last_seen_by_me_seqno: 2,
                    last_seen_by_me_timestamp: messagesMock.currentTimestamp(),
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getChatUnreadCount(nextState, chatId)).toEqual(0);
        });

        it('Should return 0 for unjoined chat', () => {
            const state = getInitialState(chatId);

            const action = updateHistory({
                authId,
                chats: chatsMock.createGroupChat()({
                    chat_id: chatId,
                    relations: chatsMock.createRelation([], 2, Role.GONE),
                }),
                users: [],
                histories: [],
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getChatUnreadCount(nextState, chatId)).toEqual(0);
        });

        it('Should return 0 when last message has no seqno', () => {
            const state = getInitialState(chatId);
            const [chatId2] = chatsMock.generateGroupsIds(1, 0);

            const action = updateHistory({
                authId,
                chats: chatsMock.createGroupChat()({
                    chat_id: chatId2,
                    relations: chatsMock.createRelation([], 1, Role.MEMBER),
                }),
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: chatId2,
                    last_seen_by_me_seqno: 0,
                    last_seen_by_me_timestamp: 0,
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            expect(getChatUnreadCount(nextState, chatId2)).toEqual(0);
        });

        it('Should return correct value of unread', () => {
            expect(getChatUnreadCount(getInitialState(chatId), chatId)).toEqual(1);
        });

        it('Should return 0 for hidden service chat on serviceId no HEALTH', () => {
            mockServiceId(services.other.id);

            expect(getChatUnreadCount(getInitialState(services.first.chatId), services.first.chatId))
                .toEqual(0);

            mockServiceId(services.first.id);

            expect(getChatUnreadCount(getInitialState(services.first.chatId), services.first.chatId))
                .not.toEqual(0);
        });
    });

    describe('#getTotalUnreadCount', () => {
        const chatId = chatsMock.generateGroupsIds(1, 0)[0];

        // функция ниже генерирует стейт ситуации, когда у нас есть одно непрочитанное сообщение в n чатах
        const getInitialState = (
            customChats: Partial<APIv3.Chat>[],
            unreadCounterMiddleware = unreadCounterMiddlewareFactory({ nsFilter: undefined }),
        ) => {
            const state = stateMock.createState({
                authId,
                messages: messagesMock.createState([]),
                buckets: {
                    maxVersion: 0,
                },
            });

            const chats = chatsMock.createGroupChat()(...customChats.map((customChat) => ({
                relations: chatsMock.createRelation([], 1, Role.MEMBER),
                ...customChat,
            })));

            const action = updateHistory({
                authId,
                chats,
                users: [],
                histories: historyMock.createChatHistory()(...customChats.map((customChat) => ({
                    last_seen_by_me_seqno: 1,
                    last_seen_by_me_timestamp: messagesMock.currentTimestamp(),
                    last_timestamp: messagesMock.nextTimestamp(),
                    last_seqno: 2,
                    chat_id: customChat.chat_id,
                }))),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            return {
                state: nextState,
                unreadCounterMiddleware,
            };
        };

        it('Should return 0 when last message timestamp equals last seen by me timestamp', () => {
            const {
                state,
                unreadCounterMiddleware,
            } = getInitialState([{ chat_id: chatId }]);

            const action = updateHistory({
                authId,
                chats: [],
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: chatId,
                    last_seen_by_me_seqno: 2,
                    last_seen_by_me_timestamp: messagesMock.currentTimestamp(),
                    last_timestamp: messagesMock.currentTimestamp(),
                    last_seqno: 2,
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getTotalUnreadCount(nextState)).toMatchObject({
                unmuted: 0,
                total: 0,
                chatsCount: 0,
            });
        });

        it('Should return 0 for unjoined chat', () => {
            const {
                state,
                unreadCounterMiddleware,
            } = getInitialState([{ chat_id: chatId }]);

            const action = updateHistory({
                authId,
                chats: chatsMock.createGroupChat()({
                    chat_id: chatId,
                    relations: chatsMock.createRelation([], 2, Role.GONE),
                }),
                users: [],
                histories: [],
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getTotalUnreadCount(nextState)).toMatchObject({
                unmuted: 0,
                total: 0,
                chatsCount: 0,
            });
        });

        it('Should return correct value of unread', () => {
            expect(getTotalUnreadCount(
                getInitialState([{ chat_id: chatId }]).state,
            )).toMatchObject({
                unmuted: 1,
                total: 1,
                chatsCount: 1,
            });
        });

        it('Should return correct value and chatCount of unread in one chat', () => {
            const {
                state,
                unreadCounterMiddleware,
            } = getInitialState([{ chat_id: chatId }]);

            const action = updateHistory({
                authId,
                chats: [],
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: chatId,
                    last_timestamp: messagesMock.nextTimestamp(),
                    last_seqno: 3,
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getTotalUnreadCount(nextState)).toMatchObject({
                unmuted: 2,
                total: 2,
                chatsCount: 1,
            });
        });

        it('Should return correct value and chatCount of unread in three chat', () => {
            const chatIds = chatsMock.generateGroupsIds(3, 0);
            const {
                state,
                unreadCounterMiddleware,
            } = getInitialState([
                { chat_id: chatIds[0] },
                { chat_id: chatIds[1] },
            ]);

            const action = updateHistory({
                authId,
                chats: [],
                users: [],
                histories: historyMock.createChatHistory()({
                    chat_id: chatIds[0],
                    last_seen_by_me_seqno: 0,
                    last_seqno: 3,
                }, {

                    chat_id: chatIds[1],
                    last_seen_by_me_seqno: 0,
                    last_seqno: 1,
                }, {
                    chat_id: chatIds[2],
                    last_seen_by_me_seqno: 0,
                    last_seqno: 0,
                }),
            });

            const nextState = {
                ...state,
                chats: chatsReducer(state.chats, action),
                conversations: conversationsReducer(state.conversations, action),
            };

            unreadCounterMiddleware(action, nextState, state);

            expect(getTotalUnreadCount(nextState)).toMatchObject({
                unmuted: 3,
                total: 3,
                chatsCount: 2,
            });
        });

        it('Should return 0 for hidden service chat on serviceId no HEALTH', () => {
            mockServiceId(services.other.id);

            const state1 = getInitialState([{ chat_id: services.first.chatId }]).state;

            expect(getTotalUnreadCount(state1)).toMatchObject({
                unmuted: 0,
                total: 0,
                chatsCount: 0,
            });

            mockServiceId(services.first.id);

            const state2 = getInitialState([{ chat_id: services.first.chatId }]).state;

            expect(getTotalUnreadCount(state2)).toMatchObject({
                unmuted: 1,
                total: 1,
                chatsCount: 1,
            });
        });

        describe('unreadNSFilter', () => {
            it('Should return 0 if chats don\'t have namespace', () => {
                const chatIds = chatsMock.generateGroupsIds(3, 0);

                const {
                    state,
                } = getInitialState(
                    [
                        { chat_id: chatIds[0] },
                        { chat_id: chatIds[1] },
                        { chat_id: chatIds[2] },
                    ],
                    unreadCounterMiddlewareFactory({ nsFilter: [1] }),
                );

                expect(getTotalUnreadCount(state, true)).toMatchObject({
                    unmuted: 0,
                    total: 0,
                    chatsCount: 0,
                });
            });

            it('Should return unread count for chats that included in unreadNSFilter', () => {
                const chatIds = chatsMock.generateGroupsIds(1, 0);
                const chatIdsWithNamespace = chatsMock.generateGroupsIds(2, 1);

                const { state } = getInitialState(
                    [
                        { chat_id: chatIds[0] },
                        { chat_id: chatIdsWithNamespace[0] },
                        { chat_id: chatIdsWithNamespace[1] },
                    ],
                    unreadCounterMiddlewareFactory({ nsFilter: [1] }),
                );

                expect(getTotalUnreadCount(state, true)).toEqual({
                    unmuted: 2,
                    total: 2,
                    chatsCount: 2,
                });
            });
        });
    });

    describe('#getLastMessage', () => {
        const [chatId] = chatsMock.generateGroupsIds(1, 0);

        it('Should be undefined if no messages', () => {
            const state = stateMock.createState({
                messages: messagesMock.createState([]),
            });

            expect(getLastMessage(state, chatId)).toEqual(undefined);
        });

        it('Should return last message', () => {
            const messages = messagesMock.createTextMessage({ chatId })(3);

            let state = stateMock.createState({
                messages: messagesMock.createState(messages),
            });

            expect(getLastMessage(state, chatId)).toEqual(messages[2]);

            const [newMessage] = messagesMock.createTextMessage({ chatId })(1);

            state = stateMock.createState({
                messages: messagesMock.createState([newMessage], undefined, state.messages),
            });

            expect(getLastMessage(state, chatId)).toEqual(newMessage);
        });

        it('Should return message with timestamp <= then timeSlice', () => {
            const timeSlice = messagesMock.currentTimestamp();

            const [message1, message2, message3] = messagesMock.createTextMessage(
                { chatId, version: 1 },
                (message, index) => ({
                    ...message,
                    prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                    timestamp: messagesMock.nextTimestamp(),
                    seqno: index + 1,
                }),
            )(3);

            let state = stateMock.createState({
                messages: messagesMock.createState([]),
            });

            expect(getLastMessage(state, chatId, timeSlice)).toEqual(undefined);

            state = stateMock.createState({
                messages: messagesMock.createState([message1, message2]),
            });

            expect(getLastMessage(state, chatId, timeSlice)).toEqual(undefined);

            state = stateMock.createState({
                messages: messagesMock.createState([message3], undefined, state.messages),
            });

            expect(getLastMessage(state, chatId, message1.timestamp)?.timestamp).toEqual(message1.timestamp);
            expect(getLastMessage(state, chatId, message2.timestamp + 1)?.timestamp).toEqual(message2.timestamp);
            expect(getLastMessage(state, chatId, message3.timestamp + 1)?.timestamp).toEqual(message3.timestamp);
        });
    });

    describe('#isVerifiedUser', () => {
        it('Should return true if is_verified', () => {
            expect(isVerifiedUser(usersMock.createFrom()({
                metadata: {
                    is_verified: true,
                },
            })[0])).toBeTruthy();
        });

        it('Should return false if is_verified does not exists', () => {
            expect(isVerifiedUser(usersMock.createFrom()({
                metadata: {
                    is_verified: false,
                },
            })[0])).toBeFalsy();

            expect(isVerifiedUser(usersMock.createFrom()({
                metadata: {},
            })[0])).toBeFalsy();
        });

        it('Should return false if metadata does not exists', () => {
            expect(isVerifiedUser(usersMock.createFrom()()[0])).toBeFalsy();
        });

        it('Should return false if userInfo does not exists', () => {
            expect(isVerifiedUser(undefined)).toEqual(false);
        });
    });

    describe('#isVerifiedChat', () => {
        const interviewerId = '98765432-4d2b-4f2b-a3b2-b3e29de83c62';
        const chatId = `${authId}_${interviewerId}`;

        function getState(status: string, metadata?: APIv3.UserMetadata): AppState {
            const state: Partial<AppState> = {
                authId,
                chats: chatsMock.createState(chatsMock.createPrivateChatWith(interviewerId)),
                users: usersMock.createState(...usersMock.createFrom()(authId, interviewerId)),
                userInfo: {
                    [interviewerId]: {
                        status,
                        user: usersMock.createFrom()({
                            guid: interviewerId,
                            metadata,
                        })[0],
                    } as Client.UserInfo,
                },
            };

            return state as AppState;
        }

        it('Should return false if is not private chat', () => {
            const notPrivateChatId = '0/5/abcd';

            expect(isVerifiedChat(getState('ok'), notPrivateChatId)).toEqual(false);
        });

        it('Should return false if has not interviewer userInfo', () => {
            const anotherInterviewerId = '00000000-4d2b-4f2b-a3b2-b3e29de83c62';
            const anotherChatId = `${authId}_${anotherInterviewerId}`;

            expect(isVerifiedChat(getState('ok'), anotherChatId)).toEqual(false);
        });

        it('Should return true if is_verified', () => {
            expect(isVerifiedChat(
                getState('ok', { is_verified: true }),
                chatId,
            )).toEqual(true);
        });

        it('Should return false if is_verified does not exists', () => {
            expect(isVerifiedChat(
                getState('ok', { is_verified: false }),
                chatId,
            )).toEqual(false);

            expect(isVerifiedChat(getState('ok', {}), chatId)).toEqual(false);
        });

        it('Should return false if metadata does not exists', () => {
            expect(isVerifiedChat(getState('ok', undefined), chatId)).toEqual(false);
        });

        it('Should return false if status is not ok', () => {
            expect(isVerifiedChat(getState('idle', undefined), chatId)).toEqual(false);
            expect(isVerifiedChat(getState('progress', undefined), chatId)).toEqual(false);
            expect(isVerifiedChat(getState('error', undefined), chatId)).toEqual(false);
        });
    });

    describe('#getOrganizationNearWorkDay', () => {
        const initialSchedule: APIv3.ScheduleItem[] = [
            { from_hour: '08:00', to_hour: '19:00', weekday: 1 }, // пн
            { from_hour: '08:00', to_hour: '19:00', weekday: 4 }, // чт
            { from_hour: '12:00', to_hour: '16:00', weekday: 5 }, // пт
            { from_hour: '21:00', to_hour: '02:00', weekday: 6 }, // сб
            { from_hour: '21:00', to_hour: '02:00', weekday: 7 }, // вс
        ];

        function getNormalizedSchedule(timezoneOffset) {
            return normalizeSchedule(initialSchedule, timezoneOffset);
        }

        const weekdayDict = ['вс', 'пн', 'вт', 'ср', 'чт', 'пт', 'сб'];

        function getWeekdayTime(weekday, time, timezone) {
            const weekdayNumber = weekdayDict.indexOf(weekday);

            return new Date(`2017.1.${weekdayNumber + 1} ${time} UTC${timezone < 0 ? '' : '+'}${timezone}`);
        }

        it('Не работает, но будет работать сегодня', () => {
            const timezoneOffset = 0;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '07:00', 0);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: false,
                now,
                workStarts: getWeekdayTime('пн', '08:00', 0),
            });
        });

        it('Работает сейчас первую минуту по расписанию сегодняшнего дня', () => {
            const timezoneOffset = 3 * 60; // в минутах
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '08:00', 3);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: true,
                now,
                workStarts: getWeekdayTime('пн', '08:00', 3),
            });
        });

        it('Работает сейчас по расписанию сегодняшнего дня', () => {
            const timezoneOffset = 4 * 60;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '12:00', 4);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: true,
                now,
                workStarts: getWeekdayTime('пн', '08:00', 4),
            });
        });

        it('Работает сейчас последнюю минуту', () => {
            const timezoneOffset = -2 * 60;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '18:59', -2);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: true,
                now,
                workStarts: getWeekdayTime('пн', '08:00', -2),
            });
        });

        it('Уже не работает сегодня первую минуту', () => {
            const timezoneOffset = -12 * 60;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '19:00', -12);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: false,
                now,
                workStarts: getWeekdayTime('чт', '08:00', -12),
            });
        });

        it('Не работает сегодня вообще', () => {
            const timezoneOffset = 2 * 60;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('вт', '08:00', 2);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: false,
                now,
                workStarts: getWeekdayTime('чт', '08:00', 2),
            });
        });

        it('Работает сейчас по расписанию вчерашнего дня с переходом на новый день', () => {
            const timezoneOffset = 0;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('пн', '01:00', 0);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: true,
                now,
                workStarts: getWeekdayTime('вс', '21:00', 0),
            });
        });

        it('Работает сейчас по расписанию вчерашнего дня с переходом на новый день с субботы на воскресенье', () => {
            const timezoneOffset = 0;
            const schedule = getNormalizedSchedule(timezoneOffset);
            const now = getWeekdayTime('вс', '01:00', 0);

            const workStarts = getWeekdayTime('сб', '21:00', 0);
            workStarts.setDate(workStarts.getDate() - 7);

            expect(
                getOrganizationNearWorkDay(schedule, now),
            ).toEqual({
                working: true,
                now,
                workStarts,
            });
        });

        it('Возвращает working=false, если расписание пустое', () => {
            const now = getWeekdayTime('вс', '01:00', 0);

            expect(
                getOrganizationNearWorkDay({}, now),
            ).toEqual({
                working: false,
                now,
                workStarts: undefined,
            });
        });
    });
});
