import React, { ReactNodeArray } from 'react';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { render, cleanup, waitFor, screen, fireEvent } from '@testing-library/react';
import { TreeSuggestMultiple } from './TreeSuggestMultiple';
import { TreeDictStub } from './Tree/stubs/TreeDictStub';

jest.mock('react-virtualized-auto-sizer', () => {
  return jest.fn().mockImplementation(({ children }) => {
    return <div>{children(500, 500)}</div>;
  });
});
jest.mock('react-vtree', () => {
  return {
    VariableSizeTree: jest.fn().mockImplementation((props) => {
      const NodeComponent = props.children;
      const collection: ReactNodeArray = [];
      if (collection.length === 0) {
        const it = props.treeWalker(true);
        for (const item of it) {
          collection.push(<NodeComponent key={item.id} data={item} />);
        }
      }
      return <div>{collection}</div>;
    }),
  };
});

const BASE_URL = '/dict';

const filter = {
  type: 'TreeSuggestMultiple',
  provider: BASE_URL,
};

const changeFilter = jest.fn();

const server = setupServer(
  rest.get(`${BASE_URL}`, (req, res, ctx) => {
    return res(ctx.json(TreeDictStub));
  }),
);

describe('TreeSuggestMultiple', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders', async () => {
    render(<TreeSuggestMultiple changeFilter={changeFilter} filter={filter} />);
    await waitFor(() => {
      expect(screen.getByText('item1')).toBeInTheDocument();
    });
  });

  describe('when changes searchText', () => {
    it('filters tree', async () => {
      render(<TreeSuggestMultiple changeFilter={changeFilter} filter={filter} />);
      await waitFor(() => {
        fireEvent.change(screen.getByTestId('TreeSuggestMultiple_searchInput'), {
          target: { value: '6' },
        });
      });
      await waitFor(() => {
        expect(screen.queryByText('item1')).not.toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByText('item4')).toBeInTheDocument();
      });
    });

    it('opens all nodes', async () => {
      render(<TreeSuggestMultiple changeFilter={changeFilter} filter={filter} />);
      await waitFor(() => {
        fireEvent.change(screen.getByTestId('TreeSuggestMultiple_searchInput'), {
          target: { value: 'item' },
        });
      });
      await waitFor(() => {
        expect(screen.getByText('5')).toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByText('3')).toBeInTheDocument();
      });
    });
  });

  describe('when click on node', () => {
    it('calls changeFilter node', async () => {
      render(<TreeSuggestMultiple changeFilter={changeFilter} filter={filter} />);
      await waitFor(() => {
        fireEvent.click(screen.getByText('item1'));
      });
      const { items, ...firstNodeWithoutItems } = TreeDictStub.items[0];
      await waitFor(() => {
        expect(changeFilter).toBeCalledTimes(1);
        expect(changeFilter).toHaveBeenCalledWith({
          ...filter,
          data: { value: [firstNodeWithoutItems] },
        });
      });
    });
  });

  describe('when filter not empty', () => {
    it('renders selectedNode in selectedNodeList', async () => {
      const { items, ...firstNodeWithoutItems } = TreeDictStub.items[0];
      const notEmptyFilter = { ...filter, data: { value: [firstNodeWithoutItems] } };
      render(<TreeSuggestMultiple changeFilter={changeFilter} filter={notEmptyFilter} />);
      await waitFor(() => {
        expect(screen.getByText(firstNodeWithoutItems.fullName)).toBeInTheDocument();
      });
    });
    describe('when click on remove node button', () => {
      it('calls change filter', async () => {
        const { items, ...firstNodeWithoutItems } = TreeDictStub.items[0];
        const notEmptyFilter = { ...filter, data: { value: [firstNodeWithoutItems] } };
        const { container } = render(
          <TreeSuggestMultiple changeFilter={changeFilter} filter={notEmptyFilter} />,
        );
        await waitFor(() => {
          const element = container.querySelector('.SelectedNode__button');
          if (element) {
            fireEvent.click(element);
          }
        });
        await waitFor(() => {
          expect(changeFilter).toHaveBeenCalledWith(filter);
        });
      });
    });
  });
});
