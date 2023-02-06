import { Optional, isValuePresent } from './optional';


describe('isValuePresent', () => {
    it('should return false if a value is null or undefined', () => {
        expect(isValuePresent(null)).toStrictEqual(false);
        expect(isValuePresent(undefined)).toStrictEqual(false);
    });

    it('should return true if a value is not null or undefined', () => {
        expect(isValuePresent({})).toStrictEqual(true);
    });
});


describe('Optional', () => {
    describe('of static method', () => {
        it('of static method should return an instance of an Optional class', () => {
            const instance = Optional.of({});
            expect(instance).toBeInstanceOf(Optional);
        });
    });

    describe('get instance method', () => {
        it('should return a value', () => {
            const value = {};
            const instance = Optional.of(value);
            expect(instance.get()).toStrictEqual(value);
        });

        it('should throw an error if a value is null or undefined', () => {
            const instanceNull = Optional.of(null);
            expect(() => instanceNull.get()).toThrowError('Optional data is null or undefined!');

            const instanceUndefined = Optional.of(undefined);
            expect(() => instanceUndefined.get()).toThrowError('Optional data is null or undefined!');
        });
    });

    describe('orElse instance method', () => {
        it('should return a value', () => {
            const value = {};
            const instance = Optional.of(value);
            expect(instance.orElse([])).toStrictEqual(value);
        });

        it('should return a default value if a value is null or undefined', () => {
            const defaultValue = {};

            const instanceNull = Optional.of<typeof defaultValue>(null);
            expect(instanceNull.orElse(defaultValue)).toStrictEqual(defaultValue);

            const instanceUndefined = Optional.of<typeof defaultValue>(undefined);
            expect(instanceUndefined.orElse(defaultValue)).toStrictEqual(defaultValue);
        });
    });

    describe('map instance method', () => {
        const mapper = jest.fn();

        it('should return a new instance with mapped value', () => {
            const value = {};
            const instance = Optional.of(value);
            const mappedValue = 0;

            mapper.mockImplementation(() => mappedValue);

            const newInstance = instance.map(mapper);

            expect(mapper).toBeCalledWith(value);
            expect(newInstance).not.toStrictEqual(instance);
            expect(newInstance.get()).toStrictEqual(mappedValue);
        });
    });

    describe('isPresent instance method', () => {
        it('should return false if a value is null or undefined', () => {
            const instanceNull = Optional.of(null);
            expect(instanceNull.isPresent()).toStrictEqual(false);

            const instanceUndefined = Optional.of(undefined);
            expect(instanceUndefined.isPresent()).toStrictEqual(false);
        });

        it('should return true otherwise', () => {
            const instance = Optional.of({});
            expect(instance.isPresent()).toStrictEqual(true);
        });
    });

});
