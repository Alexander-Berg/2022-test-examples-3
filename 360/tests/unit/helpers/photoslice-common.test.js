import { getFilteredClusterData } from '../../../components/helpers/photoslice-common';

describe('getFilteredClusterData', () => {
    it('Должен фильтровать items и счтьать size', () => {
        const { items, size, albums } = getFilteredClusterData(
            [
                { id: '/disk/1.jpg', itemId: 'item1' },
                { id: '/disk/2.jpg', itemId: 'item2' },
                { id: '/disk/3.jpg', itemId: 'item3' },
                { id: '/disk/4.jpg', itemId: 'item4' }
            ],
            (id) => ['/disk/2.jpg', '/disk/4.jpg'].includes(id)
        );

        expect(items).toEqual([
            { id: '/disk/2.jpg', itemId: 'item2' },
            { id: '/disk/4.jpg', itemId: 'item4' }
        ]);
        expect(size).toBe(2);
        expect(albums).toBe(null);
    });

    it('Должен пересчитывать albums', () => {
        const { items, size, albums } = getFilteredClusterData(
            [
                { id: '/disk/1.jpg', itemId: 'item1', albums: ['beautiful'] },
                { id: '/disk/2.jpg', itemId: 'item2', albums: [] },
                { id: '/disk/3.jpg', itemId: 'item3' },
                { id: '/disk/4.jpg', itemId: 'item4', albums: ['unbeautiful', 'screenshots'] },
                { id: '/disk/5.jpg', itemId: 'item5', albums: ['unbeautiful'] },
                { id: '/disk/6.jpg', itemId: 'item6', albums: ['beautiful'] },
            ],
            (id) => !['/disk/5.jpg', '/disk/2.jpg'].includes(id)
        );

        expect(items).toEqual([
            { id: '/disk/1.jpg', itemId: 'item1', albums: ['beautiful'] },
            { id: '/disk/3.jpg', itemId: 'item3' },
            { id: '/disk/4.jpg', itemId: 'item4', albums: ['unbeautiful', 'screenshots'] },
            { id: '/disk/6.jpg', itemId: 'item6', albums: ['beautiful'] },
        ]);
        expect(size).toBe(4);
        expect(albums).toEqual({
            beautiful: 2,
            unbeautiful: 1,
            screenshots: 1
        });
    });

    it('Должен корректно пересчитывать albums если все элементы с автоальбомами отфильтровались', () => {
        const { items, size, albums } = getFilteredClusterData(
            [
                { id: '/disk/1.jpg', itemId: 'item1', albums: ['beautiful'] },
                { id: '/disk/2.jpg', itemId: 'item2', albums: [] },
                { id: '/disk/3.jpg', itemId: 'item3' },
                { id: '/disk/4.jpg', itemId: 'item4', albums: ['unbeautiful', 'screenshots'] },
                { id: '/disk/5.jpg', itemId: 'item5', albums: ['unbeautiful'] },
                { id: '/disk/6.jpg', itemId: 'item6', albums: ['beautiful'] },
            ],
            (id) => ['/disk/2.jpg', '/disk/3.jpg'].includes(id)
        );

        expect(items).toEqual([
            { id: '/disk/2.jpg', itemId: 'item2', albums: [] },
            { id: '/disk/3.jpg', itemId: 'item3' },
        ]);
        expect(size).toBe(2);
        expect(albums).toBe(null);
    });
});
