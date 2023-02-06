import * as ajax from '@yandex-turbo/core/ajax';
import { IExtendedResponse } from '@yandex-turbo/core/ajax';
import * as Cookies from 'js-cookie';
import { generateId } from '@yandex-turbo/core/uniq';
import { CallTrackingNumber, getCallTrackingNumber } from '../call-tracking';
import { asMock } from '../as-mock';
import { CallTrackingType } from '../../lcTypes/lcTypes';

jest.mock('@yandex-turbo/core/uniq');
jest.mock('@yandex-turbo/core/ajax');
jest.mock('js-cookie');

describe('getCallTrackingNumber()', () => {
    const calltouchUrl = 'https://call.tr/?js_id=42';
    const coMagicUrl = 'https://call.tr/?sb=42';
    const uisUrl = 'https://call.tr/?sb=42';
    const rostelecomUrl = 'https://call.tr/?sb=42';
    const testRefferer = 'refferer';
    const testUserAgent = 'UserAgent';
    const originalReferrer = document.referrer;
    const originalUserAgent = navigator.userAgent;
    let _getCallTrackingNumber: typeof getCallTrackingNumber;

    beforeEach(() => {
        jest.isolateModules(() => {
            _getCallTrackingNumber = require('../call-tracking').getCallTrackingNumber;
        });
        // @ts-ignore
        window.Ya = {};
        Object.defineProperty(document, 'referrer', { value: testRefferer, configurable: true });
        Object.defineProperty(window.navigator, 'userAgent', { value: testUserAgent, configurable: true });
    });

    afterEach(() => {
        jest.clearAllMocks();
        Object.defineProperty(document, 'referrer', { value: originalReferrer });
        Object.defineProperty(window.navigator, 'userAgent', { value: originalUserAgent });
    });

    it('вызывает ajax.get с верным адресом для Calltouch', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);

        _getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch);

        expect(ajax.get).toHaveBeenCalledWith(
            `${calltouchUrl}&ref=refferer&url=http%3A%2F%2Flocalhost%2F&s_url=http%3A%2F%2Flocalhost%2F&__yandex_turbo_source_origin=http%3A%2F%2Flocalhost%2F`,
            { credentials: 'include' },
        );
        expect(Cookies.get).toHaveBeenCalled();
    });

    it('вызывает ajax.get с верным адресом для CoMagic', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(coMagicUrl, CallTrackingType.CoMagic);

        expect(ajax.get).toHaveBeenCalledWith(
            `${coMagicUrl}&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
        expect(Cookies.get).toHaveBeenCalled();
    });

    it('вызывает ajax.get с верным адресом для UIS', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(uisUrl, CallTrackingType.Uis);

        expect(ajax.get).toHaveBeenCalledWith(
            `${coMagicUrl}&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
        expect(Cookies.get).toHaveBeenCalled();
    });

    it('вызывает ajax.get с верным адресом для Rostelecom', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(rostelecomUrl, CallTrackingType.Rostelecom);

        expect(ajax.get).toHaveBeenCalledWith(
            `${coMagicUrl}&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
        expect(Cookies.get).toHaveBeenCalled();
    });

    it('добавляет _ym_uid из кук в Calltouch', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        // @ts-ignore
        jest.spyOn(Cookies, 'get').mockReturnValue('metrika');

        _getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch);

        expect(Cookies.get).toHaveBeenCalledWith('_ym_uid');
        expect(ajax.get).toHaveBeenCalledWith(
            `${calltouchUrl}&ya_client_id=metrika&ref=refferer&url=http%3A%2F%2Flocalhost%2F&s_url=http%3A%2F%2Flocalhost%2F&__yandex_turbo_source_origin=http%3A%2F%2Flocalhost%2F`,
            { credentials: 'include' },
        );
    });

    it('добавляет _ym_uid из кук в CoMagic', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        // @ts-ignore
        jest.spyOn(Cookies, 'get').mockReturnValue('metrika');
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(coMagicUrl, CallTrackingType.CoMagic);

        expect(Cookies.get).toHaveBeenCalledWith('_ym_uid');
        expect(ajax.get).toHaveBeenCalledWith(
            `${coMagicUrl}&ci=yandex-uniq-id.metrika&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
    });

    it('добавляет _ym_uid из кук в UIS', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        // @ts-ignore
        jest.spyOn(Cookies, 'get').mockReturnValue('metrika');
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(uisUrl, CallTrackingType.Uis);

        expect(Cookies.get).toHaveBeenCalledWith('_ym_uid');
        expect(ajax.get).toHaveBeenCalledWith(
            `${uisUrl}&ci=yandex-uniq-id.metrika&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
    });

    it('добавляет _ym_uid из кук в Rostelecom', () => {
        jest.spyOn(ajax, 'get').mockResolvedValue({ readedBody: undefined } as IExtendedResponse<undefined>);
        // @ts-ignore
        jest.spyOn(Cookies, 'get').mockReturnValue('metrika');
        asMock(generateId).mockReturnValue('123');

        _getCallTrackingNumber(rostelecomUrl, CallTrackingType.Rostelecom);

        expect(Cookies.get).toHaveBeenCalledWith('_ym_uid');
        expect(ajax.get).toHaveBeenCalledWith(
            `${uisUrl}&ci=yandex-uniq-id.metrika&hi=123&ur=http%3A%2F%2Flocalhost%2F&ur2=http%3A%2F%2Flocalhost%2F&referer=refferer&ua=UserAgent`,
            { credentials: 'include' },
        );
    });

    it('кеширует запросы по параметру js_id для Calltouch', () => {
        const res1 = { readedBody: { phoneNumber: 1 } } as IExtendedResponse<{ phoneNumber:number }>;
        const res2 = { readedBody: { phoneNumber: 2 } } as IExtendedResponse<{ phoneNumber:number }>;
        jest.spyOn(ajax, 'get').mockResolvedValueOnce(res1).mockResolvedValue(res2);

        expect(_getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch)).resolves.toMatchObject(res1.readedBody as {});
        expect(_getCallTrackingNumber('https://call.tr/?js_id=43')).resolves.toMatchObject(res2.readedBody as {});
        expect(_getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch)).resolves.toMatchObject(res1.readedBody as {});
    });

    it('кеширует запросы по параметру sb для CoMagic', () => {
        const res1 = { readedBody: { phoneNumber: 1 } } as IExtendedResponse<{ phoneNumber:number }>;
        const res2 = { readedBody: { phoneNumber: 2 } } as IExtendedResponse<{ phoneNumber:number }>;
        jest.spyOn(ajax, 'get').mockResolvedValueOnce(res1).mockResolvedValue(res2);

        expect(_getCallTrackingNumber(coMagicUrl, CallTrackingType.CoMagic)).resolves.toMatchObject(res1.readedBody as {});
        expect(_getCallTrackingNumber('https://call.tr/?sb=43')).resolves.toMatchObject(res2.readedBody as {});
        expect(_getCallTrackingNumber(coMagicUrl, CallTrackingType.CoMagic)).resolves.toMatchObject(res1.readedBody as {});
    });

    it('кеширует запросы по параметру sb для UIS', () => {
        const res1 = { readedBody: { phoneNumber: 1 } } as IExtendedResponse<{ phoneNumber:number }>;
        const res2 = { readedBody: { phoneNumber: 2 } } as IExtendedResponse<{ phoneNumber:number }>;
        jest.spyOn(ajax, 'get').mockResolvedValueOnce(res1).mockResolvedValue(res2);

        expect(_getCallTrackingNumber(uisUrl, CallTrackingType.Uis)).resolves.toMatchObject(res1.readedBody as {});
        expect(_getCallTrackingNumber('https://call.tr/?sb=43')).resolves.toMatchObject(res2.readedBody as {});
        expect(_getCallTrackingNumber(uisUrl, CallTrackingType.Uis)).resolves.toMatchObject(res1.readedBody as {});
    });

    it('кеширует запросы по параметру sb для Rostelecom', () => {
        const res1 = { readedBody: { phoneNumber: 1 } } as IExtendedResponse<{ phoneNumber:number }>;
        const res2 = { readedBody: { phoneNumber: 2 } } as IExtendedResponse<{ phoneNumber:number }>;
        jest.spyOn(ajax, 'get').mockResolvedValueOnce(res1).mockResolvedValue(res2);

        expect(_getCallTrackingNumber(rostelecomUrl, CallTrackingType.Rostelecom)).resolves.toMatchObject(res1.readedBody as {});
        expect(_getCallTrackingNumber('https://call.tr/?sb=43')).resolves.toMatchObject(res2.readedBody as {});
        expect(_getCallTrackingNumber(rostelecomUrl, CallTrackingType.Rostelecom)).resolves.toMatchObject(res1.readedBody as {});
    });

    it('заполняет sessionId если для Calltouch', async() => {
        jest.spyOn(ajax, 'get').mockResolvedValue({
            readedBody: {
                phoneNumber: 'foobar',
                sessionId: '12345',
            },
        } as IExtendedResponse<CallTrackingNumber>);

        await expect(_getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch)).resolves.toMatchObject({
            sessionId: '12345',
        });
    });

    it('заполняет formattedPhoneNumber если нет для Calltouch', async() => {
        jest.spyOn(ajax, 'get').mockResolvedValue({
            readedBody: {
                phoneNumber: 'foobar',
            },
        } as IExtendedResponse<CallTrackingNumber>);

        await expect(_getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch)).resolves.toMatchObject({
            formattedPhoneNumber: 'foobar',
        });
    });

    it('возвращает null при success: false для CoMagic', async() => {
        jest.spyOn(ajax, 'get').mockResolvedValue({
            readedBody: {
                success: false,
            },
        } as IExtendedResponse<CallTrackingNumber>);

        await expect(_getCallTrackingNumber(coMagicUrl, CallTrackingType.CoMagic)).resolves.toBeNull();
    });

    it('возвращает null при success: false для UIS', async() => {
        jest.spyOn(ajax, 'get').mockResolvedValue({
            readedBody: {
                success: false,
            },
        } as IExtendedResponse<CallTrackingNumber>);

        await expect(_getCallTrackingNumber(uisUrl, CallTrackingType.Uis)).resolves.toBeNull();
    });

    it('возвращает null при success: false для Rostelecom', async() => {
        jest.spyOn(ajax, 'get').mockResolvedValue({
            readedBody: {
                success: false,
            },
        } as IExtendedResponse<CallTrackingNumber>);

        await expect(_getCallTrackingNumber(rostelecomUrl, CallTrackingType.Rostelecom)).resolves.toBeNull();
    });

    it('возвращает null при ошибке для Calltouch', async() => {
        jest.spyOn(ajax, 'get').mockRejectedValue(new Error());

        await expect(_getCallTrackingNumber(calltouchUrl, CallTrackingType.Calltouch)).resolves.toBeNull();
    });
});
