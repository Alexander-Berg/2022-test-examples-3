import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const productId = 1722193751;
const productSlug = 'smartfon-samsung-galaxy-s8';
export const titles = {
    raw: 'Смартфон Samsung Galaxy S8',
    highlighted: [{
        value: 'Смартфон',
        highlight: true,
    }, {
        value: ' ',
    }, {
        value: 'Samsung',
        highlight: true,
    }, {
        value: ' Galaxy S8',
    }],
};

const categories = [{
    entity: 'category',
    id: 91491,
    name: 'Мобильные телефоны',
    fullName: 'Мобильные телефоны',
    type: 'guru',
    isLeaf: true,
    slug: 'mobilnye-telefony',
}];

export const reportState = createProduct({
    slug: productSlug,
    titles,
    categories,
}, productId);
