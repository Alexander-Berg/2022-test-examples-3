import { ArrayOperation } from 'types/ArrayOperation';
import { ArrayOperationBuilder } from '../ArrayOperationBuilder';

describe('ArrayOperationBuilder', () => {
  describe('.removeItem', () => {
    describe('when remove existed item', () => {
      it('returns operation', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.removeItem({ id: 1 })).toStrictEqual({
          type: ArrayOperation.RemoveItem,
          array: { next: [], prev: [{ id: 1 }] },
          item: { id: 1 },
        });
      });
    });

    describe('when remove not existed item', () => {
      it('returns null', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.removeItem({ id: 2 })).toBe(null);
      });
    });
  });

  describe('.removeItemById', () => {
    describe('when remove existed item', () => {
      it('returns remove operation', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.removeItemById(1)).toStrictEqual({
          type: ArrayOperation.RemoveItem,
          array: { next: [], prev: [{ id: 1 }] },
          item: { id: 1 },
        });
      });
    });

    describe('when remove not existed item', () => {
      it('returns null', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.removeItemById(2)).toBe(null);
      });
    });
  });

  describe('.addItem', () => {
    describe('when add existed item', () => {
      it('returns null', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.addItem({ id: 1 })).toBe(null);
      });
    });

    describe('when add not existed item', () => {
      it('returns add operation', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.addItem({ id: 2 })).toStrictEqual({
          type: ArrayOperation.AddItem,
          array: { next: [{ id: 1 }, { id: 2 }], prev: [{ id: 1 }] },
          item: { id: 2 },
        });
      });
    });
  });

  describe('.changeOrAddItem', () => {
    describe('when change existed item', () => {
      it('returns change operation', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.changeOrAddItem({ id: 1 })).toStrictEqual({
          type: ArrayOperation.ChangeItem,
          array: { next: [{ id: 1 }], prev: [{ id: 1 }] },
          item: { next: { id: 1 }, prev: { id: 1 } },
        });
      });
    });

    describe('when change not existed item', () => {
      it('returns add operation', () => {
        const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

        expect(arrayOperationBuilder.changeOrAddItem({ id: 2 })).toStrictEqual({
          type: ArrayOperation.AddItem,
          array: { next: [{ id: 1 }, { id: 2 }], prev: [{ id: 1 }] },
          item: { id: 2 },
        });
      });
    });
  });

  describe('.clear', () => {
    it('returns clear operation', () => {
      const arrayOperationBuilder = new ArrayOperationBuilder([{ id: 1 }]);

      expect(arrayOperationBuilder.clear()).toStrictEqual({
        type: ArrayOperation.Clear,
        array: { next: [], prev: [{ id: 1 }] },
      });
    });
  });
});
