import { getComponentPaths, normalizePath } from './path';

const RU_COMPONENTS =
    'jobs/yandex.ru/jobs/components/header,jobs/yandex.ru/jobs/components/footer,jobs/yandex.ru/jobs/components/404,jobs/yandex.ru/jobs/some/component/1';
const COM_COMPONENTS =
    'jobs/yandex.ru/jobs/_com/components/header,jobs/yandex.ru/jobs/_com/components/footer,jobs/yandex.ru/jobs/_com/components/404,jobs/yandex.ru/jobs/_com/some/component/1';

describe('lib/lpc/path', () => {
    describe('normalizePath', () => {
        it('should remove invalid symbols', () => {
            const normalized = normalizePath('"normalized&!@#$%^&*()+:;\\|<>Path[]{}');

            expect(normalized).toBe('normalizedPath');
        });

        it('should remove trailing slash', () => {
            const normalized = normalizePath('some/path/');

            expect(normalized).toBe('some/path');
        });

        it('should normalize path', () => {
            const normalized = normalizePath('./../some//path/../path');

            expect(normalized).toBe('../some/path');
        });
    });

    describe('getComponentPaths', () => {
        let components: string[];

        beforeEach(() => {
            components = [
                'some/component/1',
            ];
        });

        it('should use com cluster with com zone', () => {
            const paths = getComponentPaths(components, 'com');

            expect(paths).toBe(COM_COMPONENTS);
        });

        it('should preserve order of components (common and page components)', () => {
            const paths = getComponentPaths(components, 'ru');

            expect(paths).toBe(RU_COMPONENTS);
        });

        it('should normalize paths', () => {
            const components = [
                'some/component&!/2/../1/',
            ];

            const paths = getComponentPaths(components, 'ru');

            expect(paths).toBe(RU_COMPONENTS);
        });
    });
});
