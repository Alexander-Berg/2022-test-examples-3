import { _CHATS } from '../../components/ChatsCommon/types';

export const MENU_TYPE = 'MENU_TYPE';
export const MENU_NAME = 'MENU_NAME';
export const NEW_CHAT_NAME = 'CURRENT_CHAT_NAME';
export const PREV_CHAT_NAME = 'PREV_CHAT_NAME';
const TAG_ID = 'TAG_ID';

export const TAG_DATA = {
    name: NEW_CHAT_NAME,
    tag_id: TAG_ID,
};

export const MOCKED_CHAT = {
    id: 'CHAT_ITEM_ID',
    tag_data: {
        name: PREV_CHAT_NAME,
        tag_id: TAG_ID,
    },
};

export const STORE_FOR_TAG_UPDATE = {
    [_CHATS.ALL]: {
        chats: {
            urgent: [MOCKED_CHAT],
        },
    },

    selectedMenu: _CHATS.ALL,
    selectedMenuItem: null,
    selectedChatItem: { id: null },
    feedIsLoading: false,
    my_chats_are_loading: false,
    all_chats_are_loading: false,
    new_chats_are_loading: false,
};

export const STORE_FOR_LINES_LIST = {
    Chats: {
        [_CHATS.MY]: {
            chats: {
                urgent: [MOCKED_CHAT, MOCKED_CHAT],
            },
        },
        [_CHATS.NEW]: {},
        [_CHATS.ALL]: {},

        selectedMenu: '',
        selectedMenuItem: null,
        selectedChatItem: { id: null },
        feedIsLoading: false,
        my_chats_are_loading: false,
        all_chats_are_loading: false,
        new_chats_are_loading: false,
    },
};
