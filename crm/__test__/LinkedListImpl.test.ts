import { reaction } from 'mobx';
import { LinkedListImpl } from '../LinkedListImpl';
import { LinkedListNodeImpl } from '../LinkedListNodeImpl';

describe('LinkedListImpl', () => {
  it('creates list by array', () => {
    const linkedList = new LinkedListImpl([1, 2]);

    expect(linkedList.first!.value).toBe(1);
    expect(linkedList.last!.value).toBe(2);
    expect(linkedList.first!.next).toBe(linkedList.last);
    expect(linkedList.count).toBe(2);
  });

  describe('.removeNode', () => {
    it('removes node', () => {
      const linkedList = new LinkedListImpl([1, 2]);
      const node = linkedList.first;

      linkedList.removeNode(node!);

      expect(node!.list).toBeUndefined();
      expect(node!.previous).toBeUndefined();
      expect(node!.next).toBeUndefined();
      expect(linkedList.count).toBe(1);
    });
  });

  describe('.addAfter', () => {
    it('adds node after', () => {
      const linkedList = new LinkedListImpl([1, 2]);
      const node = new LinkedListNodeImpl(3);

      linkedList.addAfter(linkedList.first!, node);

      expect(node.list).toBe(linkedList);
      expect(node.previous).toBe(linkedList.first);
      expect(node.next).toBe(linkedList.last);
      expect(linkedList.count).toBe(3);
    });
  });

  describe('.addFirst', () => {
    it('adds fist node', () => {
      const linkedList = new LinkedListImpl();
      const node = new LinkedListNodeImpl(1);

      linkedList.addFirst(node);

      expect(node.list).toBe(linkedList);
      expect(node.previous).toBeUndefined();
      expect(node.next).toBeUndefined();
      expect(linkedList.count).toBe(1);
    });

    it('adds second node', () => {
      const linkedList = new LinkedListImpl();
      const node1 = new LinkedListNodeImpl(1);
      const node2 = new LinkedListNodeImpl(2);

      linkedList.addFirst(node1);
      linkedList.addFirst(node2);

      expect(node1.list).toBe(linkedList);
      expect(node1.previous).toBe(node2);
      expect(node1.next).toBeUndefined();

      expect(node2.list).toBe(linkedList);
      expect(node2.previous).toBeUndefined();
      expect(node2.next).toBe(node1);

      expect(linkedList.count).toBe(2);
    });
  });

  describe('.toArray', () => {
    it('converts to array', () => {
      const linkedList = new LinkedListImpl([1, 2]);

      expect(linkedList.toArray()).toStrictEqual([1, 2]);
    });
  });

  describe('mobx integration', () => {
    interface MobxIntegrationOptions {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expression: (list: LinkedListImpl<number>) => any;
      todo: (list: LinkedListImpl<number>) => void;
    }

    const setupMobxIntegrationTest = ({ expression, todo }: MobxIntegrationOptions) => {
      const linkedList = new LinkedListImpl([1, 2]);

      const watcher = jest.fn();
      const disposer = reaction(expression(linkedList), watcher, { fireImmediately: true });
      todo(linkedList);
      disposer();

      return watcher;
    };

    describe('.asArray', () => {
      it('notifies changes', () => {
        const watcher = setupMobxIntegrationTest({
          expression: (linkedList) => () => linkedList.asArray,
          todo: (linkedList) => {
            linkedList.addLast(3);
          },
        });

        expect(watcher).toBeCalledTimes(2);
        expect(watcher.mock.calls[0][0]).toStrictEqual([1, 2]);
        expect(watcher.mock.calls[1][0]).toStrictEqual([1, 2, 3]);
      });
    });

    describe('.count', () => {
      it('notifies changes', () => {
        const watcher = setupMobxIntegrationTest({
          expression: (linkedList) => () => linkedList.count,
          todo: (linkedList) => {
            linkedList.addLast(3);
          },
        });

        expect(watcher).toBeCalledTimes(2);
        expect(watcher.mock.calls[0][0]).toStrictEqual(2);
        expect(watcher.mock.calls[1][0]).toStrictEqual(3);
      });
    });

    describe('.first', () => {
      it('notifies changes', () => {
        const watcher = setupMobxIntegrationTest({
          expression: (linkedList) => () => linkedList.first?.value,
          todo: (linkedList) => {
            linkedList.addFirst(3);
          },
        });

        expect(watcher).toBeCalledTimes(2);
        expect(watcher.mock.calls[0][0]).toStrictEqual(1);
        expect(watcher.mock.calls[1][0]).toStrictEqual(3);
      });
    });

    describe('.last', () => {
      it('notifies changes', () => {
        const watcher = setupMobxIntegrationTest({
          expression: (linkedList) => () => linkedList.last?.value,
          todo: (linkedList) => {
            linkedList.addLast(3);
          },
        });

        expect(watcher).toBeCalledTimes(2);
        expect(watcher.mock.calls[0][0]).toStrictEqual(2);
        expect(watcher.mock.calls[1][0]).toStrictEqual(3);
      });
    });
  });
});
