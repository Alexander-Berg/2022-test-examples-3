import { BoardSchema } from 'schema/board/BoardSchema';

import { SourceType } from 'enums/sourceType';

export const BOARD_DEFAULT: BoardSchema = {
    collabs: [],
    direct_data: {
        url: '',
        visit_href: '',
        tracking_url_prefix: '',
    },
    id: '',
    is_default: false,
    is_liked: false,
    is_new: false,
    is_private: false,
    is_subscribed: false,
    is_wishlist: false,
    // @ts-ignore
    owner: {
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
            offer_cards: 0,
        },
        display_name: '135592191551874802',
        preferences: {},
        language: 'ru',
        sex: 'm',
        is_forbid: false,
        default_avatar_id: '0/0-0',
        is_restricted: true,
        login: 'Вася Пупкин',
    },
    service: {
        created_at: '',
        updated_at: '',
    },
    slug: '',
    stat: {
        geo_cards_count: 0,
        offer_for_publication: true,
        views_count: 0,
        subscribers_count: 0,
        fulfilled_count: 0,
        collabs_count: 0,
        immediate_child_count: 0,
        cards_count: 1,
    },
    title: '',
};

export const BOARDS = [
    {
        ...BOARD_DEFAULT,
        title: 'Interiors design',
        description: '',
        is_private: false,
        is_wishlist: false,
    },
    { ...BOARD_DEFAULT, title: 'Food', description: '', is_private: false, is_wishlist: false },
    {
        ...BOARD_DEFAULT,
        title: 'DIY',
        description: 'Do it yourself is the method of building, modifying, or repairing things without the direct aid of experts or professionals.',
        is_private: false,
        is_wishlist: false,
    },
    { ...BOARD_DEFAULT, title: 'Drinks', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Funny cats', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'My dog', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'My wish list', description: '', is_private: false, is_wishlist: true },
    { ...BOARD_DEFAULT, title: 'Dreams', description: '', is_private: true, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Travel', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Minsk', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'St.Petersburg', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Stockholm', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Madrid', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Barcelona', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Santa Cruz de Tenerife', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Las Palmas de Grand Canaria', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Lisbon', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Porto', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Milano', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Verona', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Venezia', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Riva del Garda', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Warsaw', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Prague', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Rome', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Portugal', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Italy', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Poland', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Slovakia', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Spain', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Canary Islands', description: '', is_private: false, is_wishlist: false },
    { ...BOARD_DEFAULT, title: 'Czech Republic', description: '', is_private: false, is_wishlist: false },
];

export const BOARD_PRIVATE: BoardSchema = {
    ...BOARD_DEFAULT,
    title: 'PrivateBoard',
    card_source_type: SourceType.IMAGE,
    description: '',
    is_private: true,
    is_wishlist: false,
};

export const BOARD_WISH_LIST: BoardSchema = {
    ...BOARD_DEFAULT,
    title: 'Wish list',
    card_source_type: SourceType.IMAGE,
    description: '',
    is_private: false,
    is_wishlist: true,
};
