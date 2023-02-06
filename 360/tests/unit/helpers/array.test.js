import { getSiblingsOf } from '../../../components/helpers/array';

describe('arrayHelper', () => {
    describe('getSiblingsOf', () => {
        const resources = [
            { id: '/disk/folder/1' },
            { id: '/disk/folder/2' },
            { id: '/disk/folder/3' }
        ];

        const matcher = (of, item) => of === item.id;

        it('вызванный без `options` для не последнего элемента, должен вернуть следующий', () => {
            expect(getSiblingsOf(resources, '/disk/folder/2', { matcher })).toEqual([{ id: '/disk/folder/3' }]);
        });

        it('вызванный без `options` для последнего эдемента, должен вернуть пустой массив', () => {
            expect(getSiblingsOf(resources, '/disk/folder/3', { matcher })).toEqual([]);
        });

        it('вызванный с `options.take` = n, должен вернуть n или меньше следующих элементов', () => {
            expect(getSiblingsOf(resources, '/disk/folder/1', { matcher, take: 2 })).toEqual([
                { id: '/disk/folder/2' },
                { id: '/disk/folder/3' }
            ]);
        });

        it('вызванный с `options.take` = -n, должен вернуть n или меньше предыдущих элементов', () => {
            expect(getSiblingsOf(resources, '/disk/folder/3', { matcher, take: -2 })).toEqual([
                { id: '/disk/folder/2' },
                { id: '/disk/folder/1' }
            ]);
        });

        it('вызванный с `options.loop` для последнего элемента должен вернуть первый элемент', () => {
            expect(getSiblingsOf(resources, '/disk/folder/3', { matcher, loop: true })).toEqual([{ id: '/disk/folder/1' }]);
        });

        it('вызванный с `options.filter` должен игнорировать отфильтрованные элементы', () => {
            expect(getSiblingsOf(resources, '/disk/folder/1', {
                matcher,
                filter: (({ id }) => id !== '/disk/folder/2')
            })).toEqual([{ id: '/disk/folder/3' }]);
        });

        it('если в коллекции всего 1 элемент, то метод должен вернуть пустой массив', () => {
            expect(getSiblingsOf([{ id: '/disk/folder/1' }], '/disk/folder/1', { matcher })).toEqual([]);
        });

        it('если ищем соседей для элемента не принадлежащего данной коллекции ', () => {
            expect(getSiblingsOf(resources, '/disk/folder/4', { matcher })).toEqual([]);
        });
    });
});
