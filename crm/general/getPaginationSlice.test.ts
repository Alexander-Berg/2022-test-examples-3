import { getPaginationSlice } from './getPaginationSlice';
import { paginationMock } from './stubs/paginationMock';

describe('getPaginationSlice', () => {
  describe('when active first page', () => {
    let slice;
    beforeAll(() => {
      const testPagination = { ...paginationMock, page: 1 };
      slice = getPaginationSlice(testPagination);
    });

    it('does not push first dots', () => {
      expect(slice[1].caption).not.toEqual('...');
    });

    it('push last dots', () => {
      expect(slice[slice.length - 2].caption).toEqual('...');
    });

    it('returns 7 item', () => {
      expect(slice.length).toEqual(7);
    });
  });

  describe('when active middle page', () => {
    let slice;
    beforeAll(() => {
      const testPagination = { ...paginationMock, page: 10 };
      slice = getPaginationSlice(testPagination);
    });

    it('push first dots', () => {
      expect(slice[1].caption).toEqual('...');
    });

    it('push last dots', () => {
      expect(slice[slice.length - 2].caption).toEqual('...');
    });
    it('returns 13 items', () => {
      expect(slice.length).toEqual(13);
    });
  });

  describe('when active last page', () => {
    let slice;
    beforeAll(() => {
      const testPagination = { ...paginationMock, page: 20 };
      slice = getPaginationSlice(testPagination);
    });

    it('push first dots', () => {
      expect(slice[1].caption).toEqual('...');
    });

    it('does not push last dots', () => {
      expect(slice[slice.length - 2].caption).not.toEqual('...');
    });

    it('returns 7 item', () => {
      expect(slice.length).toEqual(7);
    });
  });
});
