import * as React from 'react';
import { fireEvent, render, RenderResult } from '@testing-library/react';

import { VirtualList } from '..';
import { createMockRef } from './utils';

const ROW_HEIGHT = 50;
const ROWS_AHEAD = 4;
const VIEWPORT_HEIGHT = 400;

function getKey(value: number) {
    return `item${value}`;
}

function getRowHeight() {
    return ROW_HEIGHT;
}

function getItems(length: number) {
    return [...new Array(length).keys()];
}

const ListRow: React.FC<{ value: string }> = (props) => (
    <div
        className="ui-list-row"
        style={{ height: ROW_HEIGHT }}
    >
        {props.value}
    </div>
);

function renderRow(value: string) {
    return (
        <ListRow value={value} />
    );
}

function mountList(
    props: React.ComponentProps<typeof VirtualList>,
    viewportHeight: number,
    wrapper?: RenderResult,
    virtualListRef?: React.RefObject<VirtualList>,
) {
    class MockedVirtualList extends VirtualList {
        public listRef = createMockRef<HTMLDivElement>({
            clientHeight: viewportHeight,
            scrollHeight: props.items.length * ROW_HEIGHT,
        });
        public containerRef = createMockRef<HTMLDivElement>({
            clientHeight: viewportHeight,
        });
        public headerRef = createMockRef<HTMLDivElement>({
            clientHeight: 0,
        });
        public footerRef = createMockRef<HTMLDivElement>({
            clientHeight: 0,
        });
    }

    if (wrapper) {
        wrapper.rerender(
            <MockedVirtualList {...props} ref={virtualListRef} />
        );

        return wrapper;
    }

    return render(
        // @ts-ignore
        <MockedVirtualList {...props} ref={virtualListRef} />,
    );
}

const getSpacerStyles = (wrapper: RenderResult) =>
    wrapper.baseElement.querySelector('.ui-virtual-list__spacer')?.getAttribute('style');

describe('VirtualList', () => {
    let rafMock: jest.SpyInstance;

    beforeEach(() => {
        rafMock = jest.spyOn(window, 'requestAnimationFrame')
            .mockImplementation((callback) => {
                callback(0);

                return 0;
            });
    });

    afterEach(() => {
        rafMock.mockRestore();
    });

    it('should render correctly', () => {
        const items = getItems(50);

        const wrapper = mountList({
            renderRow,
            getKey,
            getRowHeight,
            items,
        }, VIEWPORT_HEIGHT);

        const lastRenderedRowIndex = VIEWPORT_HEIGHT / ROW_HEIGHT + ROWS_AHEAD - 1;

        const rows = wrapper.baseElement.querySelectorAll('.ui-list-row');

        expect(rows[0].textContent).toEqual('0');
        expect(rows[lastRenderedRowIndex].textContent).toEqual(`${lastRenderedRowIndex}`);
    });

    it('calls render twice on render if the list is empty', () => {
        const renderSpy = jest.spyOn(VirtualList.prototype, 'render');

        mountList({
            renderRow,
            getKey,
            getRowHeight,
            items: [],
        }, VIEWPORT_HEIGHT);

        expect(renderSpy).toBeCalledTimes(2);

        renderSpy.mockRestore();
    });

    it('calls render twice on render if the list is not empty', () => {
        const renderSpy = jest.spyOn(VirtualList.prototype, 'render');

        const items = getItems(10);

        mountList({
            renderRow,
            getKey,
            getRowHeight,
            items,
        }, VIEWPORT_HEIGHT);

        expect(renderSpy).toBeCalledTimes(2);

        renderSpy.mockRestore();
    });

    it('moves scrollbar when list is scrolled', () => {
        const virtualListRef = React.createRef<VirtualList>();
        const wrapper = mountList({
            renderRow,
            getKey,
            getRowHeight,
            items: [],
        }, VIEWPORT_HEIGHT, undefined, virtualListRef);

        const listRef = wrapper.getByTestId('listRef');
        const adjustScrollbarSpy = jest.spyOn(virtualListRef.current?.scrollbarRef.current!, 'adjust');

        fireEvent.scroll(listRef);

        expect(adjustScrollbarSpy).toBeCalled();
        adjustScrollbarSpy.mockRestore();
    });

    it('sets "scrollTop" on the list when dragging scrollbar', () => {
        const virtualListRef = React.createRef<VirtualList>();
        const wrapper = mountList({
            renderRow,
            getKey,
            getRowHeight,
            items: [],
        }, VIEWPORT_HEIGHT, undefined, virtualListRef);

        const listRef = wrapper.getByTestId('listRef');

        const requestAnimationFrameSpy = jest.spyOn(window, 'requestAnimationFrame');
        const scrollTopSpy = jest.spyOn(listRef, 'scrollTop', 'set');

        virtualListRef.current?.scrollbarRef.current?.props.onMove(0);

        expect(requestAnimationFrameSpy).toBeCalled();
        expect(scrollTopSpy).toBeCalled();

        requestAnimationFrameSpy.mockRestore();
        scrollTopSpy.mockRestore();
    });

    it('calls "renderRow" on each visible item', () => {
        const renderRowSpy = jest.fn(renderRow);

        const items = getItems(VIEWPORT_HEIGHT / ROW_HEIGHT);

        const wrapper = mountList({
            renderRow: renderRowSpy,
            getKey,
            getRowHeight,
            items,
        }, VIEWPORT_HEIGHT);

        const lastRenderedRowIndex = items.length - 1;

        expect(wrapper.baseElement.querySelectorAll('.ui-list-row')).toHaveLength(items.length);

        expect(renderRowSpy).nthCalledWith(1, 0, 0, -1);
        expect(renderRowSpy).lastCalledWith(lastRenderedRowIndex, lastRenderedRowIndex, -1);
    });

    it('should stop listening to DOM events and cancel animation frame before unmount', () => {
        const items = getItems(10);

        const wrapper = mountList({
            renderRow,
            getKey,
            getRowHeight,
            items,
        }, VIEWPORT_HEIGHT);

        const listRef = wrapper.getByTestId('listRef');

        const removeListListenerSpy = jest.spyOn(listRef, 'removeEventListener');
        const removeWindowListenerSpy = jest.spyOn(window, 'removeEventListener');
        const cancelAnimationFrameSpy = jest.spyOn(window, 'cancelAnimationFrame');

        wrapper.unmount();

        expect(removeListListenerSpy).toBeCalled();
        expect(removeWindowListenerSpy).toBeCalled();
        expect(cancelAnimationFrameSpy).toBeCalled();

        removeListListenerSpy.mockRestore();
        removeWindowListenerSpy.mockRestore();
        cancelAnimationFrameSpy.mockRestore();
    });

    it('should have default "rowsAhead" value', () => {
        const items = getItems(50);

        const wrapper = mountList({
            items,
            renderRow,
            getKey,
            getRowHeight,
        }, VIEWPORT_HEIGHT);

        const visibleRowsCount = VIEWPORT_HEIGHT / ROW_HEIGHT;
        const renderedRowsCount = wrapper.baseElement.querySelectorAll('.ui-list-row')?.length || 0;

        expect(renderedRowsCount - visibleRowsCount).toEqual(ROWS_AHEAD);
    });

    it('should recompute rows on window resize', () => {
        const items = getItems(50);
        const props = {
            items,
            renderRow,
            getKey,
            getRowHeight,
        };

        const wrapper = mountList(props, VIEWPORT_HEIGHT);
        const previousRowsCount = wrapper.baseElement.querySelectorAll('.ui-list-row')?.length || 0;
        mountList(props, VIEWPORT_HEIGHT * 2, wrapper);
        expect(wrapper.baseElement.querySelectorAll('.ui-list-row')).not.toHaveLength(previousRowsCount);
    });

    it('recomputes rows when scrolling', () => {
        const items = getItems(50);

        const wrapper = mountList({
            items,
            renderRow,
            getKey,
            getRowHeight,
        }, VIEWPORT_HEIGHT);

        const firstRowNode = wrapper.baseElement.querySelector('.ui-list-row')!;
        const listRef = wrapper.getByTestId('listRef');
        fireEvent.scroll(listRef, { target: { scrollTop: 500 } });

        expect(wrapper.baseElement.querySelector('.ui-list-row')?.textContent).not.toEqual(firstRowNode.textContent);
    });

    it('calls "onScroll" when scrolling', () => {
        const onScrollMock = jest.fn();

        const wrapper = mountList({
            onScroll: onScrollMock,
            renderRow,
            getKey,
            getRowHeight,
            items: [],
        }, VIEWPORT_HEIGHT);

        const scrollEvent = new Event('scroll');
        wrapper.getByTestId('listRef').dispatchEvent(scrollEvent);

        expect(onScrollMock).toBeCalledWith(scrollEvent);
    });

    it('renders header if "renderHeader" is defined', () => {
        const headerNode = (
            <div className="list-header">
                List header
            </div>
        );

        const wrapper = mountList({
            renderRow,
            getKey,
            getRowHeight,
            items: [],
        }, VIEWPORT_HEIGHT);

        expect(wrapper.baseElement.querySelector('.list-header')).toBeNull();

        mountList({
            renderRow,
            getKey,
            getRowHeight,
            renderHeader() {
                return headerNode;
            },
            items: [],
        }, VIEWPORT_HEIGHT, wrapper);

        expect(wrapper.baseElement.querySelector('.list-header')).not.toBeNull();
    });

    it('renders list of the correct height', () => {
        const items = getItems(10);

        const wrapper = mountList({
            rowHeight: ROW_HEIGHT,
            renderRow,
            getKey,
            items,
        }, VIEWPORT_HEIGHT);

        const expectedStyles = `height: ${items.length * ROW_HEIGHT}px;`;

        expect(getSpacerStyles(wrapper)).toEqual(expectedStyles);

        const getRowHeightSpy = jest.fn(getRowHeight);

        mountList({
            rowHeight: ROW_HEIGHT,
            renderRow,
            getKey,
            getRowHeight: getRowHeightSpy,
            items,
        }, VIEWPORT_HEIGHT, wrapper);

        expect(getRowHeightSpy).toBeCalled();
        expect(getSpacerStyles(wrapper)).toEqual(expectedStyles);

        getRowHeightSpy.mockRestore();
    });

    it('calls "getRowHeight" for each item if defined to build spacer', () => {
        const items = getItems(10);

        const getRowHeightSpy = jest.fn(getRowHeight);

        mountList({
            renderRow,
            getKey,
            getRowHeight: getRowHeightSpy,
            items,
        }, VIEWPORT_HEIGHT);

        const lastItemIndex = items.length - 1;

        // проверяем только вызовы в render, т.к. метод вызывается еще и в computeRows
        expect(getRowHeightSpy).nthCalledWith(1, 0, 0, expect.anything(), 0);
        expect(getRowHeightSpy).nthCalledWith(items.length, lastItemIndex, lastItemIndex, expect.anything(), 0);

        getRowHeightSpy.mockRestore();
    });

    it('calls "getKey" for items that are rendered', () => {
        const getKeySpy = jest.fn(getKey);
        const items = getItems(50);

        const wrapper = mountList({
            renderRow,
            getKey: getKeySpy,
            getRowHeight,
            items,
        }, VIEWPORT_HEIGHT);

        const expectedLastRowIndex = VIEWPORT_HEIGHT / ROW_HEIGHT + ROWS_AHEAD - 1;

        expect(getKeySpy).toBeCalledTimes(wrapper.baseElement.querySelectorAll('.ui-list-row').length || 0);

        expect(getKeySpy).nthCalledWith(1, 0, 0);
        expect(getKeySpy).lastCalledWith(expectedLastRowIndex, expectedLastRowIndex);

        getKeySpy.mockRestore();
    });

    it('renders empty list if "renderEmpty" is defined', () => {
        const emptyNode = (
            <div>
                Empty list
            </div>
        );
        const renderEmpty = () => emptyNode;
        const getProps = (items) => ({
            renderEmpty,
            renderRow,
            getKey,
            getRowHeight,
            items,
        });

        let wrapper = mountList(getProps([]), VIEWPORT_HEIGHT);

        expect(wrapper.baseElement.querySelectorAll('.ui-list-row')).toHaveLength(0);
        expect(Boolean(wrapper.queryByText('Empty list'))).toEqual(true);

        wrapper = mountList(getProps(getItems(10)), 10 * ROW_HEIGHT, wrapper);

        expect(wrapper.baseElement.querySelectorAll('.ui-list-row').length).toBeGreaterThan(0);
        expect(Boolean(wrapper.queryByText('Empty list'))).toEqual(false);
    });
});
