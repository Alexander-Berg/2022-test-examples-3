import React, { ReactNodeArray } from 'react';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { render, cleanup, waitFor, screen, fireEvent, act } from '@testing-library/react';
import { TemplateFilters } from './TemplateFilters';
import { TableController } from '../../services/TableController';
import { Provider } from '../../services/TableController/TableController.context';
import { FilterCollectionMock } from './stubs/FilterCollectionMock';

jest.mock('../../services/TableController');
jest.mock('react-virtualized-auto-sizer', () => {
  return jest.fn().mockImplementation(({ children }) => {
    return <div>{children(500, 500)}</div>;
  });
});
jest.mock('react-vtree', () => {
  return {
    FixedSizeTree: jest.fn().mockImplementation((props) => {
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

const BASE_URL = '/tableWithFilters';

const controller = new TableController();
controller.toggleTemplateFilter = jest.fn((_filterId) => controller);
controller.resetCurrentPage = jest.fn(() => controller);

const server = setupServer(
  rest.get(`${BASE_URL}`, (req, res, ctx) => {
    return res(ctx.json(FilterCollectionMock));
  }),
);

const TestTemplateFilters = () => {
  return (
    <Provider value={controller}>
      <TemplateFilters sourceUrl={BASE_URL} />
    </Provider>
  );
};

describe('TemplateFilters', () => {
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
    act(() => {
      render(<TestTemplateFilters />);
    });
    await waitFor(() => {
      expect(screen.getByText('Ниже 50 000')).toBeInTheDocument();
    });
  });

  describe('when click on filter', () => {
    it('calls .toggleTemplateFilter', async () => {
      act(() => {
        render(<TestTemplateFilters />);
      });
      await waitFor(() => {
        fireEvent.click(screen.getByText('Ниже 50 000'));
      });
      await waitFor(() => {
        expect(controller.toggleTemplateFilter).toBeCalledTimes(1);
      });
    });
  });

  describe('when click on group item', () => {
    it(' does not calls .toggleTemplateFilter', async () => {
      act(() => {
        render(<TestTemplateFilters />);
      });
      await waitFor(() => {
        fireEvent.click(screen.getByText('Статус лифта'));
      });
      await waitFor(() => {
        expect(controller.toggleTemplateFilter).toBeCalledTimes(0);
      });
    });
  });
});
