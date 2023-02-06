import { configureStore } from '.';

describe('store', () => {
  describe('configureStore', () => {
    it('must contain certain methods', () => {
      const store = configureStore();

      expect(store).toHaveProperty('getState');
      expect(typeof store.getState).toBe('function');
      expect(store).toHaveProperty('subscribe');
      expect(typeof store.subscribe).toBe('function');
      expect(store).toHaveProperty('dispatch');
      expect(typeof store.dispatch).toBe('function');
    });
  });
});
