const mountExpressMiddlawares = require('../../helpers/mount-express-middlewares');

import { popFnCalls } from '../helpers';

const mockedApp = {
    use: jest.fn()
};

describe('mount-express-middlewares', () => {
    it('должен подключать мидлварь', () => {
        const middlewareFn = () => null;
        const middlewareList = [{
            middleware: () => middlewareFn
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(1);
        expect(useCalls[0].length).toEqual(1);
        expect(useCalls[0][0]).toEqual(middlewareFn);
    });

    it('должен подключать мидлварь с конфигом', () => {
        const middlewareFn = () => null;
        const middlewareList = [{
            middleware: jest.fn(() => middlewareFn),
            config: {
                prop: 'value'
            }
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(1);
        expect(useCalls[0].length).toEqual(1);
        expect(useCalls[0][0]).toEqual(middlewareFn);

        const middlewareCalls = popFnCalls(middlewareList[0].middleware);
        expect(middlewareCalls.length).toEqual(1);
        expect(middlewareCalls[0].length).toEqual(1);
        expect(middlewareCalls[0][0]).toEqual(middlewareList[0].config);
    });

    it('должен подключать мидлварь с указанным путём монтирования (путь монтирования - строка)', () => {
        const middlewareFn = () => null;
        const middlewareList = [{
            mountPath: '/route',
            middleware: () => middlewareFn
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(1);
        expect(useCalls[0].length).toEqual(2);
        expect(useCalls[0][0]).toEqual(middlewareList[0].mountPath);
        expect(useCalls[0][1]).toEqual(middlewareFn);
    });

    it('должен подключать мидлварь с указанным путём монтирования (путь монтирования - регулярка)', () => {
        const middlewareFn = () => null;
        const middlewareList = [{
            mountPath: /\/route[\d]/,
            middleware: () => middlewareFn
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(1);
        expect(useCalls[0].length).toEqual(2);
        expect(useCalls[0][0]).toEqual(middlewareList[0].mountPath);
        expect(useCalls[0][1]).toEqual(middlewareFn);
    });

    it('должен подключать мидлварь с указанным путём монтирования (путь монтирования - массив из строк и регулярок)', () => {
        const middlewareFn = () => null;
        const middlewareList = [{
            mountPath: [
                '/route1',
                /\/[i|d]\/route/,
                'route2',
                /[\d]{2}/
            ],
            middleware: () => middlewareFn
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(4);
        middlewareList[0].mountPath.forEach((mountPath, index) => {
            expect(useCalls[index].length).toEqual(2);
            expect(useCalls[index][0]).toEqual(middlewareList[0].mountPath[index]);
            expect(useCalls[index][1]).toEqual(middlewareFn);
        });
    });

    it('подключение нескольких мидлварей', () => {
        const firstMiddlewareFn = () => null;
        const secondMiddlewareFn = () => null;
        const thirdMiddlewareFn = () => null;
        const middlewareList = [{
            mountPath: '/route1',
            middleware: () => firstMiddlewareFn
        }, {
            mountPath: [
                '/route2',
                /\/route2.+/
            ],
            middleware: jest.fn(() => secondMiddlewareFn),
            config: {
                prop: 'value'
            }
        }, {
            middleware: () => thirdMiddlewareFn
        }];
        mountExpressMiddlawares(mockedApp, middlewareList);
        const useCalls = popFnCalls(mockedApp.use);
        expect(useCalls.length).toEqual(4);

        expect(useCalls[0].length).toEqual(2);
        expect(useCalls[0][0]).toEqual(middlewareList[0].mountPath);
        expect(useCalls[0][1]).toEqual(firstMiddlewareFn);

        expect(useCalls[1].length).toEqual(2);
        expect(useCalls[1][0]).toEqual(middlewareList[1].mountPath[0]);
        expect(useCalls[1][1]).toEqual(secondMiddlewareFn);

        expect(useCalls[2].length).toEqual(2);
        expect(useCalls[2][0]).toEqual(middlewareList[1].mountPath[1]);
        expect(useCalls[2][1]).toEqual(secondMiddlewareFn);

        const secondMiddlewareCalls = popFnCalls(middlewareList[1].middleware);
        expect(secondMiddlewareCalls.length).toEqual(1);
        expect(secondMiddlewareCalls[0][0]).toEqual(middlewareList[1].config);

        expect(useCalls[3].length).toEqual(1);
        expect(useCalls[3][0]).toEqual(thirdMiddlewareFn);
    });
});
