import { mount } from 'enzyme';
import React from 'react';
import SquareWithClusters from '../../../../../lib/components/virtual-grid/grids/square-with-clusters';
import VirtualGrid from '../../../../../lib/components/virtual-grid';
import { clustersWithItems } from '../../../fixtures/clusters';

describe('SquareWithClusters', () => {
    let grid;
    const isGridCompleteSpy = jest.spyOn(SquareWithClusters.prototype, 'isGridComplete');
    const onScrollLimitReached = jest.fn();

    const renderItem = (props, i) => {
        const style = {
            top: props.top + 'px',
            left: props.left + 'px',
            width: props.width + 'px',
            height: props.height + 'px'
        };

        return (<div key={i} data-type={`${props.type}`} style={style}>{props.type.toUpperCase()}</div>);
    };

    const renderGroup = (props) => {
        const { geometry } = props;
        return (<div
            key={`${props.start.clusterIndex}-${props.start.resourceIndex}`}
            style={{
                top: geometry.top + 'px',
                height: geometry.height + 'px'
            }}
        >
            {props.items.map((item, i) => renderItem(item, i))}
        </div>);
    };

    jest.spyOn(VirtualGrid.prototype, '_gridContainerRef').mockImplementation(() => null);

    VirtualGrid.prototype._gridContainer = {
        getBoundingClientRect: () => ({ top: 0, left: 0, width: 1500 })
    };

    const gridProps = {
        GridType: SquareWithClusters,
        gridOptions: {
            titleHeight: 60,
            itemSize: 220,
            smallItemSize: 150,
            itemsMargin: 4,
            renderGroups: 3,
            itemsInGroup: 40,
            scrollSpeedThreshold: 5
        },
        onScrollLimitReached,
        renderGroup,
        items: clustersWithItems,
        structureVersion: 0
    };
    beforeEach(() => {
        grid = mount(<VirtualGrid {...gridProps} />);
    });

    afterAll(() => {
        jest.clearAllMocks();
    });

    it('should call _checkIfScrolledToTheEnd if the grid is not complete', () => {
        isGridCompleteSpy.mockImplementationOnce(() => false);
        const checkIfScrolledToTheEndSpy = jest.spyOn(grid.instance(), '_checkIfScrolledToTheEnd');

        grid.instance()._onWindowScroll({ scrollTop: global.pageYOffset });

        expect(checkIfScrolledToTheEndSpy).toHaveBeenCalledWith(global.pageYOffset, global.innerHeight);
    });

    it('should call onScrollLimitReached if the grid is not complete and page is scrolled to the end', () => {
        const pageYOffsetBackup = global.pageYOffset;
        global.pageYOffset = grid.instance()._grid.getHeight() - global.innerHeight;
        isGridCompleteSpy.mockImplementationOnce(() => false);

        grid.instance()._onWindowScroll({ scrollTop: global.pageYOffset });

        expect(onScrollLimitReached).toHaveBeenCalled();
        global.pageYOffset = pageYOffsetBackup;
    });

    it('_findVisibleItems', () => {
        const visibleItems = grid.instance()._findVisibleItems();

        expect(visibleItems.clusters.length).toBe(3);
        expect(visibleItems.items.length).toBe(10);
    });

    it('Не должен вызывать повторный рендер при скроле если видимые группы не поменялись', () => {
        const setStateSpy = jest.spyOn(grid.instance(), 'setState');

        window.pageYOffset = 5000;
        grid.instance()._onWindowScroll({ scrollTop: window.pageYOffset });
        expect(setStateSpy).toHaveBeenCalledTimes(1);
        window.pageYOffset = 5020;
        grid.instance()._onWindowScroll({ scrollTop: window.pageYOffset });
        expect(setStateSpy).toHaveBeenCalledTimes(1);
    });

    it('Не должен разваливаться от первого ребилда если скролл не равен 0', () => {
        window.innerWidtht = 1920;
        window.innerHeight = 1080;

        window.pageYOffset = 1;

        grid.unmount();
        const newGridProps = Object.assign({}, gridProps, {
            items: clustersWithItems.slice(0, 2),
            structureVersion: 2,
        });

        grid = mount(<VirtualGrid {...newGridProps} />);

        const rebuildSpy = jest.spyOn(grid.instance(), '_rebuild');

        grid.setProps({
            items: clustersWithItems.slice(0, 1),
            structureVersion: 3,
            rebuildOnPropsChange: ['structureVersion']
        });

        expect(rebuildSpy).toHaveBeenCalled();
    });
});
