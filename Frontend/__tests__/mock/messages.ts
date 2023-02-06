import * as faker from 'faker';
import { createUUID } from '@yandex-int/messenger.sdk';
import { entityCreatorFactory } from './utils';
import { userFactory } from './user';
import { MessageType } from '../../../constants/message';
import { generateGuid, generateTsMcs } from './common';
import { messagesReducer } from '../../messages';
import { updateMessages } from '../../sharedActions';
import { AppState } from '../..';

let currentTime = generateTsMcs();

function nextTimestamp() {
    return currentTime += 2000;
}

const messageFactory = <D extends APIv3.MessageDataTypes>(
    props: Partial<APIv3.TypedMessage<D>> = {},
): APIv3.TypedMessage<D> => {
    const {
        timestamp = nextTimestamp(),
        version = 1,
        ...rest
    } = props;

    return {
        type: MessageType.PLAIN,
        chatId: faker.random.uuid(),
        messageId: faker.random.uuid(),
        ack: 'sent',
        seqno: 1,
        from: userFactory(),
        timestamp,
        deleted: false,
        version,
        ...rest,
    } as APIv3.TypedMessage<D>;
};

function entityCreatorFactoryProducer<D extends APIv3.MessageDataTypes>(
    mapper: (message?: Partial<APIv3.Message<D>>) => Partial<APIv3.Message<D>>,
) {
    return (
        common: Partial<APIv3.Message<D>> = {},
        customMapper = (message: Partial<APIv3.Message<D>>, _: number) => message,
    ) =>
        entityCreatorFactory((messageOrId: Partial<APIv3.Message<D>> | string | undefined, index) => {
            const obj = typeof messageOrId === 'string' ? { messageId: messageOrId } : messageOrId;

            return messageFactory<D>(customMapper({
                ...common,
                ...mapper((obj || {}) as any),
            }, index));
        });
}

export function messagesMockFactory() {
    const image = {
        width: 300,
        height: 300,
        file_info: {
            id: createUUID(),
            name: faker.name.title(),
            size: faker.random.number(),
        },
    };

    return {
        currentTimestamp() {
            return currentTime;
        },
        nextTimestamp,
        createState(
            messages: APIv3.Message[],
            authId: string = generateGuid(),
            state?: AppState['messages'],
        ) {
            return messagesReducer(state, updateMessages(messages, authId));
        },
        getMessageKeys(...messages: APIv3.Message<any>[]): APIv3.MessageKey[] {
            return messages.map((message) => ({
                chatId: message.chatId,
                timestamp: message.timestamp,
            }));
        },
        createTextMessage: entityCreatorFactoryProducer<APIv3.PlainMessageText>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                text: {
                    message_text: '',
                    tokens: {
                        children: [{ children: [], tag: { type: 0, groups: 0 }, match: [faker.lorem.text(100)] }],
                    },
                    links: [],
                    emojies: [],
                },
            },
            ...message,
        })),
        createFileMessage: entityCreatorFactoryProducer<APIv3.PlainMessageFile>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                file: {
                    file_info: {
                        id: faker.random.uuid(),
                        size: faker.random.number(),
                        name: faker.name.title(),
                    },
                },
            },
            ...message,
        })),
        createImgMessage: entityCreatorFactoryProducer<APIv3.PlainMessageImage>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                image,
            },
            ...message,
        })),
        createGalleryMessage: entityCreatorFactoryProducer<APIv3.PlainMessageGallery>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                gallery: {
                    items: [image, image],
                    tokens: {
                        children: [{ children: [], tag: { type: 0, groups: 0 }, match: [faker.lorem.text(100)] }],
                    },
                    links: [],
                    text: faker.lorem.text(100),
                },
            },
            ...message,
        })),
        createVoiceMessage: entityCreatorFactoryProducer<APIv3.PlainMessageVoice>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                voice: {
                    file_info: {
                        size: faker.random.number(),
                        name: faker.name.title(),
                    },
                    duration: faker.random.number(),
                    was_recognized: false,
                    waveform: '123454321',
                },
            },
            ...message,
        })),
        createSinglePollMessage: entityCreatorFactoryProducer<APIv3.PlainMessagePoll>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                poll: {
                    answers: faker.lorem.words(5).split(' '),
                    isAnonymous: true,
                    maxChoices: 1,
                    myChoices: [],
                    results: {
                        answers: [],
                        recentVoters: [],
                        version: 0,
                        votedCount: 0,
                    },
                    title: faker.name.title(),
                },
            },
            ...message,
        })),
        createMultiplePollMessage: entityCreatorFactoryProducer<APIv3.PlainMessagePoll>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                poll: {
                    answers: faker.lorem.words(5).split(' '),
                    isAnonymous: true,
                    maxChoices: 5,
                    myChoices: [],
                    results: {
                        answers: [],
                        recentVoters: [],
                        version: 0,
                        votedCount: 0,
                    },
                    title: faker.name.title(),
                },
            },
            ...message,
        })),
        createInfoPollMessage: entityCreatorFactoryProducer<APIv3.PlainMessagePoll>((message = {}) => ({
            type: MessageType.PLAIN,
            data: {
                poll: {
                    answers: faker.lorem.words(5).split(' '),
                    isAnonymous: true,
                    maxChoices: 5,
                    myChoices: [2, 4],
                    results: {
                        answers: [10, 15, 17, 13, 20],
                        recentVoters: [],
                        version: 0,
                        votedCount: 20,
                    },
                    title: faker.name.title(),
                },
            },
            ...message,
        })),
    };
}
