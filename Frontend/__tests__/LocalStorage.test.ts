import { LocalStorage, SafetyLocalStorage, assertionFactory } from '../LocalStorage';

describe('LocalStorage', () => {
    afterEach(() => {
        SafetyLocalStorage.setLogger(() => {
            // do nothing
        });
        LocalStorage.clear();
    });

    describe('#assertionFactory', () => {
        it('notAvailable should be true', () => {
            const loggerFn = jest.fn();

            SafetyLocalStorage.setLogger(loggerFn);

            expect(LocalStorage.getJsonItem('item1')).toBeNull();

            const fn = jest.fn(() => {
                throw new Error();
            });

            const assertion = assertionFactory({
                localStorage: {
                    setItem: fn,
                } as any,
            });

            expect(assertion.notAvailable).toBeTruthy();
            expect(assertion.notAvailable).toBeTruthy();
            expect(fn).toBeCalledTimes(1);
            expect(loggerFn).toBeCalledTimes(1);
        });

        it('notAvailable should be false', () => {
            const assertion = assertionFactory(window);

            expect(assertion.notAvailable).toBeFalsy();
        });
    });

    describe('#key', () => {
        it('should be item1', () => {
            LocalStorage.setItem('item1', 'test');
            LocalStorage.setItem('item2', 'test');

            expect(LocalStorage.key(0)).toBe('item1');
        });
    });

    describe('#clear', () => {
        it('should be cleared', () => {
            LocalStorage.setItem('item1', 'test');
            LocalStorage.setItem('item2', 'test');

            expect(LocalStorage.length).toBe(2);

            LocalStorage.clear();

            expect(LocalStorage.length).toBe(0);
        });
    });

    describe('#setItem', () => {
        it('item should be set', () => {
            LocalStorage.setItem('item1', 'test');

            expect(LocalStorage.getItem('item1')).toBe('test');
        });
    });

    describe('#getJsonItem', () => {
        it('should be default value', () => {
            expect(LocalStorage.getItem('item1', 'test')).toMatch('test');
        });

        it('should be null', () => {
            expect(LocalStorage.getJsonItem('item1')).toBeNull();
        });
    });

    describe('#setJsonItem', () => {
        it('json should be set as properly', () => {
            const item = {
                test: 'ok',
            };

            LocalStorage.setJsonItem('item1', item);

            expect(LocalStorage.getJsonItem('item1')).toMatchObject(item);
        });

        it('should execute passed callback with previous value', () => {
            const data = { foo: 'bar' };

            LocalStorage.setJsonItem('k', data);

            const newData = Object.assign({}, data, { foo: 'not bar' });
            const callback = jest.fn().mockReturnValue(newData);

            LocalStorage.setJsonItem('k', callback);

            expect(callback).toBeCalledWith(expect.objectContaining(data));
            expect(LocalStorage.getJsonItem('k')).toEqual(newData);
        });
    });

    describe('#getJsonItem', () => {
        it('should be default value', () => {
            const item = {
                test: 'ok',
            };

            expect(LocalStorage.getJsonItem('item1', item)).toMatchObject(item);
        });

        it('should be null', () => {
            expect(LocalStorage.getJsonItem('item1')).toBeNull();
        });

        it('should be logged and null', () => {
            const fn = jest.fn();

            SafetyLocalStorage.setLogger(fn);
            LocalStorage.setItem('item1', '{asdasd}');

            expect(LocalStorage.getJsonItem('item1')).toBeNull();
            expect(fn).toBeCalledTimes(1);
        });
    });
});
