import { UserSchema } from 'schema/user/UserSchema';

export const USER_DEFAULT: UserSchema = {
    id: '',
    inbox: 0,
    is_business: false,
    is_subscribed: false,
    offline: false,
    role: '',
    uid: 0,
    yt_alias: '',
    stats: {
        series_cards: 0,
        board_subscriptions: 0,
        collab_boards: 0,
        user_subscriptions: 0,
        product_cards: 0,
        organization_cards: 0,
        boards_likes: 0,
        own_likes: 0,
        image_cards: 0,
        video_cards: 0,
        subscribers: 0,
        film_cards: 0,
        boards: 0,
        companies: 0,
        own_boards_likes: 0,
        theme_subscriptions: 0,
        cards: 0,
    },
    display_name: '135592191551874802',
    preferences: {
        general: {
            welcome_popup_2021: 'faq',
            welcome_popup: 1,
        },
    },
    language: 'ru',
    sex: 'm',
    is_forbid: false,
    default_avatar_id: '0/0-0',
    is_restricted: true,
    public_id: 'Вася Пупкин',
};

export const USERS = {
    USER_DEFAULT,
};
