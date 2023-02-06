/* eslint-disable import/first */
jest.mock('uuid/v1', () => ({ default: () => 'test' }));
jest.mock('../routes/stand', () => () => {});
jest.mock('../../core/utils/frontendConfig/index.ts');
jest.mock('../../core/params/chatRestrictions');
jest.mock('../../core/metrika');

import { IUtilContext } from '@yandex-int/frontend-apphost-context';
import { getMain } from '../template';

describe('Report renderer', () => {
    describe('#getMain', () => {
        it('Should not redirect if request from iframe', () => {
            const data = {};
            const mockRequest = {
                uri:
                    'https://yandex.ru/chat?build=chamb&parentOrigin=https://yandex.ru',
                params: {
                    build: ['chamb'],
                    parentOrigin: ['https://yandex.ru/chat'],
                },
                hostname: 'yandex.ru',
                path: '',
                headers: { 'x-yandex-https': 'yes' },
            };

            const ctxMock = {
                findLastItem: jest.fn((item) => {
                    switch (item) {
                        case 'request':
                            return mockRequest;
                        case 'access':
                            return {};
                        default:
                            return;
                    }
                }),
                setResponseStatus: jest.fn(),
                setResponseHeader: jest.fn(),
            } as any;

            const util = {
                setDebugMeta: jest.fn(),
                reportError: jest.fn(),
                getTemplatesState: jest.fn(),
            } as unknown as IUtilContext;

            getMain(util)(data, ctxMock);

            expect(ctxMock.setResponseStatus).not.toBeCalledWith(302);
        });

        it('Should redirect to non-chamb build if request not from iframe', () => {
            const data = {};
            const mockRequest = {
                uri: 'https://q.yandex-team.ru/?build=chamb',
                params: {
                    build: ['chamb'],
                },
                hostname: 'q.yandex-team.ru',
                path: '',
                headers: { 'x-yandex-https': 'yes' },
            };

            const ctxMock = {
                findLastItem: jest.fn((item) => {
                    switch (item) {
                        case 'request':
                            return mockRequest;
                        default:
                            return;
                    }
                }),
                setResponseStatus: jest.fn(),
                setResponseHeader: jest.fn(),
            } as any;

            const util = {
                setDebugMeta: jest.fn(),
                reportError: jest.fn(),
            } as unknown as IUtilContext;

            getMain(util)(data, ctxMock);

            expect(util.reportError).not.toBeCalled();
            expect(ctxMock.setResponseStatus).toBeCalledWith(302);
            expect(ctxMock.setResponseHeader).toBeCalledWith(
                'Location',
                '/',
            );
        });

        it('Should save chat id while redirect from chamb', () => {
            const data = {};
            const mockRequest = {
                uri: 'https://yandex.ru/chat?build=chamb&guid=123123',
                params: {
                    build: ['chamb'],
                    guid: ['123123'],
                },
                hostname: 'yandex.ru',
                path: 'chat',
                headers: { 'x-yandex-https': 'yes' },
            };

            const ctxMock = {
                findLastItem: jest.fn((item) => {
                    switch (item) {
                        case 'request':
                            return mockRequest;
                        default:
                            return;
                    }
                }),
                setResponseStatus: jest.fn(),
                setResponseHeader: jest.fn(),
            } as any;

            const util = {
                setDebugMeta: jest.fn(),
                reportError: jest.fn(),
            } as unknown as IUtilContext;

            getMain(util)(data, ctxMock);

            expect(util.reportError).not.toBeCalled();
            expect(ctxMock.setResponseStatus).toBeCalledWith(302);
            expect(ctxMock.setResponseHeader).toBeCalledWith(
                'Location',
                '/chat#/user/123123',
            );
        });
    });
});
