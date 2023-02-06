export const MAIN_PAGE_PATH = '/entity/qualityManagement@1';
export const CREATION_PAGE_PATH = '/entity/qualityManagementSelection/qualityManagementSelection/create';
export const STATUSES = [
    {
        description: 'Статус "Обрабатывается"',
        url: '/entity/qualityManagementSelection@129759985',
        expectedStatusText: 'Обрабатывается',
    },
    {
        description: 'Статус "Сформирована"',
        url: '/entity/qualityManagementSelection@172379784',
        expectedStatusText: 'Сформирована',
    },
    {
        description: 'Статус "Ошибка"',
        url: '/entity/qualityManagementSelection@132067894',
        expectedStatusText: 'Ошибка',
    },
    {
        description: 'Статус "Оценка завершена"',
        url: '/entity/qualityManagementSelection@125202884',
        expectedStatusText: 'Оценка завершена',
    },
    {
        description: 'Статус "Ошибка публикации результата"',
        url: '/entity/qualityManagementSelection@130676189',
        expectedStatusText: 'Ошибка публикации результата',
    },
    {
        description: 'Статус "Результат опубликован"',
        url: '/entity/qualityManagementSelection@141457184',
        expectedStatusText: 'Результат опубликован',
    },
];
