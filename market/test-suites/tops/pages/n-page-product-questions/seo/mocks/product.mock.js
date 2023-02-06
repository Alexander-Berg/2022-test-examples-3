import {stateProductWithDO} from '@self/platform/spec/hermione/configs/seo/mocks';

export const productId = '12345';
export const slug = 'some-slug';

const guruMock = stateProductWithDO('12345', {
    type: 'model',
    titles: {
        raw: 'Смартфон Apple iPhone 7 256GB',
    },
});

const groupMock = stateProductWithDO('12345', {
    type: 'group',
    titles: {
        raw: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
    },
});

export default {
    guruMock: {
        description: 'Тип "Гуру"',
        ...guruMock,
    },
    groupMock: {
        description: 'Тип "Групповая"',
        ...groupMock,
    },
};
