import { LcJobsEntityListItem, LcJobsEntityListType } from '../LcJobsEntityList.types';
import { getGroupedItems, getFilterItems, getSortedItems, getSearchPlaceholder } from '../LcJobsEntityList.utils';

const namesOf = (items: LcJobsEntityListItem[]): string[] => items.map(({ name }) => name);
const fillupItems = (items: Partial<LcJobsEntityListItem>[]): LcJobsEntityListItem[] => items.map(item => ({
    slug: 'defaultSlug',
    name: 'defaultName',
    publicationsCount: 0,
    group: { title: 'default' },
    ...item,
}));

describe('LcJobsEntityList.utils', () => {
    describe('getGroupedItems (группировка)', () => {
        let itemsToGroup: LcJobsEntityListItem[];
        const subjectGroupsCount = 3;

        beforeEach(() => {
            itemsToGroup = fillupItems([
                { name: 'Что-то B', publicationsCount: 10, group: { title: 'Нероссия' } },
                { name: 'Что-то пустое', publicationsCount: 0, group: { title: 'Нероссия' } },
                { name: 'Нечто A', publicationsCount: 5, group: { title: 'Россия' } },
                { name: 'Нечто пустое', publicationsCount: 0, group: { title: 'Россия' } },
                { name: 'Картинка C', publicationsCount: 1, group: { title: 'Китая' } },
            ]);
        });

        it('должна группировать', () => {
            const result = getGroupedItems(itemsToGroup);

            expect(Object.keys(result)).toHaveLength(subjectGroupsCount);
            expect(result['Россия'].items).toHaveLength(2);
            expect(result['Нероссия'].items).toHaveLength(2);
            expect(result['Китая'].items).toHaveLength(1);
        });

        it.todo('Кол-во вакансий в группе после фильтрации должно меняться (?)');
        it.todo('Группы без элементов не должны отображаться (после фильтрации)');
    });

    describe('getSortedItems', () => {
        describe('Locations', () => {
            let itemsToSort: LcJobsEntityListItem[];

            beforeEach(() => {
                itemsToSort = fillupItems([
                    { name: 'Ранний 1', slug: '0', publicationsCount: 5000, group: { title: 'Вашингтония' } },
                    { name: 'Нечто A', slug: 'ruA', publicationsCount: 5, group: { title: 'Россия' } },
                    { name: 'Немосква', slug: 'saint-petersburg', publicationsCount: 50, group: { title: 'Россия' } },
                    { name: 'Что-то B', slug: 'B', publicationsCount: 10, group: { title: 'Индия' } },
                    { name: 'Удалёнка', slug: 'remote', publicationsCount: 30, group: { title: 'Россия' } },
                    { name: 'Нечто C', slug: 'ruC', publicationsCount: 40, group: { title: 'Россия' } },
                    { name: 'Москва', slug: 'moscow', publicationsCount: 100, group: { title: 'Россия' } },
                    { name: 'Картинка D', slug: 'D', publicationsCount: 1, group: { title: 'Кития' } },
                ]);
            });

            it('Москва, Петербург и удалёнка раньше всех', () => {
                const result = getSortedItems(itemsToSort).map(v => v.slug);

                expect(result.slice(0, 3)).toEqual(['moscow', 'saint-petersburg', 'remote']);
            });

            it('Россия должна быть первой', () => {
                const result = getSortedItems(itemsToSort);

                expect(result[0].group.title).toEqual('Россия');
            });

            it('Кол-во публикаций должно влиять на результат сортировки', () => {
                const result = getSortedItems(itemsToSort).slice(3).map(v => v.publicationsCount);

                expect(result).toEqual([5000, 40, 10, 5, 1]);
            });
        });

        describe('Services', () => {
            let itemsToSort: LcJobsEntityListItem[];

            beforeEach(() => {
                itemsToSort = fillupItems([
                    { name: 'AppMetrika', slug: 'appmetrika', publicationsCount: 2, group: { title: 'A' } },
                    { name: 'Yandex.Cloud', slug: 'yandexcloud', publicationsCount: 63, group: { title: 'Y' } },
                    { name: 'Авиабилеты', slug: 'aviasales', publicationsCount: 5, group: { title: 'А' } },
                    { name: 'Авто.ру', slug: 'autoru', publicationsCount: 13, group: { title: 'А' } },
                    { name: 'Афиша', slug: 'afisha', publicationsCount: 1, group: { title: 'А' } },
                    { name: 'Баннеры', slug: 'banners', publicationsCount: 2, group: { title: 'Б' } },
                    { name: 'Бизнес', slug: 'business', publicationsCount: 5, group: { title: 'Б' } },
                    { name: 'Браузер', slug: 'browser', publicationsCount: 9, group: { title: 'Б' } },
                    { name: 'Взгляд', slug: 'vzglyad', publicationsCount: 4, group: { title: 'В' } },
                    { name: 'Видео', slug: 'video', publicationsCount: 4, group: { title: 'В' } },
                    { name: 'Внутренние сервисы', slug: 'intranet', publicationsCount: 29, group: { title: 'В' } },
                ]);
            });

            it('services with latin name should be last if publications count equal', () => {
                const itemsWithEqPubCount = itemsToSort
                    .map(item => ({ ...item, publicationsCount: 10 }));

                const result = getSortedItems(itemsWithEqPubCount, { cyrillicNameFirst: true })
                    .slice(-2).map(v => v.group.title);

                expect(result).toEqual(['A', 'Y']);
            });

            it('should be ordered by publications count inside groups', () => {
                const result = getSortedItems(itemsToSort, { cyrillicNameFirst: true })
                    .map(v => `${v.group.title} ${v.publicationsCount}`);

                expect(result).toMatchSnapshot();
            });
        });

        describe('Professions', () => {
            let itemsToSort: LcJobsEntityListItem[];

            beforeEach(() => {
                itemsToSort = fillupItems([
                    { position: 1, name: 'Образовательные проекты', publicationsCount: 11, group: { title: 'Наука' } },
                    { position: 0, name: 'Аналитика', publicationsCount: 12, group: { title: 'Завод' } },
                    { position: 0, name: 'Информационная безопасность', publicationsCount: 13, group: { title: 'Завод' } },
                    { position: 0, name: 'Эксплуатация сервисов', publicationsCount: 14, group: { title: 'Завод' } },
                    { position: 0, name: 'Дизайн', publicationsCount: 15, group: { title: 'Завод' } },
                    { position: -1, name: 'Разработка', publicationsCount: 120, group: { title: 'Завод' } },
                    { position: 0, name: 'Проектирование обрудования', publicationsCount: 16, group: { title: 'Завод' } },
                    { position: 1, name: 'Наука', publicationsCount: 17, group: { title: 'Наука' } },
                    { position: 0, name: 'Сетевые технологии', publicationsCount: 18, group: { title: 'Завод' } },
                    { position: -1, name: 'Управление проектами и продуктами', publicationsCount: 19, group: { title: 'Завод' } },
                    { position: 0, name: 'Маркетинг и реклама', publicationsCount: 20, group: { title: 'Реклама' } },
                    { position: 0, name: 'PR, мероприятия, marcom', publicationsCount: 21, group: { title: 'Реклама' } },
                    { position: 0, name: 'Перфоманс и медиа-реклама', publicationsCount: 22, group: { title: 'Реклама' } },
                    { position: 0, name: 'Исследования и аналитика', publicationsCount: 23, group: { title: 'Реклама' } },
                    { position: -1, name: 'Тестирование', publicationsCount: 24, group: { title: 'Завод' } },
                    { position: 0, name: 'Разработка контента', publicationsCount: 25, group: { title: 'Реклама' } },
                    { position: 0, name: 'Развитие бизнеса и продажи', publicationsCount: 26, group: { title: 'Торговля' } },
                    { position: 0, name: 'Бизнес-анализ', publicationsCount: 27, group: { title: 'Торговля' } },
                ]);
            });

            it('should order items by position!', () => {
                const result = getSortedItems(itemsToSort);

                expect(result.map(v => v.position)).toMatchSnapshot();
            });

            it('should order by pubcount inside groups', () => {
                const result = getSortedItems(itemsToSort)
                    .filter(v => v.position === 0)
                    .map(v => v.publicationsCount);

                expect(result).toMatchSnapshot();
            });
        });
    });

    describe('getFilterItems (поиск-фильтрация)', () => {
        let itemsToFilter: LcJobsEntityListItem[];
        const emptyItemsCount = 2;
        const fullLatinItemsCount = 2;
        const mixedItemsCount = 1;

        beforeEach(() => {
            itemsToFilter = fillupItems([
                { name: 'Нечто A', publicationsCount: 5 },
                { name: 'Нечто пустое', publicationsCount: 0 },
                { name: 'Что-то B', publicationsCount: 10 },
                { name: 'Что-то пустое', publicationsCount: 0 },
                { name: 'Картинка C', publicationsCount: 1 },
                { name: 'Latin text D', publicationsCount: 1 },
                { name: 'Latin text E', publicationsCount: 1 },
                { name: 'Mixed название', publicationsCount: 2 },
            ]);
        });

        it('должна исключать элементы без вакансий при флажке onlyWithVacancies', () => {
            const result = namesOf(getFilterItems(itemsToFilter, '', true));

            expect(result).toHaveLength(itemsToFilter.length - emptyItemsCount);
            expect(result).not.toContain('Нечто пустое');
        });

        it('должна показывать все элементы без вакансий без флажка onlyWithVacancies', () => {
            const result = namesOf(getFilterItems(itemsToFilter, '', false));

            expect(result).toHaveLength(itemsToFilter.length);
            expect(result).toContain('Нечто пустое');
        });

        it('должна поддерживаться fuzzy-search', () => {
            const result = namesOf(getFilterItems(itemsToFilter, 'пст', false));

            expect(result).toHaveLength(emptyItemsCount);
            expect(result).toContain('Нечто пустое');
        });

        it('должна поддерживаться fuzzy-search с символами латиницы', () => {
            const result = namesOf(getFilterItems(itemsToFilter, 'ltext', false));

            expect(result).toHaveLength(fullLatinItemsCount);
            expect(result).toContain('Latin text D');
        });

        it('должна поддерживаться fuzzy-search с символами латиницы и кириллицы одновременно', () => {
            const result = namesOf(getFilterItems(itemsToFilter, 'xedвание', false));

            expect(result).toHaveLength(mixedItemsCount);
            expect(result).toContain('Mixed название');
        });

        it('должна игнорировать точки-пробелы при fuzzy-search', () => {
            const result = namesOf(getFilterItems(itemsToFilter, '.п.с- т', false));

            expect(result).toHaveLength(emptyItemsCount);
            expect(result).toContain('Нечто пустое');
        });
    });

    describe('getSearchPlaceholder', () => {
        it('должна работать как ожидается', () => {
            expect(getSearchPlaceholder(LcJobsEntityListType.Locations)).toContain('город');
            expect(getSearchPlaceholder(LcJobsEntityListType.Professions)).toContain('професс');
            expect(getSearchPlaceholder(LcJobsEntityListType.Services)).toContain('сервис');
        });
    });
});
