import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';

export const productId = '12345';
export const slug = '12345-12345';

const guruMock = stateProductWithDO('12345', {
    type: 'model',
    titles: {
        raw: 'Смартфон Apple iPhone 7 256GB',
    },
});

const bookMock = stateProductWithDO('12345', {
    type: 'book',
    titles: {
        raw: 'Овидий "Искусство любви (подарочное издание)"',
    },
});

const groupMock = stateProductWithDO('12345', {
    type: 'group',
    titles: {
        raw: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
    },
});

const clusterMock = stateProductWithDO('12345', {
    type: 'cluster',
    titles: {
        raw: 'Платье Selia',
    },
});

export default {
    guruMock: {
        description: 'Тип "Гуру"',
        type: 'model',
        ...guruMock,
    },
    bookMock: {
        description: 'Тип "Книга"',
        type: 'book',
        ...bookMock,
    },
    groupMock: {
        description: 'Тип "Групповая"',
        type: 'group',
        ...groupMock,
    },
    clusterMock: {
        description: 'Тип "Кластер"',
        type: 'cluster',
        ...clusterMock,
    },
};
