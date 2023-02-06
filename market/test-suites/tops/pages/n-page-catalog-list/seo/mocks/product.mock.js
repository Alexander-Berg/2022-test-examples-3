const guruMock = {
    description: 'Тип "Гуру"',
    mock: {
        id: '12345',
        navnodes: [{
            entity: 'navnode',
            id: 54726,
            name: 'mockedCategoryName',
            fullName: 'mockedCategoryFullName',
        }],
        vendor: {
            entity: 'vendor',
            id: 153043,
            name: 'mockedVendorName',
            slug: 'mockedVendorName',
        },
        titles: {
            raw: 'Смартфон Apple iPhone 7 256GB',
            highlighted: [{
                value: 'Смартфон Apple iPhone 7 256GB',
            }],
        },
        ratingCount: 3,
        rating: 5,
        opinions: 3,
        slug: 'smartfon-apple-iphone-7-256gb',
        categories: [
            {
                entity: 'category',
                id: 91491,
                name: 'Мобильные телефоны',
                slug: 'mocked-category-slug',
                fullName: 'Мобильные телефоны',
                type: 'guru',
                isLeaf: true,
            },
        ],
        prices: {
            min: '13990',
            max: '13990',
            currency: 'RUR',
            avg: '13990',
        },
        offers: {
            count: 64,
        },
    },
};

const bookMock = {
    description: 'Тип "Книга"',
    mock: {
        id: '12345',
        type: 'book',
        titles: {
            raw: 'Овидий "Искусство любви (подарочное издание)"',
            highlighted: [
                {
                    value: 'Овидий "Искусство любви (подарочное издание)"',
                },
            ],
        },
        vendor: {
            entity: 'vendor',
            id: 153043,
            name: 'mockedVendorName',
            slug: 'mockedVendorName',
        },
        ratingCount: 3,
        rating: 5,
        opinions: 3,
        slug: 'ovidii-iskusstvo-liubvi-podarochnoe-izdanie',
        categories: [
            {
                entity: 'category',
                id: 90865,
                name: 'mockedCategoryName',
                slug: 'mocked-category-slug',
                fullName: 'mockedCategoryFullName',
            },
        ],
        prices: {
            min: '13990',
            max: '13990',
            currency: 'RUR',
            avg: '13990',
        },
        offers: {
            count: 64,
        },
    },
};

const groupMock = {
    description: 'Тип "Групповая"',
    mock: {
        id: '12345',
        type: 'group',
        titles: {
            raw: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
            highlighted: [
                {
                    value: 'Ноутбук Lenovo IdeaPad 310 15 AMD',
                },
            ],
        },
        vendor: {
            entity: 'vendor',
            id: 153043,
            name: 'mockedVendorName',
            slug: 'mockedVendorName',
        },
        ratingCount: 3,
        rating: 5,
        opinions: 3,
        slug: 'noutbuk-lenovo-ideapad-310-15-amd',
        categories: [
            {
                entity: 'category',
                id: 91013,
                name: 'mockedCategoryName',
                slug: 'mocked-category-slug',
                fullName: 'mockedCategoryFullName',
            },
        ],
        prices: {
            min: '13990',
            max: '13990',
            currency: 'RUR',
            avg: '13990',
        },
        offers: {
            count: 64,
        },
    },
};

const clusterMock = {
    description: 'Тип "Кластер"',
    mock: {
        id: '12345',
        type: 'cluster',
        titles: {
            raw: 'Платье Selia',
            highlighted: [
                {
                    value: 'Платье Selia',
                },
            ],
        },
        vendor: {
            entity: 'vendor',
            id: 153043,
            name: 'mockedVendorName',
            slug: 'mockedVendorName',
        },
        ratingCount: 3,
        rating: 5,
        opinions: 3,
        slug: 'plate-selia',
        categories: [
            {
                entity: 'category',
                id: 7811901,
                name: 'mockedCategoryName',
                slug: 'mocked-category-slug',
                fullName: 'mockedCategoryFullName',
            },
        ],
        prices: {
            min: '13990',
            max: '13990',
            currency: 'RUR',
            avg: '13990',
        },
        offers: {
            count: 64,
        },
    },
};

export default {
    guruMock,
    bookMock,
    groupMock,
    clusterMock,
};
