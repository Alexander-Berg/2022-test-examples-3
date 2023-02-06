'use strict';

const headersMasks = require('./headers-masks.js');

const obfuscateCookieValues = headersMasks.$$obfuscateCookieValues;
const cookieReplace = headersMasks.cookie;
const defaultReplace = headersMasks.ticket;
const ticketReplace = headersMasks['x-ya-service-ticket'];

const sessionIdPlain = '3:1513699982.5.0.1512049627928:pgABAAAAAAAD0BCwuAYCKg:41.1|442460952.0.2' +
    '|390213941.49.2.2:49|328420462.1209640.302.2:1209640|339769297.76191.2.2:76191|' +
    '1130000021137042.357334.2.2:357334|527840696.428246.2.2:428246|505671893.1217859.2.2:1217859' +
    '|174545.132228.';
const sessionIdValue = sessionIdPlain + 'asxhu12j312njashuc81asd90hxjk1';
const sessionIdObf = sessionIdPlain + '***';

const getCookieValue = (sessionIdValue) => 'msgsent=1; gsScrollPos-61=0; gsScrollPos-260=0; gsScrollPos-627=; ' +
    'gsScrollPos-629=; gsScrollPos-649=; pcssspb=1; pcs_for_oo-rtb-direct=1; pcs3=1; pcs_for_oo-rtb-dsp=1; ' +
    '_ym_uid=1505302889692246635; yandexuid=5982325401501173906; ' +
    'fuid01=59d617216727a5d2.ZFM9vcdkADDJZNox480MV3QeEhUDL8YUsvvmUlZEToxBY4Yi8iimB6hC73R2aXV7Sp6UcQ' +
    'dvj3DRvTmdVXXCE-aipJP4MQc2qYfICkvsjT4uu-wZcul0RD3pm4eqUr5x; mda=0; yandex_gid=213; ' +
    'i=seuiaMa9WRSFcMvFcy+spxmErwW4HBOZ9ptRtv8Ysl0hCp9A1xwgfVzuMq1LD+LD0e/ytzerxTiil5BLs25qY0mSd5c=; ' +
    '_ym_isad=1; yabs-frequency=/4/0G00028Nrba00000/K4-mS7Gj8G00/; yc=1513939794.zu.a%3A1; ' +
    'zm=m-white_bender.webp.css-https%3Awww_nTcgEjfVgOXFwvkFGQXuSoRVeGU%3Al; my=YycCAAEA; ' +
    `Session_id=${sessionIdValue}; ` +
    `sessionid2=${sessionIdValue}; ` +
    'yp=1536925773.cld.1955450#1536925773.gd.OKRUIKae7UXXmykmE%2BLzs0Q' +
    `sessguard=${sessionIdValue}; ` +
    'PR3itSbA6xKgasF3K4QRa7GrYpBVJP3zB504Jas0v9J7urg%3D%3D#1820749769.multib.1#15' +
    '29448597.szm.1_00%3A1920x1080%3A1855x592#1825374985.yb.17_3_1_873%3A%3A1501665870%3A15100149' +
    '85%3A56#1515329124.ygu.1#1514890195.ysl.1#1829059982.udn.cDp0ZXN0b3ZpeS10ZXN0NzQ%3D; ys=cst.enbl#de' +
    'f_bro.1#musicchrome.0-0-4722#svt.1#udn.cDp0ZXN0b3ZpeS10ZXN0NzQ%3D#ymrefl.60BD282839723EA4; ' +
    'L=A3pAWmMEQExDcWB/Vll3ekEPfUJ1cEhsFwMaPTwHBzYZOgdHPXx5.1513699982.13351.352443.264a4e5c8ab1a17c' +
    '8e7c5f9a79b744b6; yandex_login=testoviy-test74; stngs=colorful%3A%3Atrue%3Aon; ' +
    'device_id="a1ca595c1dd98ae76198edf5034600a6fec11dc8c"';

describe('obfuscateCookieValues', function() {
    it('если fieldValue или fieldName - falsy, должен вернуть не изменённое значение', function() {
        const value = 'Session_id=';
        const value2 = '=test';

        expect(obfuscateCookieValues(value, 'Session_id', '')).toEqual(value);
        expect(obfuscateCookieValues(value2, '', 'test')).toEqual(value2);
    });

    it('если в fieldValue нет точек, то должен вернуть не изменённое значение', function() {
        const value = 'Session_id=test';
        expect(obfuscateCookieValues(value, 'Session_id', 'test')).toEqual(value);
    });

    it('если в fieldValue последняя точка в конце строки, то должен вернуть не изменённое значение', function() {
        const value = 'Session_id=te.st.';
        expect(obfuscateCookieValues(value, 'Session_id', 'te.st.')).toEqual(value);
    });

    it('если в fieldValue есть точки, то должен заменить всё после последней точки на символы `***`', function() {
        expect(obfuscateCookieValues('Session_id=.test;', 'Session_id', '.test'))
            .toEqual('Session_id=.***');
        expect(obfuscateCookieValues(`Session_id=${sessionIdValue};`, 'Session_id', sessionIdValue))
            .toEqual(`Session_id=${sessionIdObf}`);
    });

    it('должен работать для куки sessguard', function() {
        expect(obfuscateCookieValues('sessguard=1.2.3;', 'sessguard', '1.2.3'))
            .toEqual('sessguard=1.2.***');
    });
});

describe('cookieReplace', function() {
    it('если на вход поступает не строка, то должен возвращать входное значение', function() {
        expect(cookieReplace(null)).toBeNull();
    });

    it('должен заменять подпись в куках Session_id и sessionid2', function() {
        expect(cookieReplace(getCookieValue(sessionIdValue)))
            .toEqual(getCookieValue(sessionIdObf));
    });

    it('должен корректно заменять подпись в куках Session_id и sessionid2 несколько раз подряд', function() {
        expect(cookieReplace(getCookieValue(sessionIdValue)))
            .toEqual(getCookieValue(sessionIdObf));
        expect(cookieReplace(getCookieValue('123.key')))
            .toEqual(getCookieValue('123.***'));
        expect(cookieReplace(getCookieValue('456.key2')))
            .toEqual(getCookieValue('456.***'));
    });

    it('должен заменять куки отделённые пробелом и куки в конце строки', function() {
        expect(cookieReplace('Session_id=123.key sessionid2=456.key2'))
            .toEqual('Session_id=123.*** sessionid2=456.***');
    });

    it('если Session_id или sessionid2 без ключа, то не должен их изменять', function() {
        expect(cookieReplace('Session_id=123; sessionid2=456;'))
            .toEqual('Session_id=123; sessionid2=456;');
    });
});

describe('defaultReplace', function() {
    it('если на входе пустая строка, то должен вернуть это значение', function() {
        expect(defaultReplace('')).toEqual('');
    });

    it('если на входе не пустая строка, то должен вернуть `***`', function() {
        expect(defaultReplace('test')).toEqual('***');
    });
});


describe('ticketReplace', function() {
    it('если на входе пустая строка, то должен вернуть это значение', function() {
        expect(ticketReplace('')).toEqual('');
    });

    it('если на входе не пустая строка, то должен вернуть `***`', function() {
        expect(ticketReplace('3:test:data:sign')).toEqual('***');
    });

    it('если на входе тикет, nо должен заменить подпись на `***`', function() {
        expect(ticketReplace('3:user:data:sign')).toEqual('3:user:data:***');
        expect(ticketReplace('3:serv:data:sign')).toEqual('3:serv:data:***');
    });

});
