import testStore from 'src/helpers/testStore';
import { selectAssistantState, selectCartsState, selectCartWithGoodsState } from './assistant';

const state = testStore().store.getState();

describe('assistant selectors', () => {
  describe('selectAssistantState', () => {
    it('should return selectAssistantState', () => {
      expect(selectAssistantState(state)).toEqual(state.assistant);
    });
  });

  describe('selectCartsState', () => {
    it('should return CartsState', () => {
      expect(selectCartsState(state)).toEqual(state.assistant.carts);
    });
  });

  describe('selectCartWithGoodsState', () => {
    it('should return CartWithGoodsState', () => {
      expect(selectCartWithGoodsState(state)).toEqual(state.assistant.cartWithGoods);
    });
  });
});
