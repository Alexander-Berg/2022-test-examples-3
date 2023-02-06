import stout from '@yandex-market/stout';
import type {Page} from 'puppeteer';
import {promisifyResponse} from '@yandex-market/mandrel/lib/helpers';
import type {Reporter} from 'jest-allure/src/Reporter';

import {serverInit} from './mocks/luster';
import {setBackendHandler, BackendMocks, BackendHandler} from './mocks/base/backendHandlers';
import {defaultBackendHandler} from './defaultBackendHandler';

import '../../../app';

export {setBackendHandler, getBackendHandler, BackendResponse, BackendMocks} from './mocks/base/backendHandlers';
export {
    default as blackboxMockResponse,
    CURRENT_USER_ID,
    CURRENT_USER_LOGIN,
} from './defaultBackendHandler/mockResponse/blackbox';

export type MockServer = {
    getHostName: () => string;
    initialize: (
        backendHandlerSetter: typeof setBackendHandler,
        stopServer: () => Promise<void>,
        hostName: string,
        defaultHandler: BackendHandler,
    ) => Promise<MockServer>;
    openApp: (
        url: string,
        setMocksCallback?: BackendMocks,
        params?: {
            resolution?: {width: number; height: number};
            waitUntil: 'load' | 'domcontentloaded' | 'networkidle0' | 'networkidle2';
        },
    ) => Promise<Page>;
};

export const stopServer = () => {
    const resp = stout.stopServer();

    return promisifyResponse(resp);
};

const testIds: Record<string, string> = {};
export const testId = (id: string) => {
    const {currentTestName} = expect.getState();

    testIds[currentTestName] = id;
};

export const getTestId = (): string => {
    const {currentTestName} = expect.getState();

    return testIds[currentTestName];
};

type GlobalObject = {mockServer: MockServer; reporter: Reporter; stopServer?: () => Promise<void>};

export const getReporter = (): Reporter => ((global as unknown) as GlobalObject).reporter;

export const setTestId = (id: string) => {
    testId(id);
    getReporter().testId(`https://testpalm.yandex-team.ru/testcase/${id}`);
};

export const step = async <T>(name: string, stepFn: () => Promise<T>): Promise<T> => {
    getReporter().startStep(name);
    const result = await stepFn();
    getReporter().endStep();

    return result;
};

export const startServer = (): Promise<MockServer> => {
    ((global as unknown) as GlobalObject).stopServer = stopServer;

    return serverInit.then(() => {
        const {mockServer} = (global as unknown) as GlobalObject;
        const hostName = `http://localhost:${stout?.data?.config?.server?.port}`;

        return mockServer.initialize(setBackendHandler, stopServer, hostName, defaultBackendHandler);
    });
};
