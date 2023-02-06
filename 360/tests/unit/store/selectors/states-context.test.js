import { getSort } from '../../../../components/redux/store/selectors/states-context';

describe('states-context selectors -->', () => {
    describe('getSort', () => {
        const getState = (idContext) => ({
            page: {
                idContext: idContext || '/disk'
            },
            statesContext: {
                sort: {
                    '/disk': {
                        sort: 'mtime',
                        order: '0'
                    }
                }
            },
            defaultFolders: { folders: {} },
            settings: {}
        });

        it('Должен вернуть сортировку из `store` если она там есть', () => {
            expect(getSort(getState())).toEqual({
                sort: 'mtime',
                order: '0'
            });
        });

        it('Должен вернуть дефолтную сортировку для папки если для текущего раздела её нет в `store`', () => {
            expect(getSort(getState('/disk/trusiki'))).toEqual({
                sort: 'name',
                order: '1'
            });
        });

        it('Должен вернуть дефолтную сортировку для ПФ если для текущего раздела её нет в `store`', () => {
            expect(getSort(getState('/recent'))).toEqual({
                sort: 'mtime',
                order: '0'
            });
        });
    });
});
