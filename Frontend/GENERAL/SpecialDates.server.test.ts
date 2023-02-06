import { assert } from 'chai';
import sinon from 'sinon';
import { stubGlobal } from '@lib/StubGlobal';
import { AdapterSpecialDates } from './SpecialDates@common.server';

const DateTime = require('@yandex-int/si.utils/DateTime/DateTime.js');

describe('AdapterSpecialDates.freshFormat', function() {
    let userTime: string;

    function assertFormat(docDate: string) {
        const adapter = Object.create(AdapterSpecialDates.prototype);
        const strftime = sinon.spy(DateTime.DateTime.strftime);
        adapter.privExternals = {
            BEM: {
                I18N: {
                    strftime,
                    lang: () => 'ru',
                },
            },
        };
        stubGlobal('BEM');
        const result = adapter.freshFormat(userTime, docDate);
        return {
            result: result.text,
            strftime,
        };
    }

    describe('for morning user (time range from 00:00 to 05:59)', function() {
        beforeEach(function() {
            userTime = '2014-10-05T05:00:00+03:00';
        });

        it('should return human readable desc for documents from day before yesterday', function() {
            assert.equal(assertFormat('2014-10-03T11:00:00+03:00').result, 'позавчера');
        });

        it('should return human readable desc for documents from yesterday', function() {
            assert.equal(assertFormat('2014-10-04T22:30:00+03:00').result, 'вчера', 'Yesterday before 23:00');

            assert.equal(assertFormat('2014-10-04T23:00:00+03:00').result, '6 часов назад', 'Yesterday after 23:00');
        });

        it('should return human readable desc for time range in hours', function() {
            assert.equal(assertFormat('2014-10-05T04:00:00+03:00').result, '1 час назад');
        });

        it('should return human readable desc for time range in minutes', function() {
            assert.equal(assertFormat('2014-10-05T04:30:00+03:00').result, '30 минут назад');
        });

        it('should return human readable desc for time range smaller than 1 minute', function() {
            assert.equal(assertFormat('2014-10-05T04:59:30+03:00').result, 'Менее минуты назад');
            assert.equal(assertFormat('2014-10-05T05:00:00+03:00').result, 'Менее минуты назад');
        });
    });

    describe('for morning user in timezone +5', function() {
        beforeEach(function() {
            userTime = '2014-10-05T05:00:00+05:00';
        });

        it('should return human readable desc for documents from day before yesterday', function() {
            assert.equal(assertFormat('2014-10-03T11:00:00+03:00').result, 'позавчера');
        });

        it('should return human readable desc for documents from yesterday', function() {
            assert.equal(assertFormat('2014-10-04T22:30:00+03:00').result, 'вчера', 'Yesterday before 23:00');

            assert.equal(assertFormat('2014-10-04T23:00:00+03:00').result, '4 часа назад', 'Yesterday after 23:00');
        });

        it('should return human readable desc for time range in hours', function() {
            assert.equal(assertFormat('2014-10-05T02:00:00+03:00').result, '1 час назад');
        });

        it('should return human readable desc for time range in minutes', function() {
            assert.equal(assertFormat('2014-10-05T02:30:00+03:00').result, '30 минут назад');
        });

        it('should return human readable desc for time range smaller than 1 minute', function() {
            assert.equal(assertFormat('2014-10-05T02:59:30+03:00').result, 'Менее минуты назад');
            assert.equal(assertFormat('2014-10-05T03:00:00+03:00').result, 'Менее минуты назад');
            assert.equal(assertFormat('2014-10-05T05:00:00+05:00').result, 'Менее минуты назад');
        });
    });

    describe('for common user (time range from 06:00 to 23:59)', function() {
        beforeEach(function() {
            userTime = '2014-10-05T06:00+03:00';
        });

        it('should return human readable desc for documents from day before yesterday', function() {
            assert.equal(assertFormat('2014-10-03T11:00:00+03:00').result, 'позавчера');
        });

        it('should return human readable desc for documents from yesterday', function() {
            assert.equal(assertFormat('2014-10-04T19:30:00+03:00').result, 'вчера', 'Yesterday before 20:00');
            assert.equal(assertFormat('2014-10-04T20:00:00+03:00').result, 'вчера', 'Yesterday after 20:00');
        });

        it('should return human readable desc for morning documents (00:00 to 05:59)', function() {
            assert.equal(assertFormat('2014-10-05T00:00:00+03:00').result, '6 часов назад');
            assert.equal(assertFormat('2014-10-05T05:30:00+03:00').result, '30 минут назад');
            assert.equal(assertFormat('2014-10-05T05:59:59+03:00').result, 'Менее минуты назад');
        });
    });

    describe('for common user in timezone +5', function() {
        beforeEach(function() {
            userTime = '2014-10-05T08:00+05:00';
        });

        it('should return human readable desc for documents from day before yesterday', function() {
            assert.equal(assertFormat('2014-10-03T11:00:00+03:00').result, 'позавчера');
        });

        it('should return human readable desc for documents from yesterday', function() {
            assert.equal(assertFormat('2014-10-04T19:30:00+03:00').result, 'вчера', 'Yesterday before 20:00');
            assert.equal(assertFormat('2014-10-04T20:00:00+03:00').result, 'вчера', 'Yesterday after 20:00');
        });

        it('should return human readable desc for morning documents (00:00 to 05:59)', function() {
            assert.equal(assertFormat('2014-10-05T00:00:00+03:00').result, '6 часов назад');
            assert.equal(assertFormat('2014-10-05T05:30:00+03:00').result, '30 минут назад');
            assert.equal(assertFormat('2014-10-05T05:59:59+03:00').result, 'Менее минуты назад');
        });
    });

    describe('should return date for earlier dates', function() {
        it('should return date for earlier dates', function() {
            userTime = '2014-10-05T08:00+05:00';
            assert.deepEqual(assertFormat('2014-10-02T00:00:00+03:00').strftime.lastCall.args, ['%e %B', '2014-10-02T00:00:00+03:00']);
            assert.deepEqual(assertFormat('2014-08-01T00:00:00+03:00').strftime.lastCall.args, ['%e %B', '2014-08-01T00:00:00+03:00']);
        });
    });
});

describe('AdapterSpecialDates.getText', function() {
    const adapter = Object.create(AdapterSpecialDates.prototype);
    let cDateStubbed: sinon.SinonStub;
    let strftimeStubbed: sinon.SinonStub;

    beforeEach(() => {
        adapter.context = {};
        adapter.doc = {};
        adapter.snippet = {
            forceShowDate: true,
        };
        adapter.isFresh = () => false;
        cDateStubbed = sinon.stub();
        strftimeStubbed = sinon.stub();
        strftimeStubbed.onFirstCall().returns('');
        adapter.privExternals = {
            BEM: {
                I18N: {
                    strftime: strftimeStubbed,
                    lang: () => 'ru',
                },
            },
            ReportCdate: cDateStubbed,
        };
        stubGlobal('BEM');
    });

    it('should work correct on CDate input', function() {
        adapter.snippet.date = { epoch: '1483192800', __package: 'YxWeb::Util::CDate::Lazy', tz: 'GMT', __is_plain: 1 };
        adapter.getText();
        assert.deepEqual(cDateStubbed.lastCall.args[0], adapter.snippet.date);
        assert.equal(strftimeStubbed.lastCall.args[0], '%e %B %Y');
    });

    it('should work correct on string input', function() {
        adapter.snippet.date = '2017-01-01T10:00:00+0000';
        adapter.getText();
        assert.deepEqual(cDateStubbed.lastCall.args[0], adapter.snippet.date);
        assert.equal(strftimeStubbed.lastCall.args[0], '%e %B %Y');
    });
});
