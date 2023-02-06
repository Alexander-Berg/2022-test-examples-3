import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

const answerId = 666;
const questionId = 555;
const questionSlug = 'chto-sdelat-ne-aktiviruetsia-kalendar-i-diktofon-poiavliaetsia-otchet-ob-oshibke';
const productId = 1722193751;
const productTitle = 'Смартфон Samsung Galaxy S8';
const productSlug = 'smartfon-samsung-galaxy-s8';
const product = createProduct({
    titles: {
        raw: productTitle,
    },
    categories: [
        {
            entity: 'category',
            id: 91491,
            name: 'Мобильные телефоны',
            fullName: 'Мобильные телефоны',
            type: 'guru',
            isLeaf: true,
        },
    ],
    slug: productSlug,
}, productId);
const userUid = Number(profiles.ugctest3.uid);
const otherUserUid = 123456;
const question = {
    id: questionId,
    product: {
        id: productId,
    },
    user: {
        uid: userUid,
    },
    slug: questionSlug,
};

const getAnswers = ({authorUid = userUid, canDelete = false} = {}) => [{
    id: answerId,
    user: {
        uid: authorUid,
    },
    author: {
        id: authorUid,
        entity: 'user',
    },
    canDelete: authorUid === userUid && canDelete,
    question: {
        id: questionId,
    },
}];

const getQuestions = () => [question];

const getUsers = () => ([{
    id: userUid,
    uid: {
        value: userUid,
    },
    login: 'lol',
    public_id: 'lolpop112233',
    display_name: {
        name: 'lol pop',
        display_name_empty: false,
    },
    dbfields: {
        'userinfo.firstname.uid': 'lol',
        'userinfo.lastname.uid': 'pop',
    },
}, {
    id: otherUserUid,
    uid: {
        value: otherUserUid,
    },
    login: 'unknown',
    public_id: 'unknown112233',
    dbfields: {
        'userinfo.firstname.uid': 'Someone',
        'userinfo.lastname.uid': 'Unknown',
    },
    display_name: {
        avatar: {},
    },
}]);

export {
    answerId,
    questionId,
    questionSlug,
    productId,
    productTitle,
    productSlug,
    product,
    userUid,
    otherUserUid,
    getAnswers,
    getQuestions,
    getUsers,
};
