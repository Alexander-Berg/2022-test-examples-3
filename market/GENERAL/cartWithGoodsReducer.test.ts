import {
  addGoodToCart,
  addGoodToCartFailure,
  addGoodToCartSuccess,
  getCartWithGoods,
  getCartWithGoodsFailure,
  getCartWithGoodsSuccess,
  removeGoodFromCart,
  removeGoodFromCartFailure,
  removeGoodFromCartSuccess,
  sortGoods,
  updateWhBarcode,
} from 'src/actions/assistant';
import { cartWithStats, goodUi } from 'src/mockData';
import { SortOrder, CartWithGoods } from 'src/rest/definitions';

import reducer, { initialState } from './cartWithGoodsReducer';

describe('Assistant cartWithGoods reducer', () => {
  describe('getCartWithGoods', () => {
    it('should set loading to true', () => {
      expect(reducer(initialState, getCartWithGoods(1))).toEqual({
        ...initialState,
        loading: true,
      });
    });
  });

  describe('getCartWithGoodsSuccess', () => {
    it('should set loading to false and set list of goods', () => {
      const data: CartWithGoods = {
        cartWithStats,
        goods: [goodUi],
      };

      expect(reducer(initialState, getCartWithGoodsSuccess(data))).toEqual({
        ...initialState,
        ...data,
        loading: false,
      });
    });
  });

  describe('getCartWithGoodsFailure', () => {
    it('should set loading to false', () => {
      expect(reducer(initialState, getCartWithGoodsFailure())).toEqual({
        ...initialState,
        loading: false,
      });
    });
  });

  describe('addGoodToCart', () => {
    it('should set goodStateChanging to true', () => {
      expect(reducer(initialState, addGoodToCart(1, 'barcode'))).toEqual({
        ...initialState,
        goodStateChanging: true,
      });
    });
  });

  describe('addGoodToCartSuccess', () => {
    it('should set goodStateChanging to false', () => {
      const payload = goodUi;

      expect(reducer(initialState, addGoodToCartSuccess(payload))).toEqual({
        ...initialState,
        goodStateChanging: false,
        lastModifiedGoodId: payload.good.id,
      });
    });
  });

  describe('addGoodToCartFailure', () => {
    it('should set adding to false', () => {
      expect(reducer(initialState, addGoodToCartFailure())).toEqual({
        ...initialState,
      });
    });
  });

  describe('removeGoodFromCart', () => {
    it('should set removing to true', () => {
      expect(reducer(initialState, removeGoodFromCart(1, 1))).toEqual({
        ...initialState,
        goodsInRemoval: {
          1: true,
        },
      });
    });
  });

  describe('removeGoodFromCartSuccess', () => {
    it('should set removing to false and set list of goods', () => {
      const payload = goodUi;
      const goodsInRemoval = {
        [goodUi.good.id]: true,
      };

      expect(reducer({ ...initialState, goodsInRemoval }, removeGoodFromCartSuccess(goodUi.good.id, payload))).toEqual({
        ...initialState,
        goodsInRemoval: {
          [goodUi.good.id]: false,
        },
      });
    });
  });

  describe('removeGoodFromCartFailure', () => {
    it('should set removing to false', () => {
      expect(reducer(initialState, removeGoodFromCartFailure(1))).toEqual({
        ...initialState,
        goodsInRemoval: {
          1: false,
        },
      });
    });
  });

  describe('updateWhBarcode', () => {
    it('should update whBarcode', () => {
      const whBarcode = 'barcode';

      expect(reducer(initialState, updateWhBarcode(whBarcode))).toEqual({
        ...initialState,
        whBarcode,
      });
    });
  });

  describe('sortGoods', () => {
    it('should set sortField and sortOrder', () => {
      const sortField = 'id';
      const sortOrder = SortOrder.ASC;

      expect(reducer(initialState, sortGoods(sortField, sortOrder))).toEqual({
        ...initialState,
        sortField,
        sortOrder,
      });
    });
  });
});
