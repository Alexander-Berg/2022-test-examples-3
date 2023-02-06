import produce from 'immer';
import { isPoll } from '../../helpers/messages';
import * as MessagesActions from '../messages';
import { updateMessages, updateHistory, updateHistoryStartTs } from '../sharedActions';
import { generateGuid } from './mock/common';
import { historyMockFactory } from './mock/history';
import { messagesMockFactory } from './mock/messages';
import { mutate } from './mock/utils';

function checkConsistence(registry: Client.ChatMessages, maxTimestamp: number, minTimestamp: number) {
    let message = registry.cache.get(maxTimestamp);

    while (message) {
        if (message.timestamp === minTimestamp) {
            break;
        }

        if (!message.prevTimestamp) {
            return false;
        }

        message = registry.cache.get(message.prevTimestamp);
    }

    if (!message) {
        return false;
    }

    while (message) {
        if (message.timestamp === maxTimestamp) {
            return true;
        }

        if (!message.nextTimestamp) {
            return false;
        }

        message = registry.cache.get(message.nextTimestamp);
    }

    return false;
}

describe('MessagesReducer', () => {
    const authId = generateGuid();
    const messagesMock = messagesMockFactory();
    const historyMock = historyMockFactory();

    describe('updateHistory', () => {
        it('Загрузка полной истории', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            const state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const registry = state.chats[chatId];

            expect(registry.holes.size).toBe(0);
            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual(messages.map((message) => message.timestamp));

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeTruthy();
        });

        it('Заращивание одной дырки', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            const messagesWithHoles = [...messages];
            messagesWithHoles.splice(3, 1);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: messagesWithHoles,
                    }),
                }),
            );

            let registry = state.chats[chatId];

            expect(registry.holes.size).toBe(1);
            expect(registry.holes.get(messages[4].prevTimestamp!)).toBe(messages[4].timestamp);
            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual(messagesWithHoles.map((message) => message.timestamp));

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeFalsy();

            state = MessagesActions.messagesReducer(
                state,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: [messages[3]],
                    }),
                }),
            );

            registry = state.chats[chatId];

            expect(registry.holes.size).toBe(0);
            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([messages[3].timestamp]);

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeTruthy();
        });

        it('Заращивание дырки в несколько сообщений', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            const messagesWithHoles = [...messages];
            messagesWithHoles.splice(3, 3);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: messagesWithHoles,
                    }),
                }),
            );

            let registry = state.chats[chatId];

            expect(registry.holes.size).toBe(1);
            expect(registry.holes.get(messages[6].prevTimestamp!)).toBe(messages[6].timestamp);
            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual(messagesWithHoles.map((message) => message.timestamp));

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeFalsy();

            state = MessagesActions.messagesReducer(
                state,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: [messages[3]],
                    }),
                }),
            );

            registry = state.chats[chatId];

            expect(registry.holes.size).toBe(1);
            expect(registry.holes.get(messages[6].prevTimestamp!)).toBe(messages[6].timestamp);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([messages[3].timestamp]);

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeFalsy();

            state = MessagesActions.messagesReducer(
                state,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: [messages[5]],
                    }),
                }),
            );

            registry = state.chats[chatId];

            expect(registry.holes.size).toBe(1);
            expect(registry.holes.get(messages[5].prevTimestamp!)).toBe(messages[5].timestamp);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([messages[5].timestamp]);

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeFalsy();

            state = MessagesActions.messagesReducer(
                state,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: [messages[4]],
                    }),
                }),
            );

            registry = state.chats[chatId];

            expect(registry.holes.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);
            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([messages[4].timestamp]);

            expect(checkConsistence(
                registry,
                messages[messages.length - 1].timestamp,
                messages[0].timestamp,
            )).toBeTruthy();
        });
    });

    describe('updateMessages', () => {
        it('Обновление сообщения с новой версией', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const [updatedMessage] = messagesMock.createTextMessage({ chatId })({
                timestamp: messages[2].timestamp,
                prevTimestamp: messages[2].prevTimestamp,
                version: 2,
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [updatedMessage],
                    authId,
                ),
            );

            const message = state.chats[chatId].cache.get(updatedMessage.timestamp);

            expect(message).toMatchObject({
                version: 2,
                data: updatedMessage.data,
            });
        });

        it('Обновление сообщения с новым edit_timestamp', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const [updatedMessage] = messagesMock.createTextMessage({ chatId })({
                timestamp: messages[2].timestamp,
                prevTimestamp: messages[2].prevTimestamp,
                version: 1,
                editTimestamp: messagesMock.nextTimestamp(),
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [updatedMessage],
                    authId,
                ),
            );

            const message = state.chats[chatId].cache.get(updatedMessage.timestamp);

            expect(message).toMatchObject({
                version: 1,
                data: updatedMessage.data,
                editTimestamp: updatedMessage.editTimestamp,
            });
        });

        it('Обновление сообщения с новой версией результата опросов', () => {
            const chatId = generateGuid();

            const [messageOrig] = messagesMock.createSinglePollMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))();

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages: [messageOrig],
                    }),
                }),
            );

            const nextPollResults: APIv3.PollResults = {
                version: 3,
                answers: [0, 3],
                votedCount: 3,
            };

            const updatedMessage1 = produce(messageOrig, (draft) => {
                draft.data.poll.results = nextPollResults;
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [updatedMessage1],
                    authId,
                ),
            );

            let nextMessage = state.chats[chatId].cache.get(messageOrig.timestamp);

            expect(nextMessage && isPoll(nextMessage) ? nextMessage.data.poll.results : undefined)
                .toMatchObject(nextPollResults);

            const updatedMessage2 = produce(updatedMessage1, (draft) => {
                draft.data.poll.results = {
                    version: 2,
                    answers: [0, 2],
                    votedCount: 2,
                };
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [updatedMessage2],
                    authId,
                ),
            );

            nextMessage = state.chats[chatId].cache.get(messageOrig.timestamp);

            expect(nextMessage && isPoll(nextMessage) ? nextMessage.data.poll.results : undefined)
                .toMatchObject(nextPollResults);
        });

        it('Отправка и обработка зеркалки', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const [sendingMessage1, sendingMessage2] = messagesMock.createTextMessage({ chatId }, (message) => ({
                ...message,
                timestamp: messagesMock.nextTimestamp() + 999,
                version: 0,
                ack: 'pending',
            }))(2);

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [sendingMessage1],
                    authId,
                ),
            );

            let registry = state.chats[chatId];

            expect(registry.pendingIds.size).toBe(1);
            expect(registry.pendingIds.get(sendingMessage1.messageId)).toBe(sendingMessage1.timestamp);

            expect(registry.sendQueue.length).toBe(1);
            expect(registry.sendQueue[0]).toBe(sendingMessage1.timestamp);

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [sendingMessage2],
                    authId,
                ),
            );

            registry = state.chats[chatId];

            expect(registry.pendingIds.size).toBe(2);
            expect(registry.pendingIds.get(sendingMessage1.messageId)).toBe(sendingMessage1.timestamp);
            expect(registry.pendingIds.get(sendingMessage2.messageId)).toBe(sendingMessage2.timestamp);

            expect(registry.sendQueue.length).toBe(2);
            expect(registry.sendQueue[0]).toBe(sendingMessage1.timestamp);
            expect(registry.sendQueue[1]).toBe(sendingMessage2.timestamp);

            const mirrorMessage2 = mutate(sendingMessage2, {
                timestamp: messagesMock.nextTimestamp(),
                prevTimestamp: messages[messages.length - 1].timestamp,
                version: 1,
                seqno: 11,
                ack: undefined,
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [mirrorMessage2],
                    authId,
                ),
            );

            registry = state.chats[chatId];

            expect(registry.pendingIds.size).toBe(1);
            expect(registry.pendingIds.get(sendingMessage1.messageId)).toBe(sendingMessage1.timestamp);

            expect(registry.sendQueue.length).toBe(1);
            expect(registry.sendQueue[0]).toBe(sendingMessage1.timestamp);

            expect(checkConsistence(
                registry,
                mirrorMessage2.timestamp,
                messages[0].timestamp,
            )).toBeTruthy();

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([mirrorMessage2.timestamp]);

            const mirrorMessage1 = mutate(sendingMessage1, {
                timestamp: messagesMock.nextTimestamp(),
                prevTimestamp: mirrorMessage2.timestamp,
                version: 1,
                seqno: 12,
                ack: undefined,
            });

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [mirrorMessage1],
                    authId,
                ),
            );

            registry = state.chats[chatId];

            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);

            expect(checkConsistence(
                registry,
                mirrorMessage1.timestamp,
                messages[0].timestamp,
            )).toBeTruthy();

            expect(registry[MessagesActions.UPDATER].newTs)
                .toEqual([mirrorMessage1.timestamp]);
        });
    });

    describe('removeMessage', () => {
        it('Удаление отправляемого сообщения', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const [sendingMessage1] = messagesMock.createTextMessage({ chatId }, (message) => ({
                ...message,
                timestamp: messagesMock.nextTimestamp() + 999,
                version: 0,
                ack: 'pending',
            }))(1);

            state = MessagesActions.messagesReducer(
                state,
                updateMessages(
                    [sendingMessage1],
                    authId,
                ),
            );

            const registryOld = state.chats[chatId];

            state = MessagesActions.messagesReducer(
                state,
                MessagesActions.removeMessages([{
                    chatId,
                    timestamp: sendingMessage1.timestamp,
                }]),
            );

            const registry = state.chats[chatId];

            expect(registry.pendingIds.size).toBe(0);
            expect(registry.sendQueue.length).toBe(0);

            expect(registry).not.toBe(registryOld);
        });
    });

    describe('updateHistoryStartTs', () => {
        it('Смещает historyStartTs и удаляет историю выше', () => {
            const chatId = generateGuid();

            const messages = messagesMock.createTextMessage({ chatId, version: 1 }, (message, index) => ({
                ...message,
                prevTimestamp: index === 0 ? undefined : messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
                seqno: index + 1,
            }))(10);

            let state = MessagesActions.messagesReducer(
                undefined,
                updateHistory({
                    authId,
                    users: [],
                    chats: [],
                    histories: historyMock.createChatHistory()({
                        chat_id: chatId,
                        messages,
                    }),
                }),
            );

            const startTs = messagesMock.currentTimestamp() - 6000;

            state = MessagesActions.messagesReducer(
                state,
                updateHistoryStartTs(chatId, startTs, 6),
            );

            const registry = state.chats[chatId];

            expect(registry.historyStartTs).toBe(startTs);
            expect(registry.cache.size).toBe(3);
            expect(registry.cache.get(startTs - 2000)).toBe(undefined);
            expect(registry.cache.get(startTs)).toBe(undefined);
            expect(registry.cache.get(startTs + 2000)).not.toBe(undefined);

            expect(registry.lastTimestamp).toBe(messagesMock.currentTimestamp());
        });
    });
});
