import { getCookie, getYpValue } from './index';

describe('getCookie', () => {
    const prefix = '_ym_uid=16343452114705; _ym_isad=23;';
    const suffix = 'skid=9023401574345485; _ym_visorc_28584306=a';

    let cookieValue = '';
    Object.defineProperty(document, 'cookie', {
        get: jest.fn().mockImplementation(() => cookieValue),

        set: jest.fn().mockImplementation(newValue => {
            cookieValue = newValue;
        }),
    });

    it('yandexuid', () => {
        const curCookie = 'yandexuid=5545342112';
        document.cookie = `${prefix} ${curCookie}; ${suffix}`;
        const cookie = getCookie('yandexuid');
        expect(cookie).toEqual('5545342112');
    });

    it('yexp', () => {
        const curCookie = 'yexp=t1_191142_191140_190951_191043';
        document.cookie = `${curCookie}; ${suffix}`;
        const cookie = getCookie('yexp');
        expect(cookie).toEqual('t1_191142_191140_190951_191043');
    });

    it('yandex_login', () => {
        const curCookie = 'yandex_login=test';
        document.cookie = `${prefix} ${curCookie}`;
        const cookie = getCookie('yandex_login');
        expect(cookie).toEqual('test');
    });

    it('cycada', () => {
        document.cookie = 'cycada=LFEDF23F';
        const cookie = getCookie('cycada');
        expect(cookie).toEqual('LFEDF23F');
    });

    it('ypCookie', () => {
        document.cookie = '_ym_uid=16343452114705; yp=1671828828.cld.1955450#1656060828.szm.2:1440x900:1440x800#1642946445.csc.1#1665565081.ygu.0#1931244100.multib.1#1955628029.skin.d';
        expect(getYpValue('skin')).toEqual('d');
    });
});
