import { compose, pipe, pipeRun } from '../composition';

const fn0 = jest.fn((...args) => args.join(' '));
const fn1 = jest.fn((arg) => `${arg} passed`);
const fn2 = jest.fn((arg) => `${arg} and finished`);

describe('pipe', () => {
    it('должен создавать композитную функцию, вызывающую преданные аргументами в порядке слева направо', () => {
        const composed = pipe(fn0, fn1, fn2);

        expect(composed).toBeInstanceOf(Function);

        const result = composed('some', 'arg');

        expect(fn0).toBeCalledWith('some', 'arg');
        expect(fn1).toBeCalledWith('some arg');
        expect(fn2).toBeCalledWith('some arg passed');
        expect(result).toBe('some arg passed and finished');
    });
});

describe('pipeRun', () => {
    it('должен запускать выполнение цепочки функций с заданным сэтом аргументов', () => {
        const result = pipeRun(['some', 'arg'], fn0, fn1, fn2);

        expect(fn0).toBeCalledWith('some', 'arg');
        expect(fn1).toBeCalledWith('some arg');
        expect(fn2).toBeCalledWith('some arg passed');
        expect(result).toBe('some arg passed and finished');
    });
});

describe('compose', () => {
    it('должен создавать композитную функцию, вызывающую преданные аргументами в порядке справа налево', () => {
        const composed = compose(fn2, fn1, fn0);

        expect(composed).toBeInstanceOf(Function);

        const result = composed('some', 'arg');

        expect(fn0).toBeCalledWith('some', 'arg');
        expect(fn1).toBeCalledWith('some arg');
        expect(fn2).toBeCalledWith('some arg passed');
        expect(result).toBe('some arg passed and finished');
    });
});
