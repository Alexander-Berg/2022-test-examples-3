import * as faker from 'faker';
import { ChatsState } from '../../chats';
import { producer, objectComposer, entityCreatorFactory, getRandomCount } from './utils';
import { generateGuid, generateGuids } from './common';
import { buildChat } from '../../../helpers/chat';
import { buildRelation } from '../../../helpers/relations';
import { transformRightsToNumber } from '../../../lib/compat';
import type { RecommendedChatsState } from '../../recomendatedChats';

const chatStateProducerFactory = producer(objectComposer<APIv3.Chat, ChatsState>((chat: APIv3.Chat) => chat.chat_id));
const recommendedChatsStateProducerFactory = producer((chats: APIv3.Chat[]): RecommendedChatsState => {
    return {
        chats: chats.reduce((aux, chat) => {
            aux[chat.chat_id] = chat;

            return aux;
        }, {}),
        status: 'ok',
    };
});

const commonChatFactory = (props: Partial<APIv3.Chat> = {}): APIv3.Chat => {
    return buildChat({
        avatar_id: faker.random.word(),
        description: faker.random.words(),
        name: faker.random.words(2),
        chat_id: generateGuid(),
        version: 1,
        ...props,
    });
};

interface ChatsStateFactoryProps {
    authId?: string;
    membersPull?: string[];
}

function entityCreatorFactoryProducer(mapper: (chat?: Partial<APIv3.Chat>) => Partial<APIv3.Chat>) {
    return (common: Partial<APIv3.Chat> = {}) =>
        entityCreatorFactory((chat: Partial<APIv3.Chat> | string | undefined) => {
            const obj = typeof chat === 'string' ? { chat_id: chat } : chat;

            return commonChatFactory({
                ...common,
                ...mapper(obj || {}),
            });
        });
}

export function chatsMockFactory(props: ChatsStateFactoryProps = {}) {
    const {
        authId = generateGuid(),
        membersPull = generateGuids(50),
    } = props;

    return {
        authId,
        generateChannelsIds: (count: number, namespace: number) => generateGuids(count).map((guid) => `1/${namespace}/${guid}`),
        generateGroupsIds: (count: number, namespace: number) => generateGuids(count).map((guid) => `0/${namespace}/${guid}`),
        generateBussinessIds: (count: number, namespace: number) => generateGuids(count).map((guid) => `2/${namespace}/${guid}`),
        generatePrivateId: (count: number) => generateGuids(count).map((guid) => [guid, authId].sort().join('_')),
        generatePrivateIdWith: (...guids: string[]) => guids.map((guid) => [guid, authId].sort().join('_')),
        createPrivateChatWith: (guid: string, chat: Partial<APIv3.Chat> = {}) => (commonChatFactory({
            ...chat,
            chat_id: [guid, authId].sort().join('_'),
            private: true,
            members: authId === guid ? [authId] : [authId, guid],
            partner_guid: guid,
        })),
        createPrivateChat: entityCreatorFactoryProducer((chat = {}) => {
            const [
                partnerGuid = getRandomCount(membersPull, 1)[0],
            ] = (chat.chat_id?.split('_') || []).filter((item) => item !== authId);

            return {
                ...chat,
                private: true,
                members: chat.members ? chat.members : [authId, partnerGuid],
                partner_guid: partnerGuid,
            };
        }),
        createChatWithSelf: entityCreatorFactoryProducer((chat = {}) => ({
            ...chat,
            chat_id: [authId, authId].sort().join('_'),
            private: true,
            members: [authId, authId],
        })),
        createGroupChat: entityCreatorFactoryProducer((chat = {}) => ({
            ...chat,
            private: false,
            members: chat.members ? chat.members : [
                authId,
                ...getRandomCount(membersPull, chat.member_count || Math.floor(Math.random() * 30)),
            ],
        })),
        createChannel: entityCreatorFactoryProducer((chat = {}) => ({
            ...chat,
            private: false,
            channel: true,
            members: chat.members ? chat.members : [
                authId,
                ...getRandomCount(membersPull, chat.member_count || Math.floor(Math.random() * 30)),
            ],
        })),
        createState: chatStateProducerFactory(commonChatFactory),
        createRecommendedState: recommendedChatsStateProducerFactory(commonChatFactory),
        createRelation: (
            rights: Yamb.UserRights[],
            version: number = 0,
            role: number = 0,
        ) => buildRelation(transformRightsToNumber(rights), version, role),
    };
}
