import { Observer } from './Observer';

describe('Observer', () => {
    it('Should create observer', () => {
        expect(new Observer()).toBeInstanceOf(Observer);
    });

    it('Should add listener', () => {
        const callback1 = jest.fn();
        const callback2 = jest.fn();
        const callback3 = jest.fn();
        const observer = new Observer<{ event1: []; event2: []; event3: []; foobar: []; }>()
            .addListener('event1', callback1)
            .addListener('event1', callback2)
            .addListener('event2', callback3);

        observer.runListener('foobar');

        expect(callback1).toHaveBeenCalledTimes(0);
        expect(callback2).toHaveBeenCalledTimes(0);
        expect(callback3).toHaveBeenCalledTimes(0);

        observer.runListener('event1');

        expect(callback1).toHaveBeenCalledTimes(1);
        expect(callback2).toHaveBeenCalledTimes(1);
        expect(callback3).toHaveBeenCalledTimes(0);

        observer.runListener('event2');

        expect(callback1).toHaveBeenCalledTimes(1);
        expect(callback2).toHaveBeenCalledTimes(1);
        expect(callback3).toHaveBeenCalledTimes(1);
    });

    it('Should del listener', () => {
        const callback1 = jest.fn();
        const callback2 = jest.fn();
        const callback3 = jest.fn();
        const observer = new Observer<{ event1: []; foobar: []; }>()
            .addListener('event1', callback1)
            .delListener('event1', callback1)
            .addListener('event1', callback1)
            .addListener('event1', callback2)
            .delListener('event1', callback1)
            .addListener('event1', callback3)
            .delListener('foobar', callback3);

        observer.runListener('event1');

        expect(callback1).toHaveBeenCalledTimes(0);
        expect(callback2).toHaveBeenCalledTimes(1);
        expect(callback3).toHaveBeenCalledTimes(1);
    });

    it('Should has listener', () => {
        const callback1 = jest.fn();
        const observer = new Observer<{ event1: []; event2: []; }>()
            .addListener('event1', callback1);

        expect(observer.hasListener('event1', callback1)).toBe(true);
        expect(observer.hasListener('event1', jest.fn())).toBe(false);
        expect(observer.hasListener('event2', callback1)).toBe(false);
    });

    it('Should has any listener', () => {
        const callback1 = jest.fn();
        const observer = new Observer<{ event1: []; event2: []; }>()
            .addListener('event1', callback1);

        expect(observer.anyListener('event1')).toBe(true);
        expect(observer.anyListener('event2')).toBe(false);
    });

    it('Should support args', () => {
        const callback1 = jest.fn();
        const observer = new Observer<{ event1: number[]; }>()
            .addListener('event1', callback1);

        observer.runListener('event1', 1, 2, 3);

        expect(callback1).toHaveBeenCalledWith(1, 2, 3);
    });
});
