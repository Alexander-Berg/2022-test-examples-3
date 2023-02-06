import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider, Store } from '../../State';
import { Tree } from '../../types';
import { SearchResults } from './SearchResults';
import { categoryTestId, checkboxTestId } from '../Category/Category.constants';
import { noCategoriesFoundTestId, spinnerTestId, errorTextTestId } from './SearchResults.constants';
import { createById, createCategory } from '../../utils';
import { BoolState } from '../../State/defaultStores/BoolState';

describe('components/SearchResults', () => {
  const tree: Tree = {
    byId: createById([createCategory(1), createCategory(2)]),
    getById: (id) => tree.byId[id],
    root: [],
    expanded: [],
    selectCategory: jest.fn(),
    changeLeafValue: jest.fn(),
    changeBranchValue: jest.fn(),
    setChangeStrategy: jest.fn(),
    setup: jest.fn(),
    reset: jest.fn(),
    finiteSelected: [],
    highlightPath: [],
    loading: new BoolState(),
    valueAsTree: {
      current: {},
      initial: {},
    },
  };
  const resultIds: number[] = [1, 2];

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('state.search.resultIds', () => {
    describe('when is not empty', () => {
      it('renders all categories which ids are in resultIds', () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds,
            loading: new BoolState(false),
            error: new BoolState(false),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );
        const categoryNodes = screen.getAllByTestId(categoryTestId);

        expect(categoryNodes).toHaveLength(2);
      });
    });

    describe('when is empty', () => {
      it('renders "no categories found" text', () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds: [],
            loading: new BoolState(false),
            error: new BoolState(false),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );
        const categoryNodes = screen.queryAllByTestId(categoryTestId);
        expect(categoryNodes).toHaveLength(0);

        const notFoundNode = screen.getByTestId(noCategoriesFoundTestId);
        expect(notFoundNode).toBeInTheDocument();
      });
    });
  });

  describe('state.search.isLoading', () => {
    describe('when is true', () => {
      it(`renders spinner and doesn't render the search results`, () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds,
            loading: new BoolState(true),
            error: new BoolState(false),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );
        const categoryNodes = screen.queryAllByTestId(categoryTestId);
        expect(categoryNodes).toHaveLength(0);

        const spinnerNode = screen.getByTestId(spinnerTestId);
        expect(spinnerNode).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`renders spinner and doesn't render the search results`, () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds,
            loading: new BoolState(false),
            error: new BoolState(false),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );
        const categoryNodes = screen.getAllByTestId(categoryTestId);
        expect(categoryNodes).toHaveLength(2);

        const spinnerNode = screen.queryByTestId(spinnerTestId);
        expect(spinnerNode).not.toBeInTheDocument();
      });
    });
  });

  describe('state.search.isError', () => {
    describe('when is true', () => {
      it('renders error text', () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds,
            loading: new BoolState(false),
            error: new BoolState(true),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );

        const errorTextNode = screen.getByTestId(errorTextTestId);
        expect(errorTextNode).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render error text`, () => {
        const store = ({
          tree,
          textHighlighting: {
            byId: {},
          },
          search: {
            resultIds,
            loading: new BoolState(false),
            error: new BoolState(false),
          },
        } as unknown) as Store;

        render(
          <Provider store={store}>
            <SearchResults />
          </Provider>,
        );

        const errorTextNode = screen.queryByTestId(errorTextTestId);
        expect(errorTextNode).not.toBeInTheDocument();
      });
    });
  });

  describe('on category click', () => {
    it('calls state.selectCategory', () => {
      const store = ({
        tree,
        textHighlighting: {
          byId: {},
        },
        search: {
          resultIds,
          loading: new BoolState(false),
          error: new BoolState(false),
        },
        tip: {
          onLoad: () => jest.fn(),
        },
        emit: jest.fn(),
        selectCategory: jest.fn(),
      } as unknown) as Store;

      render(
        <Provider store={store}>
          <SearchResults />
        </Provider>,
      );
      const categoryNodes = screen.getAllByTestId(categoryTestId);

      fireEvent.click(categoryNodes[0]);

      expect(tree.selectCategory).toBeCalledTimes(1);
      expect(tree.selectCategory).toBeCalledWith(1);
    });
  });

  describe('on category change', () => {
    it('calls state.tree.changeBranchValue', () => {
      const store = ({
        tree,
        textHighlighting: {
          byId: {},
        },
        search: {
          resultIds,
          loading: new BoolState(false),
          error: new BoolState(false),
        },
        tip: {
          onLoad: () => jest.fn(),
        },
        emit: jest.fn(),
        changeBranchValue: jest.fn(),
      } as unknown) as Store;

      render(
        <Provider store={store}>
          <SearchResults />
        </Provider>,
      );
      const checkboxNodes = screen.getAllByTestId(checkboxTestId);

      fireEvent.click(checkboxNodes[0]);

      expect(tree.changeBranchValue).toBeCalledTimes(1);
      expect(tree.changeBranchValue).toBeCalledWith(1, true);
    });
  });
});
