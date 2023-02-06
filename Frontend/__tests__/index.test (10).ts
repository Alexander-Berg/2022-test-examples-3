import { Heap } from '../index';

describe('ReactCache', () => {
    describe('Heap', () => {
        describe('#add', () => {
            it('Добавление ключей в кучу', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                const node2 = heap.insert(2);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: node1,
                    value: 2,
                });

                const node3 = heap.insert(3);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: undefined,
                    right: node2,
                    value: 3,
                });

                const node4 = heap.insert(4);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });
            });
        });

        describe('#bubble', () => {
            it('Всплытие из середины кучи', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.bubble(node3);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node4,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: undefined,
                    right: node4,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node3,
                    right: node2,
                    value: 4,
                });
            });

            it('Последовательное поднятие узлов', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.bubble(node1);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: node4,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node1,
                    right: node3,
                    value: 4,
                });

                heap.bubble(node2);

                expect(node1).toMatchObject({
                    left: node2,
                    right: node4,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node1,
                    right: node3,
                    value: 4,
                });

                heap.bubble(node3);

                expect(node1).toMatchObject({
                    left: node2,
                    right: node4,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: undefined,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node1,
                    right: undefined,
                    value: 4,
                });

                heap.bubble(node4);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });
            });
        });

        describe('#remove', () => {
            it('Удаление узлов', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.remove(node3);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node4,
                    right: node1,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node2,
                    value: 4,
                });

                heap.remove(node4);

                expect(node1).toMatchObject({
                    left: node2,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: node1,
                    value: 2,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 4,
                });

                heap.remove(node2);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 2,
                });

                heap.remove(node1);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(heap.removeMin()).toBe(undefined);
                expect(heap.isEmpty()).toBeTruthy();
            });

            it('Удаление последнего узла', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.remove(node1);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });
            });
        });

        describe('#removeMin', () => {
            it('Уделение последнего элемента, после всплытия последнего элемента', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.bubble(node1);
                expect(heap.removeMin()).toBe(2);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: node4,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node1,
                    right: node3,
                    value: 4,
                });
            });

            it('Последовательное удаление последних узлов', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                let removed = heap.removeMin();

                expect(removed).toBe(1);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });

                removed = heap.removeMin();

                expect(removed).toBe(2);

                expect(node2).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });

                removed = heap.removeMin();

                expect(removed).toBe(3);

                expect(node3).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 4,
                });

                removed = heap.removeMin();

                expect(removed).toBe(4);

                expect(node4).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 4,
                });

                expect(heap.removeMin()).toBe(undefined);
                expect(heap.isEmpty()).toBeTruthy();
            });

            it('Удаление последнего элемента, а затем минимального', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                heap.remove(node1);
                expect(heap.removeMin()).toBe(2);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: undefined,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: undefined,
                    right: node3,
                    value: 4,
                });
            });

            it('Добавление после удаления', () => {
                const heap = new Heap<number>();

                const node1 = heap.insert(1);
                const node2 = heap.insert(2);
                const node3 = heap.insert(3);
                const node4 = heap.insert(4);

                expect(heap.removeMin()).toBe(1);
                const node5 = heap.insert(5);

                expect(node1).toMatchObject({
                    left: undefined,
                    right: undefined,
                    value: 1,
                });

                expect(node2).toMatchObject({
                    left: node3,
                    right: undefined,
                    value: 2,
                });

                expect(node3).toMatchObject({
                    left: node4,
                    right: node2,
                    value: 3,
                });

                expect(node4).toMatchObject({
                    left: node5,
                    right: node3,
                    value: 4,
                });

                expect(node5).toMatchObject({
                    left: undefined,
                    right: node4,
                    value: 5,
                });
            });
        });
    });
});
