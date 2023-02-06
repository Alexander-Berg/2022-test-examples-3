import SquareWithClusters from '../../../../../../lib/components/virtual-grid/grids/square-with-clusters';
import clusters from '../../../../fixtures/clusters';

describe('SquareWithClusters', () => {
    let grid;
    const gridOptions = {
        containerWidth: 1500,
        viewportHeight: 900,
        items: clusters,
        titleHeight: 64,
        itemSize: 220,
        smallItemSize: 150,
        itemsMargin: 4,
        renderGroups: 3,
        itemsInGroup: 50,
        scrollSpeedThreshold: 20
    };

    beforeEach(() => {
        grid = new SquareWithClusters(gridOptions);
    });

    it('Должна корректно расчитывать высоту', () => {
        expect(grid.getHeight()).toBe(15186);
    });

    it('Должна перерасчитывать высоту при вызове rebuild', () => {
        grid.rebuild({ containerWidth: 1000 });
        expect(grid.getHeight()).toBe(17360);
    });

    it('Каждая следующая группа должна должна начинаться в конце предыдущей', () => {
        const groups = grid.getGroups();
        for (let i = 1; i < groups.length; i++) {
            expect(groups[i].top).toBe(groups[i - 1].top + groups[i - 1].height);
        }
    });

    it('Высота группы должна быть не меньше высоты вьюпорта', () => {
        grid.rebuild({ viewportHeight: 2500 });
        const groups = grid.getGroups().slice(0, -1);
        expect(groups.every((group) => group.height >= 2500)).toBe(true);
    });

    it('Сумарная высота всех груп должна быть равна высоте всей сетки', () => {
        const totalHeight = grid.getGroups().reduce((result, { height }) => result + height, 0);
        expect(Math.ceil(totalHeight)).toBe(grid.getHeight());
    });

    it('Для скрола в начале страницы должна вызвращать первые две группы', () => {
        expect(grid.getGroupsToRender(0)).toEqual({ start: 0, end: 1 });
    });

    it('Для скрола в конце страницы должна вызвращать последние две группы', () => {
        expect(grid.getGroupsToRender(14303)).toEqual({ start: 4, end: 5 });
    });

    it('Для скрола в середине страницы должна вызвращать три средние группы', () => {
        expect(grid.getGroupsToRender(7601)).toEqual({ start: 2, end: 4 });
    });

    it('Координаты элементов внутри группы должны быть относительными', () => {
        const groups = grid.getGroups();
        const items = groups[0].items;
        expect(items[0].top).toBe(0);
        expect(items[0].left).toBe(0);
    });

    it('На узких экранах всегда должно отображаться 4 элемента', () => {
        grid.rebuild({ containerWidth: 400 });
        const groups = grid.getGroups();
        const items = groups[0].items;
        let photoItems = [];

        // любые 4 подряд идущие фотографии после заголовка кластер
        items.forEach((item) => {
            if (photoItems.length < 5) {
                if (item.type === 'item') {
                    photoItems.push(item);
                } else {
                    photoItems = [];
                }
            }
        });

        const top = photoItems[0].top;
        expect(photoItems[1].top).toBe(top);
        expect(photoItems[2].top).toBe(top);
        expect(photoItems[3].top).toBe(top);
        expect(photoItems[4].top).not.toBe(top);
    });

    it('Если в опциях есть maxHeight и hardHeightLimit, то высота сетки не должна превышать maxHeight', () => {
        [1000, 3000, 5000, 10000, 15000].forEach((maxHeight) => {
            const gridWithHeightLimit = new SquareWithClusters(Object.assign({}, gridOptions, {
                hardHeightLimit: true,
                maxHeight
            }));

            const lastGroupIndex = gridWithHeightLimit._groups.length - 1;
            const lastItemInLastGroupInndex = gridWithHeightLimit._groups[lastGroupIndex].items.length - 1;
            const lastItem = gridWithHeightLimit._groups[lastGroupIndex].items[lastItemInLastGroupInndex];

            expect(lastItem.absoluteTop < maxHeight).toBe(true);
            expect(Math.abs(gridWithHeightLimit._totalHeight - maxHeight) < lastItem.height).toBe(true);
        });
    });

    it('Если сетка построена не до конца, то isGridComplete должен возвращать false', () => {
        const gridWithHeightLimit = new SquareWithClusters(Object.assign({}, gridOptions, {
            hardHeightLimit: true,
            maxHeight: 1000
        }));

        expect(gridWithHeightLimit.isGridComplete()).toBe(false);
    });

    it('С опцией noTitles кластер должен начинаться в середине строки', () => {
        const grid = new SquareWithClusters(Object.assign({}, gridOptions, { noTitles: true }));
        const { items } = grid.getGroups()[0];
        for (let i = 0; i < 7; i++) {
            expect(items[i].top).toBe(0);
        }
        for (let i = 7; i < 14; i++) {
            expect(items[i].top).toBe(214);
        }
    });
});
