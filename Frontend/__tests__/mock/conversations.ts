import { producer, objectComposer, entityCreatorFactory } from './utils';
import { ConversationsState } from '../../conversations';
import { generateGuid } from './common';

type ConversationTemplate = Partial<Client.Conversation> & { chatId?: string };

const ID_SYMBOL = Symbol('id');

const conversationsStateProducerFactory = producer<
    ConversationTemplate,
    ConversationsState,
    Client.Conversation
>(
    objectComposer<ConversationTemplate, ConversationsState>((conversation) => conversation[ID_SYMBOL]),
);

export const conversationFactory = (props: ConversationTemplate | undefined): Client.Conversation => {
    if (!props?.chatId) {
        throw new Error('chatId is required');
    }

    const {
        chatId = generateGuid(),
        ...rest
    } = props || {};

    const conversation = {
        commited_last_edit_timestamp: 0,
        prev_last_edit_timestamp: 0,
        last_seen_timestamp: 0,
        last_seen_by_me_timestamp: 0,
        last_seen_seqno: 0,
        last_seen_by_me_seqno: 0,
        last_seqno: 0,
        last_timestamp: 0,
        unread: 0,
        ...rest,
    };

    Object.defineProperty(conversation, ID_SYMBOL, {
        value: chatId,
        enumerable: false,
        writable: false,
    });

    return conversation;
};

function entityCreatorFactoryProducer() {
    return (common: ConversationTemplate = {}) =>
        entityCreatorFactory((data: ConversationTemplate | string | undefined) => {
            const chatId = typeof data === 'string' ? data : (data?.chatId || generateGuid());

            return conversationFactory({
                ...common,
                ...(typeof data === 'string' ? { chatId } : { ...data, chatId }),
            });
        });
}

export function conversationsMockFactory() {
    return {
        createConversation: entityCreatorFactoryProducer(),
        createState: conversationsStateProducerFactory(conversationFactory),
    };
}
