import {
    getSlices,
    getTopLevelItemsWithCurrent,
    getMobileTopLevelItemsWithCurrent,
    getTabsItems
} from '../../../../components/redux/store/selectors/navigation';

describe('navigation selectors', () => {
    let state;
    beforeEach(() => {
        state = {
            page: {
                idContext: '/disk'
            },
            resources: { },
            defaultFolders: {
                narod: false,
                yaru: null,
                folders: {
                    downloads: '/disk/Загрузки',
                    scans: '/disk/Сканы'
                }
            },
            environment: { session: { experiment: {} } },
            user: { }
        };
    });

    describe('getSlices', () => {
        it('У обычных пользователей не должно быть архивных разделов', () => {
            const slices = getSlices(state);

            expect(slices.find(({ id }) => id === 'narod')).toBeFalsy();
            expect(slices.find(({ id }) => id === 'yaru')).toBeFalsy();
            expect(slices.find(({ id }) => id === 'yafotki')).toBeFalsy();
            expect(slices.find(({ id }) => id === 'downloads')).not.toBeFalsy();
        });

        it('У пользователей с народа должен быть раздел "Народ"', () => {
            state.defaultFolders.narod = true;
            const slices = getSlices(state);
            expect(slices.find(({ id }) => id === 'narod')).not.toBeFalsy();
        });

        it('У пользователей с yaru должен быть раздел "Другие сервисы"', () => {
            state.defaultFolders.yaru = true;
            const slices = getSlices(state);
            expect(slices.find(({ id }) => id === 'yaru')).not.toBeFalsy();
        });

        it('У пользователя с Я.Фоток должен быть раздел "Яндекс.Фотки"', () => {
            state.user.states = { fotki_migration_state: 'done' };
            const slices = getSlices(state);
            expect(slices.find(({ id }) => id === 'yafotki')).not.toBeFalsy();
        });

        it('По умолчанию раздел "общий доступ" должен вести в "общие папки"', () => {
            const slices = getSlices(state);
            expect(slices.find(({ id }) => id === 'shared').idContext).toBe('/shared');
        });

        it('Если раздел "общие папки" пуст. то раздел общий доступ долже вести в "ссылки"', () => {
            state.resources['/shared'] = {
                isComplete: true,
                children: []
            };

            const slices = getSlices(state);
            expect(slices.find(({ id }) => id === 'shared').idContext).toBe('/published');
        });
    });

    describe('getTopLevelItemsWithCurrent', () => {
        it('Меню первого уровеня', () => {
            const items = getTopLevelItemsWithCurrent(state);
            expect(items.map(({ id }) => id)).toEqual([
                'recent',
                'files',
                'photo',
                'albums',
                'shared',
                'journal',
                'aux',
                'downloads',
                'scans',
                'trash'
            ]);
            expect(items[1].current).toBe(true);
        });

        it('Меню первого уровеня для мобильных', () => {
            const items = getMobileTopLevelItemsWithCurrent(state);
            expect(items.map(({ id }) => id)).toEqual([
                'recent',
                'files',
                'photo',
                'albums',
                'shared',
                'journal',
                'trash'
            ]);
        });

        it('Для подпапок в листинге должен быть активен раздел "файлы"', () => {
            state.page.idContext = '/disk/folder';
            const [, files] = getTopLevelItemsWithCurrent(state);
            expect(files.current).toBe(true);
        });

        it('Для подпапок c большой вложенностью в листинге должен быть активен раздел "файлы"', () => {
            state.page.idContext = '/disk/folder/subfolder/sub folder/sup/recent/folder';
            const [, files] = getTopLevelItemsWithCurrent(state);
            expect(files.current).toBe(true);
        });

        it('В разделе "Последние файлы" должен быть активным пункт меню "Последние"', () => {
            state.page.idContext = '/recent';
            const [recent] = getTopLevelItemsWithCurrent(state);
            expect(recent.current).toBe(true);
        });

        it('В разделе "все фото" должен быть активен пункт меню "Фото"', () => {
            state.page.idContext = '/photo';
            const [,, photo] = getTopLevelItemsWithCurrent(state);
            expect(photo.current).toBe(true);
        });

        it('В разделе "Альбомы" должен быть активен пункт меню "Альбомы"', () => {
            state.page.idContext = '/albums';
            const [,,, albums] = getTopLevelItemsWithCurrent(state);
            expect(albums.current).toBe(true);
        });

        it('В альбоме должен быть активен пункт меню "Альбомы"', () => {
            state.page.idContext = '/album/LongHash==';
            const [,,, albums] = getTopLevelItemsWithCurrent(state);
            expect(albums.current).toBe(true);
        });

        it('В Альбоме-срезе должен быть активен пункт меню "Альбомы"', () => {
            Object.assign(state.page, { idContext: '/photo', filter: 'beautiful' });
            const [,,, albums] = getTopLevelItemsWithCurrent(state);
            expect(albums.current).toBe(true);
        });

        it('В отфильтированном фотосрезе должен быть активен пункт меню "Фото"', () => {
            Object.assign(state.page, { idContext: '/photo', filter: 'photounlim' });
            const [,, photo] = getTopLevelItemsWithCurrent(state);
            expect(photo.current).toBe(true);
        });

        it('В разделе "Общие папки" должен быть активен пункт меню "Общий доступ"', () => {
            state.page.idContext = '/shared';
            const [,,,, shared] = getTopLevelItemsWithCurrent(state);
            expect(shared.current).toBe(true);
        });

        it('В разделе "Ссылки" должен быть активен пункт меню "Общий доступ"', () => {
            state.page.idContext = '/published';
            const [,,,, shared] = getTopLevelItemsWithCurrent(state);
            expect(shared.current).toBe(true);
        });

        it('В разделе "История" должен быть активен пункт меню "История"', () => {
            state.page.idContext = '/journal';
            const [,,,,, journal] = getTopLevelItemsWithCurrent(state);
            expect(journal.current).toBe(true);
        });

        it('В разделе "Почтовые вложения" должен быть активен пункт меню "Архив"', () => {
            state.page.idContext = '/attach';
            const [,,,,,, aux] = getTopLevelItemsWithCurrent(state);
            expect(aux.current).toBe(true);
        });

        it('В разделе "Народ" должен быть активен пункт меню "Архив"', () => {
            state.page.idContext = '/narod';
            state.defaultFolders.narod = true;
            const [,,,,,, aux] = getTopLevelItemsWithCurrent(state);
            expect(aux.current).toBe(true);
        });

        it('В разделе "Другие сервисы" должен быть активен пункт меню "Архив"', () => {
            state.page.idContext = '/attach/yaruarchive';
            state.defaultFolders.yaru = true;
            const [,,,,,, aux] = getTopLevelItemsWithCurrent(state);
            expect(aux.current).toBe(true);
        });

        it('В разделе "Яндекс.Фотки" должен быть активен пункт меню "Архив"', () => {
            state.page.idContext = '/attach/YaFotki';
            state.user.states = { fotki_migration_state: 'done' };
            const [,,,,,, aux] = getTopLevelItemsWithCurrent(state);
            expect(aux.current).toBe(true);
        });

        it('В разделе "Загрузки" должен быть активен пункт меню "Загрузки"', () => {
            state.page.idContext = '/disk/Загрузки';
            const [,,,,,,, downloads] = getTopLevelItemsWithCurrent(state);
            expect(downloads.current).toBe(true);
        });

        it('В разделе "Сканы" должен быть активен пункт меню "Сканы"', () => {
            state.page.idContext = '/disk/Сканы';
            const [,,,,,,,, scans] = getTopLevelItemsWithCurrent(state);
            expect(scans.current).toBe(true);
        });

        it('В разделе "Корзина" должен быть активен пункт меню "Корзина"', () => {
            state.page.idContext = '/trash';
            const [,,,,,,,,, trash] = getTopLevelItemsWithCurrent(state);
            expect(trash.current).toBe(true);
        });
    });

    describe('getTabsItems', () => {
        it('Меню второго уровня в разделе "Общие папки"', () => {
            state.page.idContext = '/shared';
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['shared_folders', 'published']);
            expect(current).toBe('shared_folders');
        });

        it('Меню второго уровня в разделе "Ссылки"', () => {
            state.page.idContext = '/published';
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['shared_folders', 'published']);
            expect(current).toBe('published');
        });

        it('Меню второго уровня в разделе "Почтовые вложения"', () => {
            state.page.idContext = '/attach';
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['attach']);
            expect(current).toBe('attach');
        });

        it('Меню второго уровня в разделе "Народ"', () => {
            state.page.idContext = '/narod';
            state.defaultFolders.narod = true;
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['attach', 'narod']);
            expect(current).toBe('narod');
        });

        it('Меню второго уровня в разделе "Другие сервисы"', () => {
            state.page.idContext = '/attach/yaruarchive';
            state.defaultFolders.yaru = true;
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['attach', 'yaru']);
            expect(current).toBe('yaru');
        });

        it('Меню второго уровня в разделе "Яндекс.Фотки"', () => {
            state.page.idContext = '/attach/YaFotki';
            state.user.states = { fotki_migration_state: 'done' };
            const { current, items } = getTabsItems(state);
            expect(items.map(({ id }) => id)).toEqual(['attach', 'yafotki']);
            expect(current).toBe('yafotki');
        });
    });
});
