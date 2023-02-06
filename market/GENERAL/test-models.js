export default {
    // сгруппированные по типам карточки для однотипных тестов
    groupedByType: {
        guruCard: {
            // Модель из категории гуру, на которой проверяется наличие ссылки на юр лицо
            description: 'Тип "Гуру"',
            productId: 1721714804,
            slug: 'planshet-apple-ipad-32gb-wi-fi',
        },
        groupCard: {
            // Модель из категории группа, на которой проверяется наличие ссылки на юр лицо
            description: 'Тип "Групповая"',
            productId: 1712127122,
            slug: 'noutbuk-lenovo-ideapad-310-15-amd',
        },
        clusterCard: {
            // Модель из категории кластер, на которой проверяется наличие ссылки на юр лицо
            description: 'Тип "Кластер"',
            productId: 32392221,
            slug: 'product-slug',
        },
        bookCard: {
            // Модель из категории книги, на которой проверяется наличие ссылки на юр лицо
            description: 'Тип "Книжки"',
            productId: 1159973,
            slug: 'ovidii-iskusstvo-liubvi-podarochnoe-izdanie',
        },
    },
    // сгруппированные по типам карточки для однотипных тестов
    groupedByTypeNotInStock: {
        guruCard: {
            // Модель из категории гуру не в продаже, на которой проверяется отсутствие ссылки на юр лицо
            description: 'Тип "Гуру"',
            productId: 7697429,
            slug: 'planshet-acer-iconia-tab-w500p-dock',
        },
        groupCard: {
            // Модель из категории группа не в продаже, на которой проверяется отсутствие ссылки на юр лицо
            description: 'Тип "Групповая"',
            productId: 6072338,
            slug: 'karta-pamiati-transcend-ts8gsdhc2-p2',
        },
        bookCard: {
            // Модель из категории книги не в продаже, на которой проверяется отсутствие ссылки на юр лицо
            description: 'Тип "Книжки"',
            productId: 1655184,
            slug: 'some-slug',
        },
    },
};
