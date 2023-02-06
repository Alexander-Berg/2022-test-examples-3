import {flatten} from 'ramda';
import type {Shop} from 'ginny-helpers';

import {getTestingShop} from 'spec/utils';
import {CATALOGER_SHOP_TREE_STATE_KEY, PERS_QA_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';
import {ANSWER_PAGE_SIZE} from '../constants';

type CreateAnswerParams = {
    text: string;
    id: number;
    creationTimestamp: number;
};

type CreateCommentParams = {
    answerId: number;
    text: string;
    shop: Shop;
    id: number;
    creationTimestamp: number;
};

type Entity = {
    entity: string;
    id?: number | string;
    uid?: number;
};

type Votes = {
    likeCount: number;
    dislikeCount: number;
    userVote: number;
};

type Answer = {
    text: string;
    entity: string;
    id: number;
    question: Entity;
    user: Entity;
    author: Entity;
    created: number;
    votes: Votes;
    canDelete: boolean;
    brandId: number | undefined | null;
    shopId: number;
};

type Comment = {
    text: string;
    id: number;
    entityId: number;
    projectId: number;
    state: string;
    changed: boolean;
    updateTime: number;
    createTime: number;
    votes: Votes;
    firstLevelChildCount: number;
    childCount: number;
    user: Entity;
    author: Entity;
    entity: string;
    canDelete: boolean;
    parameters: {
        shopId: string;
        projectId: string;
    };
    comments: any[];
};

const KADAVR_USER_UID = 877808223;
const KADAVR_QUESTION_ID = 1816183;
const KADAVR_QUESTION_ID_2 = 1816184;
export const KADAVR_TESTING_SHOP = 'autotest-questions-1.ru';
export const KADAVR_ANSWER_ID_1 = 1816386;
export const KADAVR_ANSWER_ID_2 = 1816390;
export const KADAVR_PRODUCT_ID = 217311253;

export const KADAVR_QUESTIONS_LIST_STATE = {
    data: [
        {
            text: 'One very interesting autotest question',
            entity: 'question',
            id: KADAVR_QUESTION_ID,
            slug: 'one-very-interesting-autotest-question',
            user: {entity: 'user', uid: KADAVR_USER_UID},
            author: {entity: 'user', id: String(KADAVR_USER_UID)},
            product: {entity: 'product', id: KADAVR_PRODUCT_ID},
            category: null,
            created: 1576597042331,
            votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
            answersCount: 11,
            canDelete: false,
            answers: [],
        },
    ],
    pager: {
        pageNum: 1,
        pageSize: 10,
        count: 1,
        totalPageCount: 1,
        pages: [{num: 1, current: true}],
    },
};

export const KADAVR_QUESTIONS_LIST_STATE_NO_COMMENTS = {
    data: [
        {
            text: 'One very interesting autotest question',
            entity: 'question',
            id: KADAVR_QUESTION_ID_2,
            slug: 'one-very-interesting-autotest-question',
            user: {entity: 'user', uid: KADAVR_USER_UID},
            author: {entity: 'user', id: String(KADAVR_USER_UID)},
            product: {entity: 'product', id: KADAVR_PRODUCT_ID},
            category: null,
            created: 1576597042331,
            votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
            answersCount: 0,
            canDelete: false,
            answers: [],
        },
    ],
    pager: {
        pageNum: 1,
        pageSize: 10,
        count: 1,
        totalPageCount: 1,
        pages: [{num: 1, current: true}],
    },
};

const createAnswer = ({text, id, creationTimestamp}: CreateAnswerParams): Answer => ({
    text,
    entity: 'answer',
    id,
    question: {entity: 'question', id: KADAVR_QUESTION_ID},
    user: {entity: 'user', uid: 404744950},
    author: {entity: 'shop', id: '10301451'},
    created: creationTimestamp,
    votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
    canDelete: true,
    brandId: null,
    shopId: 10301451,
});

export const ANSWERS_PAGE_1 = [
    createAnswer({text: 'The last but not least autotest answer', id: 1816391, creationTimestamp: 1576686321678}),
    createAnswer({text: 'Autotest answer about weather', id: KADAVR_ANSWER_ID_2, creationTimestamp: 1576686270289}),
    createAnswer({text: 'Just to clarify smth autotest answer', id: 1816389, creationTimestamp: 1576686184041}),
    createAnswer({text: 'Some very important autotest answer', id: 1816388, creationTimestamp: 1576686094118}),
    createAnswer({text: 'Yet another autotest answer', id: KADAVR_ANSWER_ID_1, creationTimestamp: 1576680109592}),
];

export const ANSWERS_PAGE_2 = [
    createAnswer({text: 'One more autotest answer', id: 1816387, creationTimestamp: 1576680061234}),
    createAnswer({text: 'In my honest autotest opinion...', id: 1816385, creationTimestamp: 1576675236534}),
];

export const ANSWERS_PAGES = {
    '1': ANSWERS_PAGE_1,
    '2': ANSWERS_PAGE_2,
};

export const KADAVR_ANSWER_SAMPLE = createAnswer({
    text: 'for kadavr response',
    id: KADAVR_ANSWER_ID_2,
    creationTimestamp: 1576680061234,
});

export const KADAVR_SUPPLIER_ANSWER_SAMPLE = {
    text: 'for kadavr response',
    entity: 'answer',
    id: KADAVR_ANSWER_ID_2,
    question: {entity: 'question', id: KADAVR_QUESTION_ID},
    user: {entity: 'user', uid: 404744950},
    author: {entity: 'shop', id: '10696733'},
    created: 1576680061234,
    votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
    canDelete: true,
    brandId: null,
    shopId: 10696733,
};

const totalAnswersCount = flatten(Object.values(ANSWERS_PAGES)).length;

const getPagesState = (currentPageNumber: number) => {
    const pages = Object.keys(ANSWERS_PAGES);

    return pages.reduce((acc, pageNumber) => {
        // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
        acc.push({num: Number(pageNumber), current: Number(pageNumber) === currentPageNumber});

        return acc;
    }, []);
};

export const KADAVR_ANSWERS_LIST_STATE = (pageNumber: number) => ({
    // @ts-expect-error(TS7053) найдено в рамках MARKETPARTNER-16237
    data: ANSWERS_PAGES[pageNumber],
    pager: {
        pageNum: pageNumber,
        pageSize: ANSWER_PAGE_SIZE,
        count: totalAnswersCount,
        totalPageCount: Math.ceil(totalAnswersCount / ANSWER_PAGE_SIZE),
        pages: getPagesState(pageNumber),
    },
});

export const KADAVR_ANSWERS_LIST_STATE_EMPTY = {
    data: [],
    pager: {
        pageNum: 1,
        pageSize: ANSWER_PAGE_SIZE,
        count: 0,
        totalPageCount: 1,
        pages: [{num: 1, current: true}],
    },
};

const createComment = ({answerId, text, shop, id, creationTimestamp}: CreateCommentParams): Comment => ({
    text,
    id,
    entityId: answerId,
    projectId: 5,
    state: 'NEW',
    changed: false,
    updateTime: Math.ceil(creationTimestamp / 1000) * 1000,
    createTime: creationTimestamp,
    votes: {likeCount: 0, dislikeCount: 0, userVote: 0},
    firstLevelChildCount: 0,
    childCount: 0,
    user: {entity: 'user', id: shop.contacts.owner.uid},
    author: {entity: 'shop', id: String(shop.shopId)},
    entity: 'commentary',
    canDelete: true,
    parameters: {shopId: '10301451', projectId: '5'},
    comments: [],
});

const COMMENTS = [
    createComment({
        answerId: KADAVR_ANSWER_ID_2,
        text: 'Comment that you could easily delete',
        shop: getTestingShop(KADAVR_TESTING_SHOP),
        id: 100077229,
        creationTimestamp: 1576827512205,
    }),
    createComment({
        answerId: KADAVR_ANSWER_ID_2,
        text: 'Hi there!',
        shop: getTestingShop(KADAVR_TESTING_SHOP),
        id: 100077228,
        creationTimestamp: 1576827453211,
    }),
    createComment({
        answerId: KADAVR_ANSWER_ID_1,
        text: 'One more comment about completely different thing',
        shop: getTestingShop('autotest-message-00'),
        id: 100077223,
        creationTimestamp: 1576755725877,
    }),
    createComment({
        answerId: KADAVR_ANSWER_ID_1,
        text: 'Comment about nothing',
        shop: getTestingShop(KADAVR_TESTING_SHOP),
        id: 100077222,
        creationTimestamp: 1576755684013,
    }),
    createComment({
        answerId: KADAVR_ANSWER_ID_1,
        text: 'The most necessary comment',
        shop: getTestingShop(KADAVR_TESTING_SHOP),
        id: 100077221,
        creationTimestamp: 1576755440799,
    }),
];

export const KADAVR_COMMENTS_LIST_STATE = {
    data: COMMENTS,
};

export const KADAVR_USER_STATE = {
    id: String(KADAVR_USER_UID),
    uid: {value: String(KADAVR_USER_UID), lite: false, hosted: false},
    login: 'test.lyakhov',
    have_password: true,
    have_hint: true,
    karma: {value: 0},
    karma_status: {value: 0},
    regname: 'test.lyakhov',
    display_name: {
        name: 'Вася Пупкин',
        public_name: 'Вася Пупкин',
        avatar: {default: '61207/462703116-1544492602', empty: false},
    },
    dbfields: {'userinfo.sex.uid': '0'},
};

export const KADAVR_PRODUCT_STATE = {
    titles: {raw: 'Смартфон Xiaomi Mi 9T Pro 6/64GB'},
    pictures: [
        {
            original: {
                url: '//avatars.mds.yandex.net/get-mpic/1568604/img_id8952730975993061463.jpeg/orig',
            },
        },
    ],
};

export const KADAVR_QUESTIONS_DEFAULT_STATE = {
    [PERS_QA_STATE_KEY]: {
        questions: KADAVR_QUESTIONS_LIST_STATE,
        answers: KADAVR_ANSWERS_LIST_STATE_EMPTY,
    },
    [CATALOGER_SHOP_TREE_STATE_KEY]: {
        category: {
            entity: 'category',
            id: 90401,
            isLeaf: false,
            modelsCount: 219,
            name: 'Все товары',
            nid: 54415,
            offersCount: 62,
            slug: 'vse-tovary',
        },
        childrenType: 'mixed',
        entity: 'navnode',
        fullName: 'Все товары',
        hasPromo: false,
        id: 54415,
        isLeaf: false,
        link: {
            params: {
                hid: ['90401'],
                nid: ['54415'],
            },
            target: 'catalog',
        },
        name: 'Все товары',
        navnodes: [
            {
                category: {
                    entity: 'category',
                    fullName: 'Электроника',
                    id: 198119,
                    isLeaf: false,
                    modelsCount: 51,
                    name: 'Электроника',
                    nid: 54440,
                    offersCount: 22,
                    type: 'gurulight',
                    slug: 'elektronika',
                    viewType: 'list',
                },
                childrenType: 'mixed',
                entity: 'navnode',
                fullName: 'Электроника',
                hasPromo: true,
                icons: [
                    {
                        entity: 'picture',
                        url:
                            '//avatars.mds.yandex.net/get-mpic/331398/cms_resources-nav_tree-node-54440-foto_video-0375cb28e972e258ce6fb3c851550b33.svg/svg',
                    },
                ],
                id: 54440,
                isLeaf: false,
                link: {
                    params: {
                        hid: ['198119'],
                        nid: ['54440'],
                    },
                    target: 'department',
                },
                name: 'Электроника',
                rootNavnode: {
                    entity: 'navnode',
                    id: 54440,
                },
                type: 'category',
                slug: 'elektronika',
            },
            {
                category: {
                    entity: 'category',
                    fullName: 'Товары для дома',
                    id: 889900,
                    isLeaf: false,
                    modelsCount: 51,
                    name: 'Товары для дома',
                    nid: 67899,
                    offersCount: 22,
                    type: 'gurulight',
                    slug: 'elektronika',
                    viewType: 'list',
                },
                childrenType: 'mixed',
                entity: 'navnode',
                fullName: 'Товары для дома',
                hasPromo: true,
                icons: [
                    {
                        entity: 'picture',
                        url:
                            '//avatars.mds.yandex.net/get-mpic/331398/cms_resources-nav_tree-node-54440-foto_video-0375cb28e972e258ce6fb3c851550b33.svg/svg',
                    },
                ],
                id: 67899,
                isLeaf: false,
                link: {
                    params: {
                        hid: ['889900'],
                        nid: ['67899'],
                    },
                    target: 'department',
                },
                name: 'Товары для дома',
                rootNavnode: {
                    entity: 'navnode',
                    id: 67899,
                },
                type: 'category',
                slug: 'elektronika',
            },
        ],
        type: 'category',
        slug: 'vse-tovary',
    },
};
