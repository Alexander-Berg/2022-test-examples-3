import * as faker from 'faker';
import { entityCreatorFactory } from './utils';

const chatHistoryFactory = (props: Partial<APIv3.ChatHistory> = {}): APIv3.ChatHistory => {
    const {
        chat_id: chatId = faker.random.uuid(),
        messages = [],
        last_seqno = 0,
        last_timestamp = 0,
        last_seen_by_me_seqno = 0,
        last_seen_timestamp = 0,
        last_seen_by_me_timestamp = 0,
        last_seen_seqno = 0,
        ...rest
    } = props;

    return {
        last_timestamp: messages.reduce((aux, message) => Math.max(aux, message.timestamp), last_timestamp),
        last_seqno: messages.reduce((aux, message) => Math.max(aux, message.timestamp), last_seqno),
        last_seen_by_me_seqno,
        last_seen_by_me_timestamp,
        last_seen_seqno,
        last_seen_timestamp,
        chat_id: chatId,
        historyStartTs: 0,
        messages: messages.map((message) => {
            message.chatId = chatId;

            return message;
        }),
        ...rest,
    };
};

function entityCreatorFactoryProducer(
    mapper: (message?: Partial<APIv3.ChatHistory>) => Partial<APIv3.ChatHistory>,
) {
    return (common: Partial<APIv3.ChatHistory> = {}) =>
        entityCreatorFactory((chatHistoryOrId: Partial<APIv3.ChatHistory> | string | undefined) => {
            const obj = typeof chatHistoryOrId === 'string' ? { chat_id: chatHistoryOrId } : chatHistoryOrId;

            return chatHistoryFactory({
                ...common,
                ...mapper(obj || {}),
            });
        });
}

export function historyMockFactory() {
    return {
        createChatHistory: entityCreatorFactoryProducer((history = {}) => ({
            ...history,
        })),
    };
}
