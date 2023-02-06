import { LoadPageManager } from '../LoadPageManager';

describe('LoadPageManager', () => {
    describe('extractID()', () => {
        it('Устанавливает нулевую страницу по умолчанию для абсолютного пути', () => {
            expect(
                // @ts-ignore private method
                LoadPageManager.extractID('https://tenorok-1-ws3.tunneler-si.yandex.ru/turbo?text=technopark.ru/yandexturbocatalog/')
            ).toBe('technopark.ru/yandexturbocatalog/0');
        });

        it('Устанавливает нулевую страницу по умолчанию для относительного пути', () => {
            expect(
                // @ts-ignore private method
                LoadPageManager.extractID('/turbo?text=technopark.ru/yandexturbocatalog/')
            ).toBe('technopark.ru/yandexturbocatalog/0');
        });

        it('Устанавливает страницу из адреса', () => {
            expect(
                // @ts-ignore private method
                LoadPageManager.extractID('/turbo?text=technopark.ru/yandexturbocatalog/&page=1')
            ).toBe('technopark.ru/yandexturbocatalog/1');
        });

        it('Считывает параметр stub, если отсутствует text', () => {
            expect(
                // @ts-ignore private method
                LoadPageManager.extractID('/turbo?stub=technopark.ru/yandexturbocatalog/&page=2')
            ).toBe('technopark.ru/yandexturbocatalog/2');
        });
    });
});
