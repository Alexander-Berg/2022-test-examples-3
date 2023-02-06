import {
  finishEditGood,
  finishEditGoodFailure,
  finishEditGoodSuccess,
  getGoods,
  getGoodsFailure,
  getGoodsSuccess,
  startEditGood,
  startEditGoodFailure,
  startEditGoodSuccess,
} from 'src/actions/photoeditor';
import { goodUi } from 'src/mockData';
import { DataPage, GoodSortBy, GoodsPaging, GoodUi } from 'src/rest/definitions';
import reducer, { initialState } from './goodsReducer';

describe('PhotoEditor goods reducer', () => {
  describe('getGoods', () => {
    it('should set loading to true and update paging', () => {
      const paging: GoodsPaging = {
        page: 2,
        sort: GoodSortBy.ID,
      };

      expect(reducer(initialState, getGoods(paging))).toEqual({
        ...initialState,
        loading: true,
        paging: {
          ...initialState.paging,
          ...paging,
        },
      });
    });
  });

  describe('getGoodsSuccess', () => {
    it('should set loading to false and set data', () => {
      const data: DataPage<GoodUi> = {
        items: [goodUi],
        totalCount: 1,
      };

      expect(reducer(initialState, getGoodsSuccess(data))).toEqual({
        ...initialState,
        loading: false,
        goods: data.items,
        totalCount: data.totalCount,
      });
    });
  });

  describe('getGoodsFailure', () => {
    it('should set loading to false', () => {
      expect(reducer(initialState, getGoodsFailure())).toEqual({
        ...initialState,
        loading: false,
      });
    });
  });

  describe('startEditGood', () => {
    it('should set goodChangingIds to { [goodId]: true }', () => {
      expect(reducer(initialState, startEditGood(1))).toEqual({
        ...initialState,
        goodChangingIds: {
          1: true,
        },
      });
    });
  });

  describe('startEditGoodSuccess', () => {
    it('should set goodChangingIds to { [goodId]: false }', () => {
      expect(reducer(initialState, startEditGoodSuccess([goodUi]))).toEqual({
        ...initialState,
        goodChangingIds: {
          [goodUi.good.id]: false,
        },
      });
    });
  });

  describe('startEditGoodFailure', () => {
    it('should set goodChangingIds to { [goodId]: false }', () => {
      expect(reducer(initialState, startEditGoodFailure([1]))).toEqual({
        ...initialState,
        goodChangingIds: {
          1: false,
        },
      });
    });
  });

  describe('finishEditGood', () => {
    it('should set goodChangingIds to { [goodId]: true }', () => {
      expect(reducer(initialState, finishEditGood(1))).toEqual({
        ...initialState,
        goodChangingIds: {
          1: true,
        },
      });
    });
  });

  describe('finishEditGoodSuccess', () => {
    it('should set goodChangingIds to { [goodId]: false }', () => {
      expect(reducer(initialState, finishEditGoodSuccess([goodUi]))).toEqual({
        ...initialState,
        goodChangingIds: {
          [goodUi.good.id]: false,
        },
      });
    });
  });

  describe('finishEditGoodFailure', () => {
    it('should set goodChangingIds to { [goodId]: false }', () => {
      expect(reducer(initialState, finishEditGoodFailure([1]))).toEqual({
        ...initialState,
        goodChangingIds: {
          1: false,
        },
      });
    });
  });
});
