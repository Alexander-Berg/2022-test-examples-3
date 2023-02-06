import { MessageType, PlainMessageContentType, PollMessageType } from '../../constants/message';

import {
    createTextData,
    findLastDeletedMessage,
    getForwardedMessage,
    getImagePreviewUrlStateless,
    getPlainMessageContentType,
    getPollType,
    isForwardedMessage,
    isReplyMessage,
} from '../messages';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';
import { generateGuid } from '../../store/__tests__/mock/common';
import { PreviewSizes } from '../../typings';

function getMessage(partialMessage: Partial<APIv3.Message> = {}) {
    return partialMessage as APIv3.Message;
}

const emptyMessage: APIv3.Message = {
    type: MessageType.UNKNOWN,
    chatId: '',
    deleted: false,
    from: {
        display_name: '',
        guid: '',
        version: 0,
    },
    messageId: '',
    timestamp: 0,
};

const INVALID_MESSAGE_DATA = {} as unknown as any;

describe('MessagesHelper', () => {
    const messagesMock = messagesMockFactory();

    describe('#getForwardedMessage', () => {
        it('Should exported messages data in right sort', () => {
            const messageId = 'm1';

            const forwardedMessage0 = getMessage({
                ...emptyMessage,
                messageId: 'fw1',
            });

            const forwardedMessage1 = getMessage({
                ...emptyMessage,
                messageId: 'fw2',
            });

            const message = {
                ...emptyMessage,
                messageId: messageId,
                forwarded: [forwardedMessage0, forwardedMessage1],
            };

            expect(getForwardedMessage(message, 0)).toBe(forwardedMessage0);
            expect(getForwardedMessage(message, 1)).toBe(forwardedMessage1);
        });
    });

    describe('#getPlainMessageContentType', () => {
        it('Should return undefined when message.data is undefined', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: undefined,
            })).toEqual(undefined);
        });

        it('Should return IMAGE when message.data has key image', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: {
                    image: {
                        width: 0,
                        height: 0,
                        file_info: {
                            name: '',
                            size: 0,
                        },
                    },
                },
            })).toEqual(PlainMessageContentType.IMAGE);
        });

        it('Should return FILE when message.data has key file', () => {
            expect(
                getPlainMessageContentType(messagesMock.createFileMessage()()[0]),
            ).toEqual(PlainMessageContentType.FILE);
        });

        it('Should return TEXT when message.data has key text', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: createTextData(''),
            })).toEqual(PlainMessageContentType.TEXT);
        });

        it('Should return STICKER when message.data has key sticker', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: {
                    sticker: {
                        id: '0',
                        set_id: '0',
                    },
                },
            })).toEqual(PlainMessageContentType.STICKER);
        });

        it('Should return CARD when message.data has key card', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: {
                    card: {
                        data: {},
                    },
                },
            })).toEqual(PlainMessageContentType.CARD);
        });

        it('Should return undefined when message.data has no known key', () => {
            expect(getPlainMessageContentType({
                ...emptyMessage,
                data: INVALID_MESSAGE_DATA,
            })).toEqual(undefined);
        });
    });

    describe('#isForwardedMessageStateless', () => {
        it('Should return true when message.forwarded is not empty and message is not reply', () => {
            expect(isForwardedMessage({
                ...emptyMessage,
                forwarded: [emptyMessage, emptyMessage],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(true);
        });

        it('Should return false when message.forwarded is undefined', () => {
            expect(isForwardedMessage({
                ...emptyMessage,
                forwarded: undefined,
            })).toEqual(false);
        });

        it('Should return false when message is reply', () => {
            expect(isForwardedMessage({
                ...emptyMessage,
                forwarded: [emptyMessage],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(false);
        });
    });

    describe('#isReplyMessage', () => {
        it('Should return true when message.forwarded.length is 1 and message.forwarded[0].chat_id is equal to message.chat_id and message.data is not undefined', () => {
            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: [emptyMessage],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(true);
        });

        it('Should return false when message.forwarded is undefined', () => {
            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: undefined,
                data: INVALID_MESSAGE_DATA,
            })).toEqual(false);
        });

        it('Should return false when message.forwarded.length is more than 1', () => {
            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: [emptyMessage, emptyMessage],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(false);

            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: [emptyMessage, emptyMessage, emptyMessage],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(false);
        });

        it('Should return false when message.forwarded[0].chat_id is not equal to message.chat_id', () => {
            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: [
                    {
                        ...emptyMessage,
                        chatId: '1',
                    },
                ],
                data: INVALID_MESSAGE_DATA,
            })).toEqual(false);
        });

        it('Should return true when message.data is undefined', () => {
            expect(isReplyMessage({
                ...emptyMessage,
                forwarded: [emptyMessage],
                data: undefined,
            })).toEqual(true);
        });
    });

    describe('#findLastDeletedMessage', () => {
        it('Should return last deleted message timestamp', () => {
            const chatId = generateGuid();
            const messages = messagesMock.createTextMessage({
                chatId,
            }, (message) => ({
                ...message,
                prevTimestamp: messagesMock.currentTimestamp(),
                timestamp: messagesMock.nextTimestamp(),
            }))(9);

            const registry = messagesMock.createState(messages).chats[chatId];

            expect(findLastDeletedMessage(registry, messages[5].timestamp)?.timestamp)
                .toBe(messages[5].timestamp);

            const message6 = registry.cache.get(messages[6].timestamp);

            if (!message6) {
                throw new Error('Message not found');
            }

            message6.deleted = true;

            expect(findLastDeletedMessage(registry, message6.timestamp)?.timestamp)
                .toBe(message6.timestamp);

            const message7 = registry.cache.get(messages[7].timestamp);

            if (!message7) {
                throw new Error('Message not found');
            }

            message7.deleted = true;

            expect(findLastDeletedMessage(registry, messages[5].timestamp)?.timestamp)
                .toBe(message7.timestamp);

            const message8 = registry.cache.get(messages[8].timestamp);

            if (!message8) {
                throw new Error('Message not found');
            }

            message8.deleted = true;

            expect(findLastDeletedMessage(registry, messages[5].timestamp)?.timestamp)
                .toBe(message8.timestamp);
        });
    });

    describe('#getImagePreviewUrl', () => {
        it('Should return url by image', () => {
            const [message] = messagesMock.createImgMessage()();

            expect(getImagePreviewUrlStateless(message, PreviewSizes.MIDDLE))
                .toEqual(`imagePreviewUrl?{"fileId":"${message.data.image.file_info.id}","size":"middle"}`);
        });

        it('Should return empty string for file', () => {
            const [message] = messagesMock.createFileMessage()();

            expect(getImagePreviewUrlStateless(message, PreviewSizes.MIDDLE))
                .toEqual('');
        });

        it('Should fallback to empty string', () => {
            expect(getImagePreviewUrlStateless(emptyMessage, PreviewSizes.MIDDLE)).toEqual('');
        });
    });

    describe('#getPollType', () => {
        it('Should return SINGLE, if myChoices is undefined and max choices <= 1', () => {
            expect(getPollType(1, undefined, false))
                .toEqual(PollMessageType.SINGLE);
        });

        it('Should return MULTIPLE, if myChoices is undefined and max choices > 1', () => {
            expect(getPollType(4, undefined, false))
                .toEqual(PollMessageType.MULTIPLE);
        });

        it('Should return INFO, if myChoices is not empty array and max choices >= 1', () => {
            expect(getPollType(1, [0], false))
                .toEqual(PollMessageType.INFO);
        });

        it('Should return INFO, if myChoices is empty array and max choices > 1', () => {
            expect(getPollType(3, [], false))
                .toEqual(PollMessageType.MULTIPLE);
        });

        it('Should return SINGLE, if myChoices is empty array and max choices == 1', () => {
            expect(getPollType(1, [], false))
                .toEqual(PollMessageType.SINGLE);
        });

        it('Should return INFO, if poll completed', () => {
            expect(getPollType(1, [], true))
                .toEqual(PollMessageType.INFO);
        });
    });
});
