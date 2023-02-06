import { NormalizedListStore } from './NormalizedListStore';

describe('NormalizedListStore', () => {
  let store: NormalizedListStore<{}>;
  let subscriberMock = jest.fn((_store) => {});

  beforeEach(() => {
    subscriberMock.mockClear();
    store = new NormalizedListStore();
  });

  describe('.constructor', () => {
    it('has no error on create', () => {
      expect(() => {
        const _store = new NormalizedListStore();
      }).not.toThrow();
    });
  });

  describe('.getItemById', () => {
    it('returns no item for empty store', () => {
      expect(store.getItemById('1')).toBe(undefined);
    });
  });

  describe('.addItemById', () => {
    it('adds new item', () => {
      const isSuccessAdd = store.addItemById('1', 1);

      expect(store.getItemById('1')).toBe(1);
      expect(isSuccessAdd).toBe(true);
    });

    it('does not add new item with same id', () => {
      store.addItemById('1', 1);
      const isSuccessAdd = store.addItemById('1', 2);

      expect(store.getItemById('1')).toBe(1);
      expect(isSuccessAdd).toBe(false);
    });
  });

  describe('.setItemById', () => {
    it('sets new item', () => {
      store.setItemById('1', 1);

      expect(store.getItemById('1')).toBe(1);
    });

    it('replaces exist item', () => {
      store.setItemById('1', 1);
      store.setItemById('1', 2);

      expect(store.getItemById('1')).toBe(2);
    });
  });

  describe('.hasItemById', () => {
    it('returns false', () => {
      expect(store.hasItemById('1')).toBe(false);
    });

    it('returns true', () => {
      store.addItemById('1', 1);

      expect(store.hasItemById('1')).toBe(true);
    });
  });

  describe('.removeItemById', () => {
    it('removes item', () => {
      store.addItemById('1', 1);
      store.addItemById('2', 2);

      store.removeItemById('1');

      expect(store.hasItemById('1')).toBe(false);
      expect(store.getOrderIds()).toStrictEqual(['2']);
    });
  });

  describe('.getOrderIds', () => {
    it('has empty data by default', () => {
      expect(store.getOrderIds()).toHaveLength(0);
    });

    it('has correct ids order', () => {
      store.addItemById('2', 2);
      store.addItemById('1', 1);

      expect(store.getOrderIds()).toStrictEqual(['2', '1']);
    });
  });

  describe('.clear', () => {
    it('clears store', () => {
      store.addItemById('1', 1);
      store.clear();

      expect(store.getOrderIds()).toHaveLength(0);
    });
  });
});
