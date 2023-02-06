import { ExpectedActionType, IMessage, NChats } from '../../types';
import MESSAGE_TYPE = NChats.MESSAGE_TYPE;

export const AUTHOR_1 = {
    first_name: "Анастасия",
    id: "b8c47d81-b7bb-4104-81fa-c302cd32407b",
    last_name: "Кулинчик",
    pn: "Игоревна",
    setup: {},
    username: "a-kulinchik",
};

export const AUTHOR_2 = {
    first_name: "Александр",
    id: "c3e64a2c-cddd-4390-a79c-bbe17456acd4",
    last_name: "Семченко",
    pn: "(staff)",
    setup: {},
    username: "andreevich",
};

export const ITEM: NChats.ITopicMessageItem = {
    author: "b8c47d81-b7bb-4104-81fa-c302cd32407b",
    id: 148717,
    text: "Text",
    timestamp: 1595500956,
    type: MESSAGE_TYPE.PLAINTEXT,
};

export const CHAT = {
    expected_action: {
        text: "",
        type: ExpectedActionType.user_message,
    },
    messages: [] as IMessage[],
    user_last_viewed: 152702,
    users: {
        '0517010c-7aab-4ee5-ad45-4079786b4b03': AUTHOR_1,
        'c3e64a2c-cddd-4390-a79c-bbe17456acd4': AUTHOR_2,
    },
};
