import pubsub from '@yandex-turbo/core/pubsub';
import { EVisibility } from '../types';
import { ActiveArticleManager, IArticleData } from '../activeArticleManager';

function flushPromises() {
    return new Promise(resolve => setImmediate(resolve));
}

describe('widgetsManager', () => {
    test('Подписывается на событие только один раз', async() => {
        const spy = jest.spyOn(pubsub, 'on');
        const manager = new ActiveArticleManager({});
        const handler = jest.fn();
        const anotherHandler = jest.fn();
        manager.listen('test-event', handler);
        manager.listen('test-event', anotherHandler);

        await flushPromises().then(() => {
            expect(spy).toBeCalledTimes(1);
        });

        spy.mockRestore();
    });

    test('Отписывает от события, если нет обработчиков', async() => {
        const spy = jest.spyOn(pubsub, 'off');
        const manager = new ActiveArticleManager({});
        const handler = jest.fn();
        const anotherHandler = jest.fn();

        manager.listen('test-event', handler);
        manager.listen('test-event', anotherHandler);
        await flushPromises();

        manager.unlisten('test-event', handler);
        await flushPromises().then(() => {
            expect(spy).not.toHaveBeenCalled();
        });

        manager.unlisten('test-event', anotherHandler);
        await flushPromises().then(() => {
            expect(spy).toHaveBeenCalled();
        });

        spy.mockRestore();
    });

    test('Вызывает обаботчики событий', async() => {
        const event: IArticleData = {
            event: 'test-event',
            pageHash: 'uniq',
            visibility: EVisibility.visible,
        };
        const preprocessor = jest.fn(e => e);
        const manager = new ActiveArticleManager({ preprocessWidget: preprocessor });
        const handler1 = jest.fn();
        const handler2 = jest.fn();
        const wontBeCalled = jest.fn();

        manager.listen('test-event', handler1);
        manager.listen('test-event', handler2);
        manager.listen('another-event', wontBeCalled);
        await flushPromises();

        pubsub.trigger('test-event', event);

        expect(preprocessor).toHaveBeenCalled();
        expect(handler1).toHaveBeenCalled();
        expect(handler2).toHaveBeenCalled();
        expect(wontBeCalled).not.toHaveBeenCalled();

        pubsub.trigger('test-event', { ...event, visibility: EVisibility.hidden });

        expect(handler1).toHaveBeenCalledWith({ pageHash: '', event: '', visibility: EVisibility.hidden });
        expect(handler2).toHaveBeenCalledWith({ pageHash: '', event: '', visibility: EVisibility.hidden });
    });

    test('Активен всегда последний виджет', async() => {
        const event1: IArticleData = {
            event: 'test-event',
            pageHash: 'uniq',
            visibility: EVisibility.visible,
        };

        const event2: IArticleData = {
            event: 'test-event',
            pageHash: 'uniq1',
            visibility: EVisibility.visible,
        };

        const emptyWidget: IArticleData = {
            visibility: EVisibility.hidden,
            pageHash: '',
            event: '',
        };

        const manager = new ActiveArticleManager({});
        const handler = jest.fn();

        manager.listen('test-event', handler);
        await flushPromises();

        expect(manager.getCurrentWidget()).toEqual(emptyWidget);
        expect(manager.getWidgetsCount()).toEqual(0);

        pubsub.trigger('test-event', event1);
        expect(manager.getCurrentWidget()).toEqual(event1);
        expect(manager.getWidgetsCount()).toEqual(1);

        pubsub.trigger('test-event', event2);
        expect(manager.getCurrentWidget()).toEqual(event2);
        expect(manager.getWidgetsCount()).toEqual(2);

        pubsub.trigger('test-event', { ...event1, visibility: EVisibility.hidden });
        expect(manager.getCurrentWidget()).toEqual(event2);
        expect(manager.getWidgetsCount()).toEqual(1);

        pubsub.trigger('test-event', { ...event2, visibility: EVisibility.hidden });
        expect(manager.getCurrentWidget()).toEqual(emptyWidget);
        expect(manager.getWidgetsCount()).toEqual(0);
    });
});
