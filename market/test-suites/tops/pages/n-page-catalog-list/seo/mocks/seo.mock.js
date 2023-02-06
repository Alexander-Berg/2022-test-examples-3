import {createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

const createProductMock = (productId, type, title) => createProduct(
    {
        type,
        titles: {
            raw: title,
            highlighted: [
                // особенность работы refaker
                // @see https://st.yandex-team.ru/MARKETFRONTECH-1027
                {value: title, highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
                {value: '', highlight: false},
            ],
        },
        lingua: {
            type: {nominative: '', genitive: '', dative: '', accusative: ''},
        },
    },
    productId
);

export const product = {
    mock: createProductMock(1722193751, 'model', 'Смартфон Samsung Galaxy S8'),
    id: 1722193751,
};
