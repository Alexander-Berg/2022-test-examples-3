import {
  createCart,
  createCartFailure,
  createCartSuccess,
  deleteCart,
  deleteCartFailure,
  deleteCartSuccess,
  getCarts,
  getCartsFailure,
  getCartsSuccess,
} from 'src/actions/assistant';
import { cartWithStats } from 'src/mockData';
import { CartWithStats, DataPage } from 'src/rest/definitions';
import reducer, { initialState } from './cartsReducer';

describe('Assistant carts reducer', () => {
  describe('getCarts', () => {
    it('should set loading to true and set page to 2', () => {
      expect(reducer(initialState, getCarts({ paging: { page: 2 } }))).toEqual({
        ...initialState,
        paging: {
          ...initialState.paging,
          page: 2,
        },
        loading: true,
      });
    });
  });

  describe('getCartsSuccess', () => {
    it('should set loading to false and set list of carts', () => {
      const payload: DataPage<CartWithStats> = {
        items: [cartWithStats],
        totalCount: 1,
      };

      expect(reducer(initialState, getCartsSuccess(payload))).toEqual({
        ...initialState,
        loading: false,
        carts: payload.items,
        totalCount: payload.totalCount,
      });
    });
  });

  describe('getCartsFailure', () => {
    it('should set loading to false', () => {
      expect(reducer(initialState, getCartsFailure())).toEqual({
        ...initialState,
        loading: false,
      });
    });
  });

  describe('createCart', () => {
    it('should set creating to true', () => {
      expect(reducer(initialState, createCart('1'))).toEqual({
        ...initialState,
        creating: true,
      });
    });
  });

  describe('createSuccess', () => {
    it('should set creating to false', () => {
      expect(reducer(initialState, createCartSuccess(cartWithStats))).toEqual({
        ...initialState,
        creating: false,
      });
    });
  });

  describe('createFailure', () => {
    it('should set creating to false', () => {
      expect(reducer(initialState, createCartFailure())).toEqual({
        ...initialState,
        creating: false,
      });
    });
  });

  describe('deleteCart', () => {
    it('should set deleting to true', () => {
      expect(reducer(initialState, deleteCart(1))).toEqual({
        ...initialState,
        cartsInRemoval: {
          1: true,
        },
      });
    });
  });

  describe('deleteCartSaccess', () => {
    it('should set deleting to false', () => {
      expect(reducer(initialState, deleteCartSuccess(1))).toEqual({
        ...initialState,
        cartsInRemoval: {
          1: false,
        },
      });
    });
  });

  describe('deleteCartFailure', () => {
    it('should set deleting to false', () => {
      expect(reducer(initialState, deleteCartFailure(1))).toEqual({
        ...initialState,
        cartsInRemoval: {
          1: false,
        },
      });
    });
  });
});
