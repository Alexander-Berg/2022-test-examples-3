exports.sampleTest = {
    title: 'параметр sku находится в URL страницы',
    parent: {
        title: 'По умолчанию',
        parent: {
            title: 'Проверка url на наличие sku в query параметрах.',
            parent: {
                title: 'Отзывы.',
                parent: {
                    title: 'Табы.',
                    parent: {
                        title: 'Карточка модели с параметром sku.',
                        parent: {
                            title: 'Морда карточки модели.'
                        }
                    }
                }
            }
        }
    }
};

exports.sampleH2test = {
    fn: {
        __isWrapper: true,
        __meta: {
            id: 'm-touch-3820',
            issue: 'MARKETFRONT-64247',
            environment: 'kadavr',
            suiteName: 'Ссылка "Искать везде" и плитки категорий.',
            defaultParams: { text: 'Мобильные телефоны', nid: 54421, slug: 'detskie-tovary' },
            feature: 'Фильтры'
        },
        __test: {
            __meta: {
                id: 'm-touch-3820',
                issue: 'MARKETFRONT-64247',
                environment: 'kadavr'
            },
            __title: 'По поисковому запросу "Мобильные телефоны" ' +
                'На категорийной выдаче ссылка "Искать везде" отображается'
        },
        __title: 'Страница поисковой выдачи. Взаимодействие с фильтрами. Ссылка "Искать везде" и плитки категорий. ' +
            'По поисковому запросу "Мобильные телефоны" На категорийной выдаче ссылка "Искать везде" отображается'
    }
}

