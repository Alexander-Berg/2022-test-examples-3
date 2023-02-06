import { TraceLogger, loggerDataType } from './TraceLogger';
import { IUtilContextExtended } from '../../../../typings/apphost';

describe('TraceLogger', () => {
    const loggerFn = jest.fn((data: loggerDataType) => data);
    const util = {
        getLog: () => loggerFn,
    } as unknown as IUtilContextExtended;

    beforeEach(() => {
        TraceLogger.clearInstance();
        loggerFn.mockClear();
    });

    it('valid used singleton', () => {
        const tl = TraceLogger.createInstance(util);
        const tl2 = TraceLogger.getInstance();
        const tl3 = TraceLogger.createInstance(util);

        expect(tl).toEqual(tl2);
        expect(tl).toEqual(tl3);
        expect(tl3).toEqual(tl2);
    });

    it('try send csr event', () => {
        const testData = { test: 123 };
        const expectedData = { test: 123, is_ssr: false };

        const tl = TraceLogger.createInstance(util);
        tl.sendCsrEvent(testData);
        let loggedData = loggerFn.mock.results[0].value;

        // вычищаем unixtime - он всегда разный
        loggedData = JSON.parse(loggedData);
        delete loggedData.unixtime;
        loggedData = JSON.stringify(loggedData);

        expect(loggedData).toEqual(JSON.stringify(expectedData));
    });

    it('try send ssr event', () => {
        const testData = { test: 123 };
        const expectedData = { test: 123, is_ssr: true };

        const tl = TraceLogger.createInstance(util);
        tl.sendSsrEvent(testData);
        let loggedData = loggerFn.mock.results[0].value;

        // вычищаем unixtime - он всегда разный
        loggedData = JSON.parse(loggedData);
        delete loggedData.unixtime;
        loggedData = JSON.stringify(loggedData);

        expect(loggedData).toEqual(JSON.stringify(expectedData));
    });

    it('without instance', () => {
        const el = TraceLogger.getInstance();
        const addedData = { aaa: 123 };
        let exception = false;

        try {
            el.sendCsrEvent(addedData);
        } catch {
            exception = true;
        }

        try {
            el.sendSsrEvent(addedData);
        } catch {
            exception = true;
        }

        expect(exception).toEqual(false);
    });
});
