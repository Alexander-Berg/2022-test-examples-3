import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Virtuoso, VirtuosoProps } from 'react-virtuoso';
import { AsyncTaskStatus } from 'types/AsyncTaskStatus';
import 'react-virtualized-auto-sizer';
import { MAIN_SPINNER_TEST_ID } from './VirtualList.config';
import { VirtualList } from './VirtualList';
import { NextLoader, PreviousLoader } from './EdgeLoader';
import { VirtualListServiceStub } from './VirtualListService.stub';
import { ListWrapper } from './ListWrapper';

const mockedListSize = { width: 100, height: 200 };

jest.mock('react-virtualized-auto-sizer', () => ({ children }) => children(mockedListSize));

jest.mock('react-virtuoso', () => ({
  Virtuoso: jest.fn(({ components }) => (
    <components.List>
      {components.Header && <components.Header />}
      {components.Footer && <components.Footer />}
    </components.List>
  )),
}));

const MockedVirtuoso = (Virtuoso as unknown) as jest.Mock<null, [VirtuosoProps<void, void>]>;
const itemContentRender = jest.fn((_index) => null);

describe('VirtualList', () => {
  beforeEach(() => {
    itemContentRender.mockClear();
    MockedVirtuoso.mockClear();
  });

  describe('init loading', () => {
    it('renders main spinner', () => {
      const virtualListServiceStub = new VirtualListServiceStub();
      virtualListServiceStub.loadInitTask.status = AsyncTaskStatus.Pending;

      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      expect(screen.queryByTestId(MAIN_SPINNER_TEST_ID)).toBeInTheDocument();
    });
  });

  describe('no data', () => {
    it('renders no data placeholder', () => {
      const virtualListServiceStub = new VirtualListServiceStub(0);

      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      expect(screen.queryByText('Список пуст')).toBeInTheDocument();
    });
  });

  describe('render list', () => {
    it('passes correct props to div wrap', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);

      const style = { color: 'black' };

      const { container } = render(
        <VirtualList
          className="className"
          style={style}
          listService={virtualListServiceStub}
          itemContent={itemContentRender}
          data-other="data"
        />,
      );

      expect(container.firstChild).toHaveStyle(style);
      expect(container.firstChild).toHaveClass('className');
    });

    it('passes correct props to virtual component', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);

      const style = { color: 'black' };

      render(
        <VirtualList
          className="className"
          style={style}
          listService={virtualListServiceStub}
          itemContent={itemContentRender}
          data-other="data"
        />,
      );

      const mockedVirtuosoProps = MockedVirtuoso.mock.calls[0][0];

      expect(MockedVirtuoso).toBeCalledWith(
        {
          alignToBottom: true,
          style: mockedListSize,
          firstItemIndex: virtualListServiceStub.firstItemIndex,
          totalCount: virtualListServiceStub.length,
          initialTopMostItemIndex: virtualListServiceStub.initialTopMostItemIndex,
          isScrolling: expect.any(Function),
          startReached: virtualListServiceStub.startReached,
          endReached: virtualListServiceStub.endReached,
          itemContent: mockedVirtuosoProps.itemContent,
          computeItemKey: mockedVirtuosoProps.computeItemKey,
          increaseViewportBy: 200,
          components: {
            Header: PreviousLoader,
            Footer: NextLoader,
            List: ListWrapper,
          },
          followOutput: 'smooth',
          'data-other': 'data',
        },
        {},
      );
    });

    it('passes correct itemContent', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);

      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      const mockedVirtuosoProps = MockedVirtuoso.mock.calls[0][0];
      mockedVirtuosoProps.itemContent!(9);

      expect(itemContentRender).toBeCalledWith(virtualListServiceStub.getItemByAbsoluteIndex(9));
    });

    it('passes correct computeItemKey', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);
      virtualListServiceStub.getItemIdByAbsoluteIndex = jest.fn((_index) => -1);

      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      const mockedVirtuosoProps = MockedVirtuoso.mock.calls[0][0];
      mockedVirtuosoProps.computeItemKey!(10);

      expect(virtualListServiceStub.getItemIdByAbsoluteIndex).toBeCalledWith(10);
    });

    it('adds disableHover class on scroll', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);

      const { container } = render(
        <VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />,
      );

      const mockedVirtuosoProps = MockedVirtuoso.mock.calls[0][0];
      mockedVirtuosoProps.isScrolling!(true);

      expect(container.firstChild?.firstChild).toHaveClass('ListWrapper_disableHover');
    });

    it('does not add disableHover when not scrolling', () => {
      const virtualListServiceStub = new VirtualListServiceStub(10);

      const { container } = render(
        <VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />,
      );

      const mockedVirtuosoProps = MockedVirtuoso.mock.calls[0][0];
      mockedVirtuosoProps.isScrolling!(true);
      mockedVirtuosoProps.isScrolling!(false);

      expect(container.firstChild?.firstChild).not.toHaveClass('ListWrapper_disableHover');
    });
  });

  describe('when init load error', () => {
    const virtualListServiceStub = new VirtualListServiceStub(10);
    virtualListServiceStub.loadInitTask.status = AsyncTaskStatus.Error;
    virtualListServiceStub.loadInitTask.error = new Error('init');
    const retryInitSpy = jest.spyOn(virtualListServiceStub, 'retryInit');

    it('renders error', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      expect(screen.queryByText(/init/)).toBeInTheDocument();
    });

    it('handles retry', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);
      fireEvent.click(screen.getByRole('button', { name: /повторить/i }));
      expect(retryInitSpy).toBeCalledTimes(1);
    });
  });

  describe('when previous load error', () => {
    const virtualListServiceStub = new VirtualListServiceStub(10);
    virtualListServiceStub.loadInitTask.status = AsyncTaskStatus.Complete;
    virtualListServiceStub.loadPreviousTask.status = AsyncTaskStatus.Error;
    virtualListServiceStub.loadPreviousTask.error = new Error('previous');
    virtualListServiceStub.hasMorePrevious = true;
    const retryLoadPreviousSpy = jest.spyOn(virtualListServiceStub, 'retryLoadPrevious');

    it('renders error', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      expect(screen.queryByText(/previous/)).toBeInTheDocument();
    });

    it('handles retry', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);
      fireEvent.click(screen.getByRole('button', { name: /повторить/i }));
      expect(retryLoadPreviousSpy).toBeCalledTimes(1);
    });
  });

  describe('when next load error', () => {
    const virtualListServiceStub = new VirtualListServiceStub(10);
    virtualListServiceStub.loadInitTask.status = AsyncTaskStatus.Complete;
    virtualListServiceStub.loadNextTask.status = AsyncTaskStatus.Error;
    virtualListServiceStub.loadNextTask.error = new Error('next');
    virtualListServiceStub.hasMoreNext = true;
    const retryLoadNextSpy = jest.spyOn(virtualListServiceStub, 'retryLoadNext');

    it('renders error', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);

      expect(screen.queryByText(/next/)).toBeInTheDocument();
    });

    it('handles retry', () => {
      render(<VirtualList listService={virtualListServiceStub} itemContent={itemContentRender} />);
      fireEvent.click(screen.getByRole('button', { name: /повторить/i }));
      expect(retryLoadNextSpy).toBeCalledTimes(1);
    });
  });
});
