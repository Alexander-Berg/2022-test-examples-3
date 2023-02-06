import { setupServer } from 'msw/node';
import { waitFor } from '@testing-library/react';
import { SortObject } from 'types/api/form/Form';
import { rest } from 'msw';
import { TableController } from './TableController.service';
import { tableData } from './__mocks__/tableData';

const BASE_URL = '/table';

const server = setupServer(
  rest.get(`${BASE_URL}`, (req, res, ctx) => {
    const query = req.url.searchParams.get('query');
    const responseData = { ...tableData };
    if (query) {
      const decodedQuery = JSON.parse(decodeURIComponent(query));
      responseData.filters = {
        userFilters: decodedQuery?.userFilters,
      };
      responseData.sort = decodedQuery?.sort;
    }
    return res(ctx.json(responseData));
  }),
);

describe('TableController', () => {
  beforeAll(() => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  describe('.constructor', () => {
    it('fetch table data', async () => {
      const controller = new TableController(BASE_URL);
      await waitFor(() => {
        expect(controller.tableData).toEqual(tableData);
      });
    });
  });

  describe('.getUserFilterById', () => {
    it('gets UserFilter', async () => {
      const controller = new TableController();
      const testUserFilter = { type: 'test', data: { value: 10 } };
      controller.addUserFilter('col1', testUserFilter);
      expect(controller.getUserFilterById('col1')).toEqual(testUserFilter);
    });
  });

  describe('.addUserFilter', () => {
    it('adds UserFilter', async () => {
      const controller = new TableController();
      const testUserFilter = { type: 'test', data: { value: 10 } };
      controller.addUserFilter('col1', testUserFilter);
      expect(controller.getUserFilterById('col1')).toEqual(testUserFilter);
    });
  });

  describe('.removeUserFilter', () => {
    it('removes UserFilter', async () => {
      const controller = new TableController();
      const testUserFilter = { type: 'test', data: { value: 10 } };
      controller.addUserFilter('col1', testUserFilter);
      controller.removeUserFilter('col1');
      expect(controller.getUserFilterById('col1')).toEqual(undefined);
    });
  });

  describe('.getSortById', () => {
    it('gets sort by Id', async () => {
      const controller = new TableController();
      const testSortObject = { id: 'col1', order: 'Asc' } as SortObject;
      controller.addSort(testSortObject);
      expect(controller.getSortById('col1')).toEqual(testSortObject);
    });
  });

  describe('.addSortField', () => {
    it('adds sort by field', async () => {
      const controller = new TableController();
      const testSortObject = { id: 'col1', order: 'Asc' } as SortObject;
      controller.addSort(testSortObject);
      expect(controller.getSortById('col1')).toEqual(testSortObject);
    });
  });

  describe('.removeSortField', () => {
    it('removes sort by field', async () => {
      const controller = new TableController();
      const testSortObject = { id: 'col1', order: 'Asc' } as SortObject;
      controller.addSort(testSortObject);
      controller.removeSort('col1');
      expect(controller.getSortById('col1')).toEqual(undefined);
    });
  });

  describe('.addTemplateFilter', () => {
    it('adds template filter', async () => {
      const controller = new TableController();
      controller.addTemplateFilter(111);
      expect(controller.hasTemplateFilter(111)).toEqual(true);
    });
  });

  describe('.removeTemplateFilter', () => {
    it('removes template filter', async () => {
      const controller = new TableController();
      controller.addTemplateFilter(111);
      controller.addTemplateFilter(122);
      controller.addTemplateFilter(133);
      controller.removeTemplateFilter(111);
      expect(controller.hasTemplateFilter(111)).toEqual(false);
      expect(controller?.templateFilters).toEqual([122, 133]);
    });
  });

  describe('.toggleTemplateFilter', () => {
    it('toggles template filter', async () => {
      const controller = new TableController();
      controller.addTemplateFilter(111);
      controller.toggleTemplateFilter(111);
      expect(controller.hasTemplateFilter(111)).toEqual(false);
    });
    it('clears selected filters', async () => {
      const controller = new TableController();
      controller.addTemplateFilter(112);
      controller.toggleTemplateFilter(111);
      expect(controller.hasTemplateFilter(112)).toBeFalsy();
      expect(controller.hasTemplateFilter(111)).toBeTruthy();
    });
  });

  describe('.setCurrentPage', () => {
    it('sets current page', async () => {
      const controller = new TableController();
      controller.setCurrentPage(5);
      expect(controller.getCurrentPage()).toEqual(5);
    });
  });

  describe('.setPageSize', () => {
    it('sets page size', async () => {
      const controller = new TableController();
      controller.setPageSize(20);
      expect(controller.getPageSize()).toEqual(20);
    });
  });

  describe('.resetCurrentPage', () => {
    it('resets current page', async () => {
      const controller = new TableController();
      controller.setCurrentPage(20).resetCurrentPage();
      expect(controller.getCurrentPage()).toEqual(1);
    });
  });

  describe('.fetch', () => {
    it('fetches data with query params', async () => {
      const controller = new TableController(BASE_URL);
      const testSortObject = { id: 'col1', order: 'Asc' } as SortObject;
      controller.addSort(testSortObject).fetch();
      await waitFor(() => {
        expect(controller.fetchingTableData).toEqual(false);
      });
      expect(controller?.tableData?.sort).toEqual([testSortObject]);
    });
  });

  describe('when call methods by chaining', () => {
    it('sends correct query', async () => {
      const controller = new TableController(BASE_URL);
      const testUserFilter = { type: 'test', data: { value: 10 } };
      const testSortObject = { id: 'col1', order: 'Asc' } as SortObject;
      controller
        .addSort(testSortObject)
        .addUserFilter('col1', testUserFilter)
        .fetch();
      await waitFor(() => {
        expect(controller.fetchingTableData).toEqual(false);
      });
      expect(controller?.tableData?.sort).toEqual([testSortObject]);
      expect(controller?.tableData?.filters?.userFilters?.col1).toEqual(testUserFilter);
    });
  });
});
