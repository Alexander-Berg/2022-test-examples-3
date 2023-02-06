import { OEBS_TREE } from '~/src/features/ServiceTree/redux/ServiceTree.actions';
import { getSampleOebsTree, getNotFoundError } from '~/test/jest/mocks/data/service-tree';
import { render } from './service-tree.po';

describe('Дерево OEBS сервисов', () => {
    it('1. Ожидание загрузки дерева сервисов', () => {
        // - do: открыть страницу дерева OEBS (/embed/oebs/tree/1)
        // рендерим компонент дерева OEBS
        const serviceTree = render({
            loadingReducer: { [OEBS_TREE]: true },
        });

        // - assert: отображается спиннер
        expect(serviceTree.spinner?.container).toBeInTheDocument();
    });

    it('2. Отображение дерева сервисов', () => {
        // - do: открыть страницу дерева OEBS (/embed/oebs/tree/1)
        // рендерим компонент дерева OEBS
        const serviceTree = render({
            serviceTree: { services: getSampleOebsTree() },
        });

        // - assert: отображаютя 4 сервиса
        expect(serviceTree.services.map(x => x.name)).toEqual(['ABC-1', 'ABC-2', 'ABC-3', 'ABC-4']);
    });

    it('3. Проверка ссылки', () => {
        // - do: открыть страницу дерева OEBS (/embed/oebs/tree/1)
        // рендерим компонент дерева OEBS
        const serviceTree = render({
            serviceTree: { services: getSampleOebsTree() },
        });

        // - do: находим сервис с именем АВС-1
        const service = serviceTree.services.find(x => x.name === 'ABC-1');

        // - assert: url на сервис
        expect(service?.url).toEqual('/services/abc-1/');
    });

    it('4. Открытие ката', () => {
        // - do: открыть страницу дерева OEBS (/embed/oebs/tree/1)
        // рендерим компонент дерева OEBS
        const serviceTree = render({
            serviceTree: { services: getSampleOebsTree() },
        });

        // - do: находим сервис с именем АВС-3
        const service = serviceTree.services.find(x => x.name === 'ABC-3');

        // - assert: дополнительные поля скрыты
        expect(service?.cut.isOpen).toBeFalsy();

        // - do: раскрываем кат
        service?.cut.click();

        // - assert: дополнительные поля раскрыты
        expect(service?.cut.isOpen).toBeTruthy();
    });

    it('5. Отображение ошибки', () => {
        // - do: открыть страницу дерева OEBS (/embed/oebs/tree/1)
        // рендерим компонент дерева OEBS
        const serviceTree = render({
            errorReducer: { [OEBS_TREE]: getNotFoundError() },
        });

        // - assert: отображается сообщение об ошибке
        expect(serviceTree.error?.message).toEqual('Not found.');
    });
});
