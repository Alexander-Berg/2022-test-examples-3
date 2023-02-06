import { createContext } from '@yandex-turbo/applications/health/utils/context';

describe('Health/createContext', () => {
    const { provider, consumer } = createContext<string>();

    it('Корректно возвращает результат ф-ии', () => {
        const result = provider('v1')(() => 'someValue');

        expect(result).toBe('someValue');
    });

    it('Корректно отрабатывает получение значения', () => {
        provider('v1')(() => f());

        function f() {
            const value = consumer();
            expect(value).toBe('v1');
        }
    });

    it('Корректно отрабатывает получение значения, второй вызов', () => {
        provider('v1')(() => null);
        provider('v2')(() => f());

        function f() {
            const value = consumer();
            expect(value).toBe('v2');
        }
    });

    it('Корректно отрабатывает получение значения после переопределения', () => {
        provider('v1')(() => f1());

        function f1() {
            provider('v2')(() => f2());
        }

        function f2() {
            const value = consumer();
            expect(value).toBe('v2');
        }
    });

    it('После завершения ф-ии, контекст очищается', () => {
        provider('v1')(() => null);

        const value = consumer();
        expect(value).toBe(undefined);
    });

    it('После падения ф-ии, контекст очищается', () => {
        try {
            provider('v1')(() => f());
        } catch (e) {}

        function f() {
            throw new Error('some error');
        }

        const value = consumer();
        expect(value).toBe(undefined);
    });
});
