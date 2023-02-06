import { Queue } from '../Queue';
import { QueueTask } from '../QueueTask';

describe('Queue', () => {
    describe('#push', () => {
        it('Задачи должны выполняться при push-e если очередь на заблокирована', () => {
            const queue = new Queue();
            const task1 = jest.fn();
            const task2 = jest.fn();

            queue.push(new QueueTask(task1));

            expect(task1).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();

            queue.push(new QueueTask(task2));

            expect(task1).toBeCalledTimes(1);
            expect(task2).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Задачи должны выполняться после разблокировки очереди', () => {
            const queue = new Queue(true);
            const task1 = jest.fn();
            const task2 = jest.fn();

            queue.push(new QueueTask(task1));
            queue.push(new QueueTask(task2));

            expect(task1).not.toBeCalled();
            expect(queue.isEmpty()).toBeFalsy();

            expect(task2).not.toBeCalled();
            expect(queue.isEmpty()).toBeFalsy();

            queue.unlock();

            expect(task1).toBeCalledTimes(1);
            expect(task2).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Очередь выполнения должна прерваться, если во время исполниения задачи она была заблокирована', () => {
            const queue = new Queue();
            const task1 = jest.fn(() => {
                queue.lock();
            });
            const task2 = jest.fn();

            queue.push(new QueueTask(task1));
            queue.push(new QueueTask(task2));

            expect(task1).toBeCalledTimes(1);
            expect(task2).not.toBeCalled();
            expect(queue.isEmpty()).toBeFalsy();

            queue.unlock();

            expect(task1).toBeCalledTimes(1);
            expect(task2).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Задача добавленная в другой задаче должна быть выполнена', () => {
            const queue = new Queue();
            const task2 = jest.fn();
            const task1 = jest.fn(() => {
                queue.push(new QueueTask(task2));
            });

            queue.push(new QueueTask(task1));

            expect(task1).toBeCalledTimes(1);
            expect(task2).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();
        });
    });

    describe('#cancel', () => {
        it('Все задачи в очереди должны быть отменены', () => {
            const queue = new Queue(true);
            const task1 = jest.fn();
            const task2 = jest.fn();
            const onCancel1 = jest.fn();
            const onCancel2 = jest.fn();

            queue.push(new QueueTask(task1, { onCancel: onCancel1 }));
            queue.push(new QueueTask(task2, { onCancel: onCancel2 }));

            queue.cancel();

            queue.unlock();

            expect(task1).not.toBeCalled();
            expect(task2).not.toBeCalled();

            expect(onCancel1).toBeCalledTimes(1);
            expect(onCancel2).toBeCalledTimes(1);

            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Задачи c указанным тэгом должны быть отменены', () => {
            const queue = new Queue(true);
            const task1 = jest.fn();
            const task2 = jest.fn();
            const task3 = jest.fn();
            const onCancel1 = jest.fn();
            const onCancel2 = jest.fn();
            const onCancel3 = jest.fn();

            queue.push(new QueueTask(task1, { tag: 'test', onCancel: onCancel1 }));
            queue.push(new QueueTask(task2, { tag: 'test', onCancel: onCancel2 }));
            queue.push(new QueueTask(task3, { onCancel: onCancel3 }));

            queue.cancel({ tag: 'test' });

            queue.unlock();

            expect(task1).not.toBeCalled();
            expect(task2).not.toBeCalled();
            expect(task3).toBeCalledTimes(1);

            expect(onCancel1).toBeCalledTimes(1);
            expect(onCancel2).toBeCalledTimes(1);
            expect(onCancel3).not.toBeCalled();

            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Задача c указанным id должна быть отменена', () => {
            const queue = new Queue(true);
            const task1 = jest.fn();
            const task2 = jest.fn();
            const task3 = jest.fn();
            const onCancel1 = jest.fn();
            const onCancel2 = jest.fn();
            const onCancel3 = jest.fn();

            queue.push(new QueueTask(task1, { id: 'canceled_task', onCancel: onCancel1 }));
            queue.push(new QueueTask(task2, { tag: 'test', onCancel: onCancel2 }));
            queue.push(new QueueTask(task3, { onCancel: onCancel3 }));

            queue.cancel({ id: 'canceled_task' });

            queue.unlock();

            expect(task1).not.toBeCalled();
            expect(task2).toBeCalledTimes(1);
            expect(task3).toBeCalledTimes(1);

            expect(onCancel1).toBeCalledTimes(1);
            expect(onCancel2).not.toBeCalled();
            expect(onCancel3).not.toBeCalled();

            expect(queue.isEmpty()).toBeTruthy();
        });

        it('Задача c указанным id должна быть отменена, если отмена вызвана в другой задаче', () => {
            const queue = new Queue(true);
            const task1 = jest.fn(() => {
                queue.cancel({ tag: 'test' });
            });
            const task2 = jest.fn();
            const onCancel1 = jest.fn();
            const onCancel2 = jest.fn();

            queue.push(new QueueTask(task1, { onCancel: onCancel1 }));
            queue.push(new QueueTask(task2, { tag: 'test', onCancel: onCancel2 }));

            queue.unlock();

            expect(task1).toBeCalledTimes(1);
            expect(task2).not.toBeCalled();

            expect(onCancel1).not.toBeCalled();
            expect(onCancel2).toBeCalledTimes(1);

            expect(queue.isEmpty()).toBeTruthy();
        });

        it('В обработчике отмены нельзя добавлять задачи', (done) => {
            const queue = new Queue(true);
            const task1 = jest.fn();
            const task2 = jest.fn();
            const onCancel1 = jest.fn(() => {
                expect(queue.push(new QueueTask(task2))).toBeFalsy();
                done();
            });

            queue.push(new QueueTask(task1, { id: 'canceled_task', onCancel: onCancel1 }));

            queue.cancel({ id: 'canceled_task' });

            queue.unlock();

            expect(task1).not.toBeCalled();
            expect(task2).toBeCalledTimes(0);

            expect(onCancel1).toBeCalledTimes(1);

            expect(queue.isEmpty()).toBeTruthy();

            queue.push(new QueueTask(task2));

            expect(task2).toBeCalledTimes(1);
            expect(queue.isEmpty()).toBeTruthy();
        });
    });
});
