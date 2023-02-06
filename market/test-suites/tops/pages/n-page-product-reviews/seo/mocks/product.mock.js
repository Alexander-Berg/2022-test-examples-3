import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';

export const productId = '12345';
export const slug = 'sime-slug';

export const guruMock = stateProductWithDO(productId, {
    type: 'model',
    titles: {
        raw: 'Смартфон Apple iPhone 7 256GB',
    },
});

const bookMock = stateProductWithDO(productId, {
    type: 'book',
    titles: {
        raw: 'Овидий "Искусство любви (подарочное издание)"',
    },
});

const groupMock = stateProductWithDO(productId, {
    type: 'group',
    titles: {
        raw: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
    },
});

const clusterMock = stateProductWithDO(productId, {
    type: 'cluster',
    titles: {
        raw: 'Платье Selia',
    },
});


export const productSeoMock = {
    guru: {
        description: 'Тип "Гуру"',
        ...guruMock,
    },
    book: {
        description: 'Тип "Книга"',
        ...bookMock,
    },
    group: {
        description: 'Тип "Групповая"',
        ...groupMock,
    },
    cluster: {
        description: 'Тип "Кластер"',
        ...clusterMock,
    },
};
