import {
    getMessageUrl,
    isMessageHidden,
    isComposePreviewDisabled,
    getComposePreviewData,
    getComposePreviewUrl,
    isHistoryPartConsistent,
    shouldHaveNotification,
    getCountDeletedMessage,
    canDeleteMessage,
    canEditMessage,
    getPollMessageInfo,
} from '../message';
import { AppState } from '../../store';
import { ModerationActions } from '../../constants/fanout';

import i18n from '../../../shared/lib/i18n';
import * as ru from '../../../langs/yamb/ru.json';
import * as en from '../../../langs/yamb/en.json';
import { Times } from '../../lib/Date/DateTimes';
import { getRelativeTime } from '../../lib/Date';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';
import { localSettingsMockFactory } from '../../store/__tests__/mock/localSettings';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { generateGuid } from '../../store/__tests__/mock/common';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { createTextData } from '../../helpers/messages';
import { PollMessageType } from '../../constants/message';

declare var global: NodeJS.Global & { FLAGS: Record<string, boolean> };

global.FLAGS = {
    REACTIONS: true,
};

function getState(partialState: Partial<AppState> = {}): AppState {
    return partialState as AppState;
}

describe('MessageSelectors', () => {
    const messagesMock = messagesMockFactory();
    const localSettingsMock = localSettingsMockFactory();
    const usersMock = usersMockFactory();

    describe('#getMessageUrl', () => {
        it('Should return url and isOnlyUrl = true', () => {
            const [message1, message2] = messagesMock.createTextMessage({ chatId: generateGuid() })(
                {
                    data: createTextData('yandex.ru/chat'),
                },
                {
                    data: createTextData('https://yandex.ru/chat'),
                },
            );

            const state = getState({
                messages: messagesMock.createState([message1, message2]),
            });

            const urlData1 = getMessageUrl(state, message1);

            expect(urlData1.url).toEqual('http://yandex.ru/chat');
            expect(urlData1.isOnlyUrl).toBeTruthy();

            const urlData2 = getMessageUrl(state, message2);

            expect(urlData2.url).toEqual('https://yandex.ru/chat');
            expect(urlData2.isOnlyUrl).toBeTruthy();
        });

        it('Should return url and isOnlyUrl = true from md link', () => {
            const [message1, message2] = messagesMock.createTextMessage({ chatId: generateGuid() })(
                {
                    data: createTextData('[yandex](yandex.ru/chat)'),
                },
                {
                    data: createTextData('[yandex full](https://yandex.ru/chat)'),
                },
            );

            const state = getState({
                messages: messagesMock.createState([message1, message2]),
            });

            const urlData1 = getMessageUrl(state, message1);

            expect(urlData1.url).toEqual('http://yandex.ru/chat');
            expect(urlData1.isOnlyUrl).toBeFalsy();

            const urlData2 = getMessageUrl(state, message2);

            expect(urlData2.url).toEqual('https://yandex.ru/chat');
            expect(urlData2.isOnlyUrl).toBeFalsy();
        });

        it('Should return url from text message', () => {
            const [message1] = messagesMock.createTextMessage({ chatId: generateGuid() })(
                {
                    data: createTextData('Some text \n WoW https://yandex.ru/chat zZz'),
                },
            );

            const state = getState({
                messages: messagesMock.createState([message1]),
            });

            const { url, isOnlyUrl } = getMessageUrl(state, message1);

            expect(url).toEqual('https://yandex.ru/chat');
            expect(isOnlyUrl).toBeFalsy();
        });

        it('Should return empty string when message not found', () => {
            const state = getState({
                messages: messagesMock.createState([]),
            });

            const { url, isOnlyUrl } = getMessageUrl(state, {
                chatId: generateGuid(),
                timestamp: messagesMock.currentTimestamp(),
            });

            expect(url).toEqual('');
            expect(isOnlyUrl).toBeFalsy();
        });

        describe('INTERNAL=true', () => {
            beforeAll(() => {
                // @ts-ignore
                window.flags.internal = true;
            });

            afterAll(() => {
                // @ts-ignore
                delete window.flags.internal;
            });

            it('Should return empty string when url is yandex-team', () => {
                const [message1] = messagesMock.createTextMessage({ chatId: generateGuid() })(
                    {
                        data: createTextData('https://staff.yandex-team.ru'),
                    },
                );

                const state = getState({
                    messages: messagesMock.createState([message1]),
                });

                const { url, isOnlyUrl } = getMessageUrl(state, message1);

                expect(url).toEqual('');
                expect(isOnlyUrl).toBeFalsy();
            });

            it('Should return another url when url is yandex-team', () => {
                const [message1] = messagesMock.createTextMessage({ chatId: generateGuid() })(
                    {
                        data: createTextData('https://staff.yandex-team.ru https://yandex.ru'),
                    },
                );

                const state = getState({
                    messages: messagesMock.createState([message1]),
                });

                const { url, isOnlyUrl } = getMessageUrl(state, message1);

                expect(url).toEqual('https://yandex.ru');
                expect(isOnlyUrl).toBeFalsy();
            });
        });
    });

    describe('#getRelativeTime', () => {
        const todayTimestamp = 1556197558307; // 25 апреля 2019г.

        const today = new Date(todayTimestamp);
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);

        it('returns "Сегодня" for today (ru)', () => {
            i18n.locale('ru', ru);

            const actual = getRelativeTime(new Date(todayTimestamp), today);

            expect(actual).toBe(i18n('common.today'));
        });

        it('returns "Today" for today (en)', () => {
            i18n.locale('en', en);

            const actual = getRelativeTime(new Date(todayTimestamp), today);

            expect(actual).toBe(i18n('common.today'));
        });

        it('returns "Вчера" for today (ru)', () => {
            i18n.locale('ru', ru);
            const yesterdayTimestamp = todayTimestamp - Times.DAY;

            const actual = getRelativeTime(new Date(yesterdayTimestamp), today);

            expect(actual).toBe(i18n('common.yesterday'));
        });

        it('returns "Yesterday" for today (en)', () => {
            i18n.locale('en', en);
            const yesterdayTimestamp = todayTimestamp - Times.DAY;

            const actual = getRelativeTime(new Date(yesterdayTimestamp), today);

            expect(actual).toBe(i18n('common.yesterday'));
        });

        it('returns correct date for dates before yesterday and less than 1yr ago (ru)', () => {
            i18n.locale('ru', ru);
            const beforeYesterdayTimestamp = todayTimestamp - (Times.DAY * 2);

            const actual = getRelativeTime(new Date(beforeYesterdayTimestamp), today);

            expect(actual).toBe('23 апреля');
        });

        it('returns correct date for dates before yesterday and less than 1yr ago (en)', () => {
            i18n.locale('en', en);
            const beforeYesterdayTimestamp = todayTimestamp - (Times.DAY * 2);

            const actual = getRelativeTime(new Date(beforeYesterdayTimestamp), today);

            expect(actual).toBe('23 April');
        });

        it('returns correct date for dates more than 1yr ago (ru)', () => {
            i18n.locale('ru', ru);
            const twoYearsAgo = todayTimestamp - (Times.YEAR * 2);

            const actual = getRelativeTime(new Date(twoYearsAgo), today);

            expect(actual).toBe('25 апреля 2017 г.');
        });

        it('returns correct date for dates more than 1yr ago (en)', () => {
            i18n.locale('en', en);
            const twoYearsAgo = todayTimestamp - (Times.YEAR * 2);

            const actual = getRelativeTime(new Date(twoYearsAgo), today);

            expect(actual).toBe('25 April 2017');
        });
    });

    describe('#isMessageHidden', () => {
        it('returns false when message not found', () => {
            const state = getState({
                messages: messagesMock.createState([]),
            });

            expect(isMessageHidden(state, messagesMock.createTextMessage()()[0])).toBeFalsy();
        });

        it('returns false when message.moderation_action is not HIDE', () => {
            const [message] = messagesMock.createTextMessage()();

            const state = getState({
                messages: messagesMock.createState([message]),
            });

            expect(isMessageHidden(state, message)).toBeFalsy();
        });

        it('returns false when messages.revealedMessagesIds contains message', () => {
            const [message] = messagesMock.createTextMessage()({
                revealed: true,
                moderationAction: ModerationActions.HIDE,
            });

            const state = getState({
                messages: messagesMock.createState([message]),
            });

            expect(isMessageHidden(state, message)).toBeFalsy();
        });

        it('returns true', () => {
            const [message] = messagesMock.createTextMessage()({
                moderationAction: ModerationActions.HIDE,
            });

            const state = getState({
                messages: messagesMock.createState([message]),
            });

            expect(isMessageHidden(state, message)).toBeTruthy();
        });
    });

    describe('#isComposePreviewDisabled', () => {
        it('returns true', () => {
            const chatId = 'X';
            const state = getState({ compose: { [chatId]: { preview: '', urlPreviewDisabled: true } } });

            expect(isComposePreviewDisabled(state, chatId)).toBe(true);
        });

        it('returns false', () => {
            const chatId = 'X';
            const state = getState({ compose: { [chatId]: { preview: '', urlPreviewDisabled: false } } });
            const stateEmpty = getState({ compose: {} });

            expect(isComposePreviewDisabled(state, chatId)).toBe(false);
            expect(isComposePreviewDisabled(stateEmpty, chatId)).toBe(false);
        });
    });

    describe('#getComposePreviewData', () => {
        it('returns undefined', () => {
            const chatId = 'X';
            const state = getState({ compose: { [chatId]: { preview: '', urlPreviewDisabled: true } } });
            const stateEmpty = getState({ compose: {} });

            expect(getComposePreviewData(state, chatId)).toBe(undefined);
            expect(getComposePreviewData(stateEmpty, chatId)).toBe(undefined);
        });

        it('returns data', () => {
            const chatId = 'X';
            const previewData = { url: 'http://yandex.ru', preview: { title: 'Yandex', description: 'Yandex' } };
            const state = getState({
                preview: {
                    'http://yandex.ru': {
                        data: previewData,
                        validUntil: 1000,
                    },
                },
                compose: { [chatId]: { preview: previewData.url } },
            });

            expect(getComposePreviewData(state, chatId)).toBe(previewData);
        });
    });

    describe('#getComposePreviewUrl', () => {
        it('returns undefined', () => {
            const chatId = 'X';
            const state = getState({ compose: { [chatId]: { preview: undefined } } });
            const stateEmpty = getState({ compose: {} });

            expect(getComposePreviewUrl(state, chatId)).toBe(undefined);
            expect(getComposePreviewUrl(stateEmpty, chatId)).toBe(undefined);
        });

        it('returns data', () => {
            const chatId = 'X';
            const previewUrl = 'http://yandex.ru';
            const state = getState({ compose: { [chatId]: { preview: previewUrl } } });

            expect(getComposePreviewUrl(state, chatId)).toBe(previewUrl);
        });
    });

    describe('#isHistoryPartConsistent', () => {
        it('Should return false if there\'s no messages in chat', () => {
            const chatId = 'X';
            const state = getState({
                messages: messagesMock.createState([]),
            });

            expect(isHistoryPartConsistent(
                state,
                chatId,
                0,
                10,
            )).toBeFalsy();
        });

        it('Should be failed if minTimestamp > maxTimestamp', () => {
            const chatId = 'X';
            const messages = messagesMock.createTextMessage({ chatId: chatId })(
                {
                    timestamp: 0,
                    prevTimestamp: 1,
                },
            );

            const state = getState({
                messages: messagesMock.createState(messages),
            });

            expect(isHistoryPartConsistent(
                state,
                chatId,
                10,
                1,
            )).toBeFalsy();
        });

        it('Should be ok if only one message', () => {
            const chatId = 'X';
            const messages = messagesMock.createTextMessage({ chatId: chatId })(
                {
                    timestamp: 0,
                    prevTimestamp: 1,
                },
            );

            const state = getState({
                messages: messagesMock.createState(messages),
            });

            expect(isHistoryPartConsistent(
                state,
                chatId,
                0,
                0,
            )).toBeTruthy();
        });

        it('Should return false if there\'s a break between messages', () => {
            const chatId = 'X';
            const messages = messagesMock.createTextMessage({ chatId: chatId })(
                {
                    timestamp: 1,
                    version: 1,
                },
                {
                    timestamp: 2,
                    prevTimestamp: 1,
                    version: 1,
                },
                {
                    timestamp: 4,
                    prevTimestamp: 3,
                    version: 1,
                },
            );

            const state = getState({
                messages: messagesMock.createState(messages),
            });

            expect(isHistoryPartConsistent(
                state,
                chatId,
                0,
                4,
            )).toBeFalsy();
        });

        it('Should return true if there\'s no breaks between messages', () => {
            const chatId = 'X';
            const messages = messagesMock.createTextMessage({ chatId: chatId })(
                {
                    timestamp: 1,
                    version: 1,
                },
                {
                    timestamp: 2,
                    prevTimestamp: 1,
                    version: 1,
                },
                {
                    timestamp: 3,
                    prevTimestamp: 2,
                    version: 1,
                },
            );

            const state = getState({
                messages: messagesMock.createState(messages),
            });

            expect(isHistoryPartConsistent(
                state,
                chatId,
                1,
                3,
            )).toBeTruthy();
        });
    });

    describe('#shouldHaveNotification', () => {
        it('Should return false if notifications are disabled', () => {
            const [message] = messagesMock.createTextMessage()();

            const state = getState({
                localSettings: localSettingsMock.createState({
                    enableWebPush: false,
                    enableNotifications: false,
                }),
            });

            expect(shouldHaveNotification(state, message)).toBeFalsy();
        });

        it('Should return true if chat is muted and message has mention', () => {
            const [user] = usersMock.createFrom()();

            const [message] = messagesMock.createTextMessage()({
                mentions: [user],
            });

            const state = getState({
                authId: user.guid,
                localSettings: localSettingsMock.createState({
                    enableWebPush: false,
                    enableNotifications: true,
                }),
                messages: messagesMock.createState([message]),
                buckets: {
                    maxVersion: 0,
                    chat_mutings: {
                        data: {
                            [message.chatId]: {
                                mute: true,
                                mute_mentions: false,
                            },
                        },
                        version: 1,
                    },
                },
            });

            expect(shouldHaveNotification(state, message)).toBeTruthy();
        });

        it('Should return false if notifications are enabled, but chat is muted and message does not have mention', () => {
            const [user] = usersMock.createFrom()();
            const [message] = messagesMock.createTextMessage()();

            const state = getState({
                authId: user.guid,
                localSettings: localSettingsMock.createState({
                    enableWebPush: false,
                    enableNotifications: true,
                }),
                messages: messagesMock.createState([message]),
                buckets: {
                    maxVersion: 0,
                    chat_mutings: {
                        data: {
                            [message.chatId]: {
                                mute: true,
                                mute_mentions: false,
                            },
                        },
                        version: 1,
                    },
                },
            });

            expect(shouldHaveNotification(state, message)).toBeFalsy();
        });

        it('Should return false if notifications are enabled, but chat is muted and message is important', () => {
            const [user] = usersMock.createFrom()();
            const [message] = messagesMock.createTextMessage()();

            message.important = true;

            const state = getState({
                authId: user.guid,
                localSettings: localSettingsMock.createState({
                    enableWebPush: false,
                    enableNotifications: true,
                }),
                messages: messagesMock.createState([message]),
                buckets: {
                    maxVersion: 0,
                    chat_mutings: {
                        data: {
                            [message.chatId]: {
                                mute: true,
                                mute_mentions: false,
                            },
                        },
                        version: 1,
                    },
                },
            });

            expect(shouldHaveNotification(state, message)).toBeTruthy();
        });

        it('Should return false if web pushes and notifications are enabled', () => {
            const [message] = messagesMock.createTextMessage()();

            const state = getState({
                localSettings: localSettingsMock.createState({
                    enableWebPush: true,
                    enableNotifications: true,
                }),
            });

            expect(shouldHaveNotification(state, message)).toBeFalsy();
        });
    });

    describe('#getCountDeletedMessage', () => {
        it('Should return count deleted message', () => {
            const chatId = generateGuid();
            const messages = messagesMock.createTextMessage({
                chatId,
            }, (message) => ({
                ...message,
                prevTimestamp: messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
            }))(9);

            const state = getState({
                messages: messagesMock.createState(messages),
            });

            expect(getCountDeletedMessage(state, messages[5])).toBe(0);

            const message6 = state.messages.chats[chatId].cache.get(messages[6].timestamp);

            if (!message6) {
                throw new Error('Message not found');
            }

            message6.deleted = true;

            expect(getCountDeletedMessage(state, message6)).toBe(1);

            const message7 = state.messages.chats[chatId].cache.get(messages[7].timestamp);

            if (!message7) {
                throw new Error('Message not found');
            }

            message7.deleted = true;

            expect(getCountDeletedMessage(state, message6)).toBe(2);

            const message8 = state.messages.chats[chatId].cache.get(messages[8].timestamp);

            if (!message8) {
                throw new Error('Message not found');
            }

            message8.deleted = true;

            expect(getCountDeletedMessage(state, message8)).toBe(3);
            expect(getCountDeletedMessage(state, message7)).toBe(3);
        });
    });

    function createStateWithChat(chat: APIv3.Chat, user: APIv3.User): [AppState, APIv3.Message[]] {
        const messages = messagesMock.createTextMessage({
            chatId: chat.chat_id,
            mentions: [user],
        })(
            {
                from: {
                    guid: user.guid,
                    display_name: 'test',
                    version: 1,
                },
            },
            {},
        );

        const state = getState({
            authId: user.guid,
            messages: messagesMock.createState(messages),
            chats: {
                [chat.chat_id]: chat,
            }
        });

        return [state, messages];
    }

    describe('#canDeleteMessage', () => {
        it('Private chat', () => {
            const chatId = '64b15156-0f58-863c-d311-eacc5d24ab50_8118f21e-10fe-ce62-4e5a-b6571777969c';

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory({
                authId: user.guid,
            }).createPrivateChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 19468,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canDeleteMessage(state, messageOwner)).toBeTruthy();
            expect(canDeleteMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat with edit rights', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 17742,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canDeleteMessage(state, messageOwner)).toBeTruthy();
            expect(canDeleteMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat without edit rights', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 1358,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canDeleteMessage(state, messageOwner)).toBeFalsy();
            expect(canDeleteMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat for admin', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 2,
                    rights: 32734,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canDeleteMessage(state, messageOwner)).toBeTruthy();
            expect(canDeleteMessage(state, messageIncoming)).toBeTruthy();
        });
    });

    describe('getPollMessageInfo', () => {
        it('should be type=single counters from original', () => {
            const chatId = generateGuid();
            const messages = messagesMock.createSinglePollMessage({
                chatId,
            }, (message) => ({
                ...message,
                prevTimestamp: messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
            }))(1);

            const state = getState({
                messages: messagesMock.createState(messages),
                counters: {},
            });

            expect(getPollMessageInfo(state, messages[0]))
                .toMatchObject({
                    myChoices: [],
                    results: {
                        answers: [],
                        recentVoters: [],
                        version: 0,
                        votedCount: 0,
                    },
                    type: PollMessageType.SINGLE,
                });
        });

        it('should be type=info with counters from original msg', () => {
            const chatId = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 1,
                votedCount: 1,
            };
            const myChoices = [0];

            const messages = messagesMock.createSinglePollMessage({
                chatId,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results,
                        maxChoices: 2,
                        myChoices,
                    },
                },
                prevTimestamp: messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
            }))();

            const state = getState({
                messages: messagesMock.createState(messages),
                counters: {},
            });

            expect(getPollMessageInfo(state, messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });

        it('should be type=info with counters from orig message', () => {
            const chatId = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 3,
                votedCount: 1,
            };
            const myChoices = [0];

            const messages = messagesMock.createSinglePollMessage({
                chatId,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results,
                        maxChoices: 2,
                        myChoices,
                    },
                },
                prevTimestamp: messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
            }))();

            const state = getState({
                messages: messagesMock.createState(messages),
                counters: {
                    [chatId]: {
                        [messages[0].timestamp]: {
                            views: 0,
                            poll: {
                                results: {
                                    recentVoters: [],
                                    answers: [1, 0],
                                    version: 1,
                                    votedCount: 1,
                                },
                                myChoices,
                            },
                        },
                    },
                },
            });

            expect(getPollMessageInfo(state, messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });

        it('should be type=info with counters from forward if no orig message', () => {
            const chatId1 = generateGuid();
            const chatId2 = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 3,
                votedCount: 1,
            };
            const myChoices = [0];

            const forwarded = messagesMock.createSinglePollMessage({
                chatId: chatId1,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results,
                        maxChoices: 2,
                        myChoices,
                    },
                },
            }))();

            const messages = messagesMock.createTextMessage({
                chatId: chatId2,
            }, (message) => ({
                ...message,
                forwarded,
            }))();

            const state = getState({
                messages: messagesMock.createState(messages),
                counters: {},
            });

            expect(getPollMessageInfo(state, forwarded[0], messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });

        it('should be type=info with counters from orig forwarded message', () => {
            const chatId1 = generateGuid();
            const chatId2 = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 3,
                votedCount: 1,
            };
            const myChoices = [0];

            const forwarded = messagesMock.createSinglePollMessage({
                chatId: chatId1,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results: {
                            ...results,
                            version: 1,
                        },
                        maxChoices: 2,
                        myChoices,
                    },
                },
            }))();

            const [forward] = forwarded;

            const forwardedOrigin = messagesMock.createSinglePollMessage()({
                ...forwarded[0],
                data: {
                    poll: {
                        ...forward.data!.poll,
                        results,
                        maxChoices: 2,
                        myChoices,
                    },
                },
                timestamp: forward.timestamp,
            });

            const messages = messagesMock.createTextMessage({
                chatId: chatId2,
            }, (message) => ({
                ...message,
                forwarded,
            }))();

            const state = getState({
                messages: messagesMock.createState([...messages, ...forwardedOrigin]),
                counters: {},
            });

            expect(getPollMessageInfo(state, forward, messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });

        it('should take counters for forward from counters', () => {
            const chatId1 = generateGuid();
            const chatId2 = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 4,
                votedCount: 1,
            };
            const myChoices = [0];

            const forwarded = messagesMock.createSinglePollMessage({
                chatId: chatId1,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results: {
                            ...results,
                            version: 1,
                        },
                        maxChoices: 2,
                        myChoices,
                    },
                },
            }))();

            const [forward] = forwarded;

            const forwardedOrigin = messagesMock.createSinglePollMessage()({
                ...forwarded[0],
                data: {
                    poll: {
                        ...forward.data!.poll,
                        results: {
                            ...results,
                            version: 2,
                        },
                        maxChoices: 2,
                        myChoices,
                    },
                },
                timestamp: forward.timestamp,
            });

            const messages = messagesMock.createTextMessage({
                chatId: chatId2,
            }, (message) => ({
                ...message,
                forwarded,
            }))();

            const state = getState({
                messages: messagesMock.createState([...messages, ...forwardedOrigin]),
                counters: {
                    [chatId1]: {
                        [forward.timestamp]: {
                            views: 0,
                            poll: {
                                results,
                                myChoices,
                            },
                        },
                    },
                },
            });

            expect(getPollMessageInfo(state, forward, messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });

        it('should take counters for forward from original if version in counters lesses', () => {
            const chatId1 = generateGuid();
            const chatId2 = generateGuid();
            const results = {
                recentVoters: [],
                answers: [1, 0],
                version: 4,
                votedCount: 1,
            };
            const myChoices = [0];

            const forwarded = messagesMock.createSinglePollMessage({
                chatId: chatId1,
            }, (message) => ({
                ...message,
                data: {
                    poll: {
                        ...message.data!.poll,
                        results: {
                            ...results,
                            version: 1,
                        },
                        maxChoices: 2,
                        myChoices,
                    },
                },
            }))();

            const [forward] = forwarded;

            const forwardedOrigin = messagesMock.createSinglePollMessage()({
                ...forwarded[0],
                data: {
                    poll: {
                        ...forward.data!.poll,
                        results,
                        maxChoices: 2,
                        myChoices,
                    },
                },
                timestamp: forward.timestamp,
            });

            const messages = messagesMock.createTextMessage({
                chatId: chatId2,
            }, (message) => ({
                ...message,
                forwarded,
            }))();

            const state = getState({
                messages: messagesMock.createState([...messages, ...forwardedOrigin]),
                counters: {
                    [chatId1]: {
                        [forward.timestamp]: {
                            views: 0,
                            poll: {
                                results: {
                                    ...results,
                                    version: 2,
                                },
                                myChoices,
                            },
                        },
                    },
                },
            });

            expect(getPollMessageInfo(state, forward, messages[0]))
                .toMatchObject({
                    myChoices,
                    results,
                    type: PollMessageType.INFO,
                });
        });
    });

    describe('#canEditMessage', () => {
        it('Private chat', () => {
            const chatId = '64b15156-0f58-863c-d311-eacc5d24ab50_8118f21e-10fe-ce62-4e5a-b6571777969c';

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory({
                authId: user.guid,
            }).createPrivateChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 19468,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canEditMessage(state, messageOwner)).toBeTruthy();
            expect(canEditMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat with edit rights', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 17742,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canEditMessage(state, messageOwner)).toBeTruthy();
            expect(canEditMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat without edit rights', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 1,
                    rights: 1358,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canEditMessage(state, messageOwner)).toBeFalsy();
            expect(canEditMessage(state, messageIncoming)).toBeFalsy();
        });

        it('Group chat for admin', () => {
            const chatId = generateGuid();

            const [user] = usersMock.createFrom()();

            const [chat] = chatsMockFactory().createGroupChat({
                chat_id: chatId,
                relations: {
                    role: 2,
                    rights: 32734,
                    version: 1,
                },
            })();

            const [state, [messageOwner, messageIncoming]] = createStateWithChat(chat, user);

            expect(canEditMessage(state, messageOwner)).toBeTruthy();
            expect(canEditMessage(state, messageIncoming)).toBeFalsy();
        });
    });
});
