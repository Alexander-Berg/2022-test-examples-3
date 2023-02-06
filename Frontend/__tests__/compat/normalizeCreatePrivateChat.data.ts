export const CREATE_PRIVATE_CHAT_NORMAL_RESPONSE: Yamb.API.CreatePrivateChat.Response = {
    user: {
        status: ['is_robot'],
        display_name: 'The newest диалог для yandex',
        gender: 'male',
        nickname: 'test_maps_yandex_ru_1_bot',
        version: 1565017581290022,
        guid: '7162de39-4140-455f-9c2a-5966b6d4b4b8',
        avatar_id: 'user_avatar/dialogs/399212/fea4d163fd2615d4e3ce',
        is_robot: true,
    },
    chat: {
        moderation_status: 'ok',
        roles: {
            admin: [],
        },
        rights: ['read', 'write'],
        private: true,
        version: 1566468348397960,
        chat_id: '58d89954-d738-4dea-ab89-3a02df11750d_7162de39-4140-455f-9c2a-5966b6d4b4b8',
        members: ['58d89954-d738-4dea-ab89-3a02df11750d', '7162de39-4140-455f-9c2a-5966b6d4b4b8'],
        exclude: [],
        create_timestamp: 1566468348.39796,
        permissions: {
            groups: [],
            users: ['58d89954-d738-4dea-ab89-3a02df11750d', '7162de39-4140-455f-9c2a-5966b6d4b4b8'],
            departments: [],
        },
    },
};

/**
 * В неполном ответе поля right и members приходят с пустыми массивами
 * @see https://st.yandex-team.ru/MSSNGRFRONT-3716
 */
export const CREATE_PRIVATE_CHAT_BAD_RESPONSE: Yamb.API.CreatePrivateChat.Response = {
    user: {
        status: ['is_robot'],
        display_name: 'The newest диалог для yandex',
        gender: 'male',
        nickname: 'test_maps_yandex_ru_1_bot',
        version: 1565017581290022,
        guid: '7162de39-4140-455f-9c2a-5966b6d4b4b8',
        avatar_id: 'user_avatar/dialogs/399212/fea4d163fd2615d4e3ce',
        is_robot: true,
    },
    chat: {
        moderation_status: 'ok',
        roles: {
            admin: [],
        },
        rights: [],
        private: true,
        version: 1566468348397960,
        chat_id: '58d89954-d738-4dea-ab89-3a02df11750d_7162de39-4140-455f-9c2a-5966b6d4b4b8',
        members: [],
        exclude: [],
        create_timestamp: 1566468348.39796,
        permissions: {
            groups: [],
            users: ['58d89954-d738-4dea-ab89-3a02df11750d', '7162de39-4140-455f-9c2a-5966b6d4b4b8'],
            departments: [],
        },
    },
};
