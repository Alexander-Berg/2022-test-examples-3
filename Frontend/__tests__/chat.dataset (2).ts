import { UserRights } from '../../constants/Chat';
import { DialogInfo, DialogState } from '../../typings/assistant';
import { DialogCompose } from '../../reducers/dialogsComposeses';

export const ALICE_DIALOG_ID = 'alice';

export const PRIVATE_CHAT = {
    avatar: '',
    description: '',
    exclude: [],
    members: ['1', '2'],
    name: '',
    permissions: {
        groups: [],
        users: ['1', '2'],
        departments: [],
    },
    private: true,
    roles: {
        admin: ['1'],
    },
    rights: [UserRights.WRITE, UserRights.READ],
    version: 1549369115812230,
    chat_id: '1_2',
};

export const EMPTY_DIALOG: DialogInfo = {
    title: '',
    image_url: '',
    style: null,
    menu_items: [],
    url: '',
    access_time: 0,
};

export const EMPTY_DIALOG_COMPOSE: DialogCompose = {
    text: '',
    state: DialogState.SUSPENDED,
    shouldListen: false,
    actionTimeout: -1,
    historyFetching: 'ok',
    dialogFetching: 'ok',
};

export const GROUP_CHAT = {
    avatar: '',
    description: '',
    exclude: [],
    members: ['1', '2', '3', '4'],
    name: '',
    permissions: {
        groups: [],
        users: ['1', '2', '3', '4'],
        departments: [],
    },
    private: false,
    roles: {
        admin: ['1'],
    },
    rights: [UserRights.WRITE, UserRights.READ],
    version: 1549369115812230,
    chat_id: '1234',
};

export const ROBOT_CHAT_STATE = {
    chats: {
        '7b39b71f-f82b-4141-9190-778705ba1a73_ca4e0835-0998-4780-949a-fd2004a72c59': {
            members: [
                '7b39b71f-f82b-4141-9190-778705ba1a73',
                'ca4e0835-0998-4780-949a-fd2004a72c59',
            ],
            private: true,
        },
    },
    users: {
        '7b39b71f-f82b-4141-9190-778705ba1a73': {
            status: [
                'is_robot',
            ],
        },
    },
    authId: 'ca4e0835-0998-4780-949a-fd2004a72c59',
};

export const HUMAN_CHAT_STATE = {
    chats: {
        '7b39b71f-f82b-4141-9190-778705ba1a73_ca4e0835-0998-4780-949a-fd2004a72c59': {
            members: [
                '7b39b71f-f82b-4141-9190-778705ba1a73',
                'ca4e0835-0998-4780-949a-fd2004a72c59',
            ],
        },
    },
    users: {
        '7b39b71f-f82b-4141-9190-778705ba1a73': {},
    },
    authId: 'ca4e0835-0998-4780-949a-fd2004a72c59',
};
