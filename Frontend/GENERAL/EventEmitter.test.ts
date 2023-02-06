import {
    EventEmitter,
} from '.';

describe('EventEmitter', () => {
    describe('#.eventNames()', () => {
        test('нет событий', () => {
            const ee = new EventEmitter();

            expect(ee.eventNames()).toEqual([]);
        });

        test('три события', () => {
            interface EE {
                load: [];
                error: [];
                progress: [];
                abort: [];
            }

            const ee = new EventEmitter<EE>();

            ee.on('load', () => {});
            ee.on('error', () => {});
            ee.on('progress', () => {});

            expect(ee.eventNames()).toEqual(['load', 'error', 'progress']);
        });
    });

    describe('#.listeners(eventName)', () => {
        test('нет слушателей', () => {
            interface EE {
                message: [];
            }

            const ee = new EventEmitter<EE>();

            expect(ee.listeners('message')).toEqual([]);
        });

        test('два слушателя', () => {
            interface EE {
                message: [];
            }

            const ee = new EventEmitter<EE>();

            const onMessage = () => {};

            ee.on('message', onMessage);
            ee.on('message', onMessage);

            expect(ee.listeners('message')).toEqual([onMessage, onMessage]);
        });
    });

    describe('#.on(eventName, listener)', () => {
        test('порядок слушателей', () => {
            interface EE {
                message: [number, string];
            }

            const ee = new EventEmitter<EE>();

            const onMessage1 = jest.fn((_arg1, _arg2) => {});
            const onMessage2 = jest.fn((_arg1, _arg2) => {});
            const onMessage3 = jest.fn((_arg1, _arg2) => {});

            ee.on('message', onMessage1);
            ee.on('message', onMessage2);
            ee.on('message', onMessage3);

            expect(ee.eventNames()).toEqual(['message']);
            expect(ee.listeners('message')).toEqual([onMessage1, onMessage2, onMessage3]);

            ee.emit('message', 3, 'abc');

            expect(onMessage1.mock.calls[0]).toEqual([3, 'abc']);
            expect(onMessage2.mock.calls[0]).toEqual([3, 'abc']);
            expect(onMessage3.mock.calls[0]).toEqual([3, 'abc']);

            ee.emit('message', 7, 'cba');

            expect(onMessage1.mock.calls[1]).toEqual([7, 'cba']);
            expect(onMessage2.mock.calls[1]).toEqual([7, 'cba']);
            expect(onMessage3.mock.calls[1]).toEqual([7, 'cba']);

            expect(onMessage1.mock.calls.length).toBe(2);
            expect(onMessage2.mock.calls.length).toBe(2);
            expect(onMessage3.mock.calls.length).toBe(2);
        });

        test('должен вернуть EventEmitter', () => {
            interface EE {
                message: [];
            }

            const ee = new EventEmitter<EE>();

            expect(ee.on('message', () => {})).toBe(ee);
        });
    });

    describe('#.off(eventName, listener)', () => {
        test('два события', () => {
            interface EE {
                message: number;
                error: [string, boolean];
                load: [];
            }

            const ee = new EventEmitter<EE>();

            const onMessage1 = () => {};
            const onMessage2 = () => {};
            const onError = () => {};

            ee.on('message', onMessage1);
            ee.on('message', onMessage2);
            ee.on('error', onError);
            ee.on('message', onMessage1);
            ee.on('error', onError);

            expect(ee.eventNames()).toEqual(['message', 'error']);
            expect(ee.listeners('message')).toEqual([onMessage1, onMessage2, onMessage1]);
            expect(ee.listeners('error')).toEqual([onError, onError]);

            ee.off('message', onMessage1);

            expect(ee.listeners('message')).toEqual([onMessage1, onMessage2]);
            expect(ee.listeners('error')).toEqual([onError, onError]);

            ee.off('message', onMessage1);

            expect(ee.listeners('message')).toEqual([onMessage2]);

            ee.off('message', onMessage1);

            expect(ee.listeners('message')).toEqual([onMessage2]);

            ee.off('message', onMessage2);

            expect(ee.eventNames()).toEqual(['error']);
            expect(ee.listeners('error')).toEqual([onError, onError]);

            ee.off('error', onError);
            expect(ee.listeners('error')).toEqual([onError]);

            ee.off('error', onError);
            expect(ee.eventNames()).toEqual([]);
        });

        test('должен вернуть EventEmitter', () => {
            interface EE {
                message: [];
            }

            const ee = new EventEmitter<EE>();

            expect(ee.off('message', () => {})).toBe(ee);
        });
    });

    describe('#.removeAllListeners([eventName])', () => {
        test('все события', () => {
            interface EE {
                message: [];
                error: [];
                load: [];
            }

            const ee = new EventEmitter<EE>();

            ee.on('message', () => {});
            ee.on('error', () => {});
            ee.on('load', () => {});

            expect(ee.eventNames()).toEqual(['message', 'error', 'load']);

            ee.removeAllListeners();

            expect(ee.eventNames()).toEqual([]);
        });

        test('одно событие', () => {
            interface EE {
                message: [];
                error: [];
            }

            const ee = new EventEmitter<EE>();

            const onMessage = () => {};
            const onError = () => {};

            ee.on('message', onMessage);
            ee.on('error', onError);
            ee.on('message', onMessage);

            expect(ee.eventNames()).toEqual(['message', 'error']);
            expect(ee.listeners('message')).toEqual([onMessage, onMessage]);
            expect(ee.listeners('error')).toEqual([onError]);

            ee.removeAllListeners('message');

            expect(ee.eventNames()).toEqual(['error']);
            expect(ee.listeners('error')).toEqual([onError]);
        });

        test('должен вернуть EventEmitter', () => {
            const ee = new EventEmitter();

            expect(ee.removeAllListeners()).toBe(ee);
        });
    });

    describe('#.emit(eventName, [...args])', () => {
        test('разные аргументы', () => {
            interface EE {
                abort: [];
                error: string;
                load: [number];
                timeout: [boolean, string];
                progress: [number, string, boolean, string];
            }

            const ee = new EventEmitter<EE>();

            const onAbort = jest.fn(() => {});
            const onError = jest.fn(_arg => {});
            const onLoad = jest.fn(_arg => {});
            const onTimeout = jest.fn((_arg1, _arg2) => {});
            const onProgress = jest.fn((_arg1, _arg2, _arg3, _arg4) => {});

            ee.on('abort', onAbort);
            ee.on('error', onError);
            ee.on('load', onLoad);
            ee.on('timeout', onTimeout);
            ee.on('progress', onProgress);

            ee.emit('abort');
            ee.emit('error', 'abc');
            ee.emit('load', 123);
            ee.emit('timeout', true, 'tmt');
            ee.emit('progress', 321, 'prs', false, 'srp');

            expect(onAbort.mock.calls[0]).toEqual([]);
            expect(onError.mock.calls[0]).toEqual(['abc']);
            expect(onLoad.mock.calls[0]).toEqual([123]);
            expect(onTimeout.mock.calls[0]).toEqual([true, 'tmt']);
            expect(onProgress.mock.calls[0]).toEqual([321, 'prs', false, 'srp']);

            ee.emit('timeout', false, 'qwe');

            expect(onTimeout.mock.calls[1]).toEqual([false, 'qwe']);

            ee.emit('abort');
            ee.emit('abort');

            expect(onAbort.mock.calls.length).toBe(3);
            expect(onError.mock.calls.length).toBe(1);
            expect(onLoad.mock.calls.length).toBe(1);
            expect(onTimeout.mock.calls.length).toBe(2);
            expect(onProgress.mock.calls.length).toBe(1);
        });

        test('слушатели удаляющие события', () => {
            interface EE {
                message: [];
            }

            const ee = new EventEmitter<EE>();

            const onMessage2 = jest.fn(() => ee.removeAllListeners('message'));
            const onMessage1 = jest.fn(() => ee.off('message', onMessage2));
            const onMessage3 = jest.fn(() => ee.removeAllListeners());

            ee.on('message', onMessage1);
            ee.on('message', onMessage2);
            ee.on('message', onMessage3);

            expect(ee.listeners('message')).toEqual([onMessage1, onMessage2, onMessage3]);

            ee.emit('message');

            expect(onMessage1.mock.calls.length).toBe(1);
            expect(onMessage2.mock.calls.length).toBe(1);
            expect(onMessage3.mock.calls.length).toBe(1);

            expect(ee.listeners('message')).toEqual([]);

            ee.emit('message');

            expect(onMessage1.mock.calls.length).toBe(1);
            expect(onMessage2.mock.calls.length).toBe(1);
            expect(onMessage3.mock.calls.length).toBe(1);
        });
    });
});
