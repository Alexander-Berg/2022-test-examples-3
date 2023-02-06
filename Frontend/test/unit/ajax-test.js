const assert = require('assert');

describe('RUM ajax', function() {
    let sentCounters = [];

    function createAjaxPerf(options) {
        options = Object.assign({
            xhr: { status: 200, statusText: 'OK' },
            url: 'url',
            actionTime: Date.now()
        }, options);

        return new Ya.Rum.AjaxPerf(options);
    }

    function assertCountersCount(expectedCount) {
        assert.equal(sentCounters.length, expectedCount);
    }

    function assertCounter(counter, vars, url) {
        assert.deepEqual(sentCounters, [{
            path: '690.1201', // tech.ajax
            url,
            vars
        }]);
    }

    before(() => {
        // mock required browser environment
        global.window = global;
        window.navigator = { userAgent: 'Mosaic 1.0' };
        window.Ya = {
            Rum: {
                getVarsList: () => [],
                getTime: () => Date.now(),
                normalize: value => value,
                pushConnectionTypeTo: () => {
                },
                getResourceTimings: (url, cb) => cb(null),
                sendCounter: (path, varsArr, counterParams, url) => {
                    var vars = varsArr.filter(Boolean).join(',').replace(/=\d{4,}/g, '=12345');
                    sentCounters.push({ path, vars, url });
                }
            }
        };

        require('../../src/bundle/ajax');
    });

    beforeEach(function() {
        sentCounters = [];
    });

    after(() => {
        ['Ya', 'navigator'].forEach(name => delete window[name]);
        delete global.window;
    });

    it('should extend Rum with AjaxPerf', () => {
        assert.equal(typeof Ya.Rum.AjaxPerf, 'function');
    });

    it('should ignore calls to send() when request is not completed', () => {
        createAjaxPerf().send();
        assertCountersCount(0);
    });

    it('should send counter when request is completed', () => {
        const ajaxPerf = createAjaxPerf();

        ajaxPerf.onRequestComplete();
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.689=12345,1201.2154=12345,' +
            '1201.3103=12345,1201.789=12345',
            null);
    });

    it('should send counter without XHR info when no XHR', () => {
        const ajaxPerf = createAjaxPerf({ xhr: null });

        ajaxPerf.onRequestComplete();
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,1201.906=12345,1201.689=12345,1201.2154=12345,1201.3103=12345,1201.789=12345',
            null);
    });

    it('should send counter when no URL', () => {
        const ajaxPerf = createAjaxPerf({ url: null });

        ajaxPerf.onRequestComplete();
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.689=12345,1201.2154=12345,' +
            '1201.3103=12345,1201.789=12345',
            null);
    });

    it('should send counter when no actionTime', () => {
        const ajaxPerf = createAjaxPerf({ actionTime: null });

        ajaxPerf.onRequestComplete();
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.2154=12345,' +
            '1201.3103=12345',
            null);
    });

    it('should send counter with custom requestTime', () => {
        const ajaxPerf = createAjaxPerf({ requestTime: 10 });

        ajaxPerf.onRequestComplete();
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.689=12345,1201.2154=12345,' +
            '1201.3103=12345,1201.789=12345',
            null);
    });

    it('should send ajax.dom time if available', () => {
        const ajaxPerf = createAjaxPerf();

        ajaxPerf.onRequestComplete();
        ajaxPerf.onRenderComplete(9);
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.689=12345,1201.2154=12345,' +
            '1201.3103=12345,1201.789=12345,1201.1310=9',
            null);
    });

    it('should send URL and error information on error', () => {
        const ajaxPerf = createAjaxPerf();

        ajaxPerf.onRequestError('Error type');
        ajaxPerf.send();

        assertCountersCount(1);
        assertCounter(sentCounters[0],
            '1042=Mosaic%201.0,2772.720=200,2772.720.232=OK,1201.906=12345,1201.689=12345,1201.2154=12345,1201.3103=12345,' +
            '1201.789=12345,1201.1030=1,1201.1030.1304=Error type',
            'url');
    });
});
