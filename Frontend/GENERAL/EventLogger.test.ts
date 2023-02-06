import { EventLogger, loggerDataType } from './EventLogger';
import { IUtilContextExtended } from '../../../../typings/apphost';

describe('EventLogger', () => {
    const loggerFn = jest.fn((data: loggerDataType) => data);
    const util = {
        getLog: () => loggerFn,
    } as unknown as IUtilContextExtended;
    const emptyCreateData = {
        meta: null,
        ssr_meta: null,
        bidReqId: null,
        format: null,
        blockId: null,
        pcodever: null,
        requestId: null,
        product_type: null,
        product_type_meta: null,
        is_adfox: false,
        is_native: false,
        is_aab: false,
        is_ssp: false,
        is_sdk: null,
        device: 'desktop',
        width: null,
        height: null,
        is_ssr: false,
        timing: 0,
        route: null
    };

    beforeEach(() => {
        EventLogger.clearInstance();
        loggerFn.mockClear();
    });

    it('valid used singleton', () => {
        const el = EventLogger.createInstance(util);
        const el2 = EventLogger.getInstance();
        const el3 = EventLogger.createInstance(util);

        expect(el).toEqual(el2);
        expect(el).toEqual(el3);
        expect(el3).toEqual(el2);
    });

    it('empty params on constructor', () => {
        const el = EventLogger.createInstance(util);
        el.sendEvent();
        let loggedData = loggerFn.mock.results[0].value;

        // вычищаем unixtime - он всегда разный
        loggedData = JSON.parse(loggedData);
        delete loggedData.unixtime;
        loggedData = JSON.stringify(loggedData);

        expect(loggedData).toEqual(JSON.stringify(emptyCreateData));
    });

    it('some add params', () => {
        const addedData = { aaa: 123 };
        const addedData2 = {
            a2: { a3: 123 },
            a4: 1.4,
            a5: '123',
            a6: [1, 2, 3]
        };
        const el = EventLogger.createInstance(util);
        el.addParams(addedData);
        el.addParams(addedData2);
        el.sendEvent();
        let loggedData = loggerFn.mock.results[0].value;

        // вычищаем unixtime - он всегда разный
        loggedData = JSON.parse(loggedData);
        delete loggedData.unixtime;
        loggedData = JSON.stringify(loggedData);

        expect(loggedData).toEqual(JSON.stringify({
            ...addedData,
            ...addedData2,
            ...emptyCreateData
        }));
    });

    it('without instance', () => {
        const el = EventLogger.getInstance();
        const addedData = { aaa: 123 };
        let exception = false;

        try {
            el.addParams(addedData);
        } catch {
            exception = true;
        }

        expect(exception).toEqual(false);
    });
});
