import { ApphostContext } from '@yandex-int/frontend-apphost-context';
import { processLoaderRedirect, processContextLoaderRedirect, ContextRoute } from './index';

const apphostContext = {
    setResponseHeader: () => {},
    setResponseStatus: () => {},
} as unknown as ApphostContext;

describe('processLoaderRedirect', () => {
    it('should not redirect request', () => {
        expect(processLoaderRedirect(apphostContext, {})).toBe(false);
    });

    it('should not redirect request for google.com', () => {
        expect(processLoaderRedirect(apphostContext, { host: 'google.com' })).toBe(false);
    });

    it('should redirect request for yandex.ru domain', () => {
        expect(processLoaderRedirect(apphostContext, { host: 'yandex.ru' })).toBe(true);
    });

    it('should redirect request for yastatic.net domain', () => {
        expect(processLoaderRedirect(apphostContext, { host: 'yastatic.net' })).toBe(true);
    });

    it('should redirect request for an.yandex.ru domain', () => {
        expect(processLoaderRedirect(apphostContext, { host: 'an.yandex.ru' })).toBe(true);
    });
});

describe('processCommonLoaderRedirect', () => {
    it('should not redirect request', () => {
        expect(processContextLoaderRedirect(apphostContext, {}, ContextRoute.CONTEXT)).toBe(false);
    });

    it('should not redirect request for google.com', () => {
        expect(processContextLoaderRedirect(apphostContext, { host: 'google.com' }, ContextRoute.CONTEXT)).toBe(false);
    });

    it('should not redirect request for yandex.ru domain', () => {
        expect(processContextLoaderRedirect(apphostContext, { host: 'yandex.ru' }, ContextRoute.CONTEXT)).toBe(false);
    });

    it('should not redirect request for yastatic.net domain', () => {
        expect(processContextLoaderRedirect(apphostContext, { host: 'yastatic.net' }, ContextRoute.CONTEXT)).toBe(false);
    });

    it('should redirect request for an.yandex.ru domain', () => {
        expect(processContextLoaderRedirect(apphostContext, { host: 'an.yandex.ru' }, ContextRoute.CONTEXT)).toBe(true);
    });
});
