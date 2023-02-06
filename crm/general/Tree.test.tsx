import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider, Store } from '../../State';
import { Tree } from './Tree';
import { createById, createCategory } from '../../utils';
import { columnTestId } from './Column/Column.constants';
import { spinnerTestId, notFoundTextTestId } from './Tree.constants';

describe('components/Tree', () => {
  let store: Store = new Store();
  beforeAll(() => {
    Element.prototype.scrollIntoView = jest.fn();
  });

  afterAll(() => {
    jest.clearAllMocks();
  });

  beforeEach(() => {
    store = new Store();
  });

  const byId = createById([createCategory(1, [createCategory(2)])]);
  const root = [1];
  const highlightPath = [];
  const valueAsTree = {};

  it('renders root column', () => {
    store.tree.setup({
      byId,
      root,
      highlightPath,
      valueAsTree,
    });

    render(
      <Provider store={store}>
        <Tree />
      </Provider>,
    );
    const columnNodes = screen.getAllByTestId(columnTestId);

    expect(columnNodes).toHaveLength(1);
  });

  describe('state.loading.state', () => {
    beforeEach(() => {
      store.tree.setup({
        byId,
        root,
        highlightPath,
        valueAsTree,
      });
    });

    describe('when is true', () => {
      it('renders spinner', () => {
        store.tree.loading.on();

        render(
          <Provider store={store}>
            <Tree />
          </Provider>,
        );
        const spinnerNode = screen.getByTestId(spinnerTestId);

        expect(spinnerNode).toBeInTheDocument();
      });
    });

    describe('when is false', () => {
      it(`doesn't render spinner`, () => {
        store.tree.loading.off();

        render(
          <Provider store={store}>
            <Tree />
          </Provider>,
        );
        const spinnerNode = screen.queryByTestId(spinnerTestId);
        expect(spinnerNode).not.toBeInTheDocument();
      });
    });
  });

  describe('state.root', () => {
    describe('when is empty', () => {
      const byId = {};
      const root = [];
      const highlightPath = [];
      const valueAsTree = {};

      it('renders "not found" text', () => {
        store.tree.setup({
          byId,
          root,
          highlightPath,
          valueAsTree,
        });

        render(
          <Provider store={store}>
            <Tree />
          </Provider>,
        );
        const infoTextNode = screen.getByTestId(notFoundTextTestId);

        expect(infoTextNode).toBeInTheDocument();
      });
    });

    describe('when contains rootIds', () => {
      const byId = createById([createCategory(1, [createCategory(2)])]);
      const root = [1];
      const highlightPath = [1];
      const valueAsTree = {};

      it('renders the tree', () => {
        store.tree.setup({
          byId,
          root,
          highlightPath,
          valueAsTree,
        });

        render(
          <Provider store={store}>
            <Tree />
          </Provider>,
        );

        const columnNodes = screen.getAllByTestId(columnTestId);
        expect(columnNodes).toHaveLength(2);
      });
    });
  });

  describe('state.expanded', () => {
    const byId = createById([createCategory(1, [createCategory(2)])]);
    const root = [1];
    const highlightPath = [1];
    const valueAsTree = {};

    it('renders column for each expanded category', () => {
      store.tree.setup({
        byId,
        root,
        highlightPath,
        valueAsTree,
      });

      render(
        <Provider store={store}>
          <Tree />
        </Provider>,
      );
      const columnNodes = screen.getAllByTestId(columnTestId);

      expect(columnNodes).toHaveLength(2);
    });
  });
});
