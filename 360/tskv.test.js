'use strict';

const tskv = require('./tskv.js');

describe('формирование ключей и значение', function() {
    it('должен работать', function() {
        const res = tskv('test', 'level', { test: 'passed' });
        expect(res).toContain('tskv\t');
        expect(res).toContain('\ttskv_format=test\t');
        expect(res).toContain('\tlevel=level\t');
        expect(res).toContain('\ttest=passed');

        expect(res).toMatch(/\ttimestamp=\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\t/, 'Неправильное поле timestamp');
        expect(res).toMatch(/\ttimezone=[+-]\d{4}\t/, 'Неправильное поле timezone');
    });

    it('должен экранировать символы (TAB, NL, CR, backslash)', function() {
        const res = tskv('test', 'level', {
            c1: 'x\tx',
            c2: 'x\nx\nx',
            c3: 'x\rx x',
            c4: 'x\\x'
        });
        expect(res).toContain('\tc1=x\\tx');
        expect(res).toContain('\tc2=x\\nx\\nx');
        expect(res).toContain('\tc3=x\\rx x');
        expect(res).toContain('\tc4=x\\\\x');
    });

    it('должен обрабатывать объекты (JSON.stringify)', function() {
        const res = tskv('test', 'level', { test: { key: 'val' } });
        expect(res).toContain('\ttest={"key":"val"}');
    });
});

describe('формирование timestamp/timezone', function() {
    it('должен правильно формировать timestamp', function() {
        const timestamp = tskv.getTimestamp(new Date());
        expect(timestamp).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('должен правильно формировать timezone', function() {
        const timezone = tskv.getTimezone(new Date());
        expect(timezone).toMatch(/^[+-]\d{4}$/);
    });

    it('должен однозначно восстанавливать время', function() {
        const date = new Date();
        date.setMilliseconds(0);
        const timestamp = tskv.getTimestamp(date);
        const timezone = tskv.getTimezone(date);
        expect(Number(date)).toEqual(Number(new Date(timestamp + ' ' + timezone)));
    });

    it('должен правильно обрабатывать переход на летнее время', function() {
        const str = '2010-07-01 16:33:00';
        const date = new Date(str);
        const timestamp = tskv.getTimestamp(date);
        const timezone = tskv.getTimezone(date);

        expect(timestamp).toEqual(str);
        expect(date.toString()).toContain(timezone);
    });

    it('должен правильно округлять timestamp', function() {
        jest.useFakeTimers();
        jest.setSystemTime(1501);
        const tmp = tskv('test', 'debug', {}).match(/(?<==)[^\t]+/g);
        jest.useRealTimers();
        const date = new Date(tmp[1] + ' ' + tmp[2]);
        const unixtime = tmp[3] * 1000;

        expect(date.getTime()).toEqual(unixtime);
    });
});
