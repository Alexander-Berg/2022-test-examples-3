/* eslint-disable no-console, dot-notation, @typescript-eslint/no-use-before-define */
const { assert } = require('chai');
const { describeRum, getCounters, getCountersByPath, checkCounter } = require('./test-utils');

// При правке этих тестов нужно также править версию для async load
describeRum(httpPort => {
    describe('navigate', () => {
        before(async function() {
            const reqId = Math.random();

            const page = this.page = await this.browser.newPage();

            await page.goto(`http://localhost:${httpPort}/?reqid=${reqId}`, {
                waitUntil: ['load', 'networkidle0']
            });

            await page.$eval('body', () => {
                window.Ya.Rum.sendTrafficData();
                window.Ya.Rum.finalizeLayoutShiftScore();
                window.Ya.Rum.finalizeLargestContentfulPaint();
            });

            await page.click('#button');

            await page.$eval('body', () => {
                window.scrollTo({ behavior: 'smooth', top: 300, left: 0 });
            });

            // Ожидаем записи счётчиков с учётом тайм-аута лонг-тасков
            await new Promise(resolve => {
                setTimeout(resolve, 10000);
            });

            await page.$eval('body', () => {
                window.disableScrollTask();
                window.scrollTo({ behavior: 'smooth', top: 0, left: 0 });
            });

            await new Promise(resolve => {
                setTimeout(resolve, 1100);
            });

            this.counters = await getCounters(reqId);
        });

        after(async function() {
            await this.page.close();
        });

        it('should produce correct tech.timing counter', async function() {
            const commonTimings = [
                '2111', // fetchStart
                '2112', // domainLookupStart
                '2113', // domainLookupEnd
                '2114', // connectStart
                '2116', // connectEnd
                '2117', // requestStart
                '2119', // responseStart
                '2120', // responseEnd
                '2770', // domInteractive
                '2123', // domContentLoadedEventStart
                '2131' // domContentLoadedEventEnd
            ];
            const timingRequiredTimings = [
                '1036', // wait
                '1037', // dns
                '1038', // tcp
                '1039', // ttfb
                '1040', // html
                '1040.906', // html.total
                '1310.2084', // dom.loading
                '1310.2085', // dom.interactive
                '1310.1309', // dom.init
                '1310.1007', // dom.loaded
                '2769' // domLoading
            ];
            const navigationRequiredTimings = [
                ...commonTimings,
                '2124', // domComplete
                '2126', // loadEventEnd
                '2125' // loadEventStart
            ];

            const tCounters = getCountersByPath(this.counters, '690.1033');
            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];

            assert.equal(counter.vars['1484'], '1', 'Page is not visible');
            assert.notOk(counter.vars['1484.719'], 'Visibility was changed');

            assert.ok(counter.vars['2437'], 'Connection type is invalid');
            assert.ok(counter.vars['2870'], 'Effective connection type is invalid');

            const rtt = parseInt(counter.vars['rtt']);
            assert.isNotNaN(rtt, 'RTT is invalid');
            assert.ok(rtt >= 0, 'RTT < 0');

            const dwl = parseFloat(counter.vars['dwl']);
            assert.isNotNaN(dwl, 'Downlink is invalid');
            assert.ok(rtt >= 0, 'Downlink < 0');

            const deviceMemory = parseInt(counter.vars['3140']);
            assert.isNotNaN(deviceMemory, 'Device memory is invalid');
            assert.isAbove(deviceMemory, 0, 'Device memory <= 0');

            const hardwareConcurrency = parseInt(counter.vars['3141']);
            assert.isNotNaN(hardwareConcurrency, 'Hardware concurrency is invalid');
            assert.isAbove(hardwareConcurrency, 0, 'Hardware concurrency <= 0');

            checkCounter(counter, timingRequiredTimings);

            const nCounters = getCountersByPath(this.counters, '690.2096.2892');
            assert.lengthOf(nCounters, 1, 'There must be the only one counter');

            const navCounter = nCounters[0];

            assert.ok(navCounter.vars['2437'], 'Connection type is invalid');
            assert.ok(navCounter.vars['2870'], 'Effective connection type is invalid');

            checkCounter(navCounter, navigationRequiredTimings);
        });

        it('should produce correct tech.perf.navigation counter', async function() {
            const requiredTimings = [
                '2116', // connectEnd
                '2114', // connectStart
                '2124', // domComplete
                '2131', // domContentLoadedEventEnd
                '2123', // domContentLoadedEventStart
                '2770', // domInteractive
                '2113', // domainLookupEnd
                '2112', // domainLookupStart
                '2136', // duration
                '2111', // fetchStart
                '2126', // loadEventEnd
                '2125', // loadEventStart
                '1385', // redirectCount
                '2110', // redirectEnd
                '2109', // redirectStart
                '2117', // requestStart
                '2120', // responseEnd
                '2119', // responseStart
                '2115', // secureConnectionStart
                '2322', // startTime
                '2128', // unloadEventEnd
                '2127', // unloadEventStart
                '2137' // workerStart
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2892');
            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];

            assert.ok(counter.vars['2437'], 'Connection type is invalid');
            assert.ok(counter.vars['2870'], 'Effective connection type is invalid');

            assert.equal(counter.vars['76'], 'navigate', 'Request type is invalid');
            assert.equal(counter.vars['2888'], 'navigation', 'Entry type is invalid');
            assert.equal(counter.vars['2889'], 'navigation', 'Initiator type is invalid');
            assert.equal(counter.vars['2890'], 'http/1.1', 'Next hop protocol is invalid');

            const transferSize = parseInt(counter.vars['2323']);
            assert.isNotNaN(transferSize, 'Transfer size is invalid');
            assert.isAbove(transferSize, 0, 'Transfer size is too small');

            const encodedBodySize = parseInt(counter.vars['2887']);
            assert.isNotNaN(encodedBodySize, 'Encoded body size is invalid');
            assert.isAbove(encodedBodySize, 0, 'Encoded body size is too small');

            const decodedBodySize = parseInt(counter.vars['2886']);
            assert.isNotNaN(decodedBodySize, 'Decoded body size is invalid');
            assert.isAbove(decodedBodySize, 0, 'Decoded body size is too small');

            checkCounter(counter, requiredTimings);
        });

        it('should produce correct tech.perf.images counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.277');
            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];

            const firstImageTime = parseFloat(counter.vars['ft']);
            assert.isNotNaN(firstImageTime, 'First image time is invalid');
            assert.isAbove(firstImageTime, 0, 'First image time is too small');

            const lastImageTime = parseFloat(counter.vars['lt']);
            assert.isNotNaN(lastImageTime, 'Last image time is invalid');
            assert.isAbove(lastImageTime, 0, 'Last image time is too small');

            const resourcesExpected = parseInt(counter.vars['er']);
            assert.isNotNaN(resourcesExpected, 'Expected resources count is invalid');
            assert.isAbove(resourcesExpected, 0, 'Expected resources count is too small');

            const resourcesFound = parseInt(counter.vars['fr']);
            assert.isNotNaN(resourcesFound, 'Found resources count is invalid');
            assert.isAbove(resourcesFound, 0, 'Found resources count is too small');

            assert.equal(counter.vars['d'], '1', 'Counter does not have data URI flag');
        });

        it('should produce correct tech.perf.traffic counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.361'); // tech.perf.traffic
            assert.ok(tCounters.length >= 1, 'There must be traffic counter');

            const counter = tCounters[0];

            const resourceData = counter.vars['d'];
            assert.ok(resourceData, 'Traffic data is empty');

            const time = counter.vars['t'];
            assert.ok(time, 'Send time is empty');

            const chunks = resourceData.split(';');

            for (const chunk of chunks) {
                if (!chunk) {
                    continue;
                }

                const [groupKey, sCount, sSize] = chunk.split('!');

                assert.ok(groupKey, `There is no domain with extension for some chunk in ${resourceData}`);

                const count = parseInt(sCount);
                const size = parseInt(sSize);

                assert.isNotNaN(count, `Invalid count for ${groupKey}`);
                assert.isAbove(count, 0, `Negative count for ${groupKey}`);

                assert.isNotNaN(size, `Invalid size for ${groupKey}`);
                assert.isAbove(size, 0, `Negative size for ${groupKey}`);
            }
        });

        it('should produce correct tech.perf.cls counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.4004'); // tech.perf.cls
            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];
            const cls = counter.vars['s'];

            assert.ok(cls, 'CLS data is empty');

            var score = parseFloat(cls);

            assert.isNotNaN(score, 'CLS is NaN');
            assert.isAbove(score, 0, 'CLS is negative');
        });

        it('should produce correct tech.perf.time[id=paint.first-paint] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === '1926.2793'); // 1926.2793 – paint.first-paint

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=paint.first-contentful-paint] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === '1926.2794'); // 1926.2794 – paint.first-contentful-paint

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=largest-loading-elem-paint] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === 'largest-loading-elem-paint');

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=largest-contentful-paint] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === 'largest-contentful-paint');

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=element-timing.identifier] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === 'element-timing.image');

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=tti] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === '2795'); // 2795 – tti

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            const longTaskData = counter.vars['2796.2797']; // 2796.2797 – long-task.value

            assert.ok(longTaskData, 'Long-task data is absent');
            assert.match(longTaskData, /s-\d+-\d+/, 'Long task data is invalid');

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.time[id=load] counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.207');
            const fCounters = tCounters.filter(x => x.vars['1701'] === '1724'); // 1724 – load

            assert.lengthOf(fCounters, 1, 'There must be the only one counter');

            const counter = fCounters[0];

            checkCounter(counter, ['207']); // 207 – time
        });

        it('should produce correct tech.perf.delta[id=main-bundle-parse] counter', async function() {
            const requiredTimings = [
                '207.2154', // time.start
                '207.1428', // time.end
                '2877' // delta
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2877');
            const deltaCounters = tCounters.filter(x => x.vars['1701'] === 'main-bundle-parse');
            assert.lengthOf(deltaCounters, 1, 'There must be the only one counter');
            const counter = deltaCounters[0];

            assert.isAbove(Number(counter.vars['2877']), 0, 'Delta <= 0');
            checkCounter(counter, requiredTimings);
        });

        it('should produce correct tech.perf.delta[id=content-parse] counter', async function() {
            const requiredTimings = [
                '207.2154', // time.start
                '207.1428', // time.end
                '2877' // delta
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2877');
            const deltaCounters = tCounters.filter(x => x.vars['1701'] === 'content-parse');

            assert.lengthOf(deltaCounters, 1, 'There must be the only one counter');

            const counter = deltaCounters[0];

            checkCounter(counter, requiredTimings);
        });

        it('should produce correct tech.perf.delta[id=long-task-delta-before-implementation-load] counter', async function() {
            const requiredTimings = [
                '207.2154', // time.start
                '207.1428', // time.end
                '2877' // delta
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2877');
            const deltaCounters = tCounters.filter(x => x.vars['1701'] === 'long-task-delta-before-implementation-load');

            assert.lengthOf(deltaCounters, 1, 'There must be the only one counter');

            const counter = deltaCounters[0];

            checkCounter(counter, requiredTimings);

            assert.approximately(counter.vars['207.1428'] - counter.vars['207.2154'], 95, 0.0000001, 'Expect long-task-delta-before-implementation-load\'s value to be equal to 95 (see test.html)');
        });

        it('should produce correct tech.perf.delta[id=first-input] counter', async function() {
            const requiredTimings = [
                '207.2154', // time.start
                '207.1428', // time.end
                '2877', // delta
                'duration', // first-input duration
                'js', // first-input js duration
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2877');
            const deltaCounters = tCounters.filter(x => x.vars['1701'] === 'first-input');
            assert.lengthOf(deltaCounters, 1, 'There must be the only one counter');
            const counter = deltaCounters[0];

            assert.isAbove(Number(counter.vars['2877']), 0, 'Delta <= 0');
            checkCounter(counter, requiredTimings);
            assert.equal(counter.vars['name'], 'mousedown', 'There must be the event name');
            assert.equal(counter.vars['target'], 'button.button.button2',
                'There must be the CSS selector for target element of first-input event');
        });

        it('should produce correct tech.perf.long-task counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.2796'); // tech.perf.long-task
            assert.ok(tCounters.length === 0, 'There must be no long-task counter');
        });

        it('should produce correct tech.perf.scroll counter', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.768'); // tech.perf.scroll
            assert.ok(tCounters.length >= 1, 'There must be scroll counter');

            const counter = tCounters[0];

            checkCounter(counter, ['d']);

            const firstVal = parseFloat(counter.vars.d);
            const lastVal = parseFloat(tCounters[tCounters.length - 1].vars.d);
            assert.isAtLeast(firstVal, 200, 'Scroll is lagging, scroll counter\'s d is big');
            assert.isBelow(lastVal, 50, 'Smooth scroll, scroll counter\'s d is small');
        });

        it('should produce correct tech.perf.resource_timing[id=rum-implementation] counter', async function() {
            const requiredTimings = [
                '2116', // connectEnd
                '2114', // connectStart
                '2113', // domainLookupEnd
                '2112', // domainLookupStart
                '2136', // duration
                '2111', // fetchStart
                '2110', // redirectEnd
                '2109', // redirectStart
                '2117', // requestStart
                '2120', // responseEnd
                '2119', // responseStart
                '2115', // secureConnectionStart
                '2322', // startTime
                '2137' // workerStart
            ];

            const tCounters = getCountersByPath(this.counters, '690.2096.2044')
                .filter(x => x.vars['1701'] === 'rum-interface');

            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];

            const transferSize = parseInt(counter.vars['2323']);
            assert.isNotNaN(transferSize, 'Transfer size is invalid');
            assert.isAbove(transferSize, 0, 'Transfer size is too small');

            const encodedBodySize = parseInt(counter.vars['2887']);
            assert.isNotNaN(encodedBodySize, 'Encoded body size is invalid');
            assert.isAbove(encodedBodySize, 0, 'Encoded body size is too small');

            const decodedBodySize = parseInt(counter.vars['2886']);
            assert.isNotNaN(decodedBodySize, 'Decoded body size is invalid');
            assert.isAbove(decodedBodySize, 0, 'Decoded body size is too small');

            checkCounter(counter, requiredTimings);
        });
    });

    describe('AJAX', () => {
        before(async function() {
            const reqId = Math.random();

            const page = this.page = await this.browser.newPage();

            await page.goto(`http://localhost:${httpPort}/?reqid=${reqId}`, {
                waitUntil: ['load', 'networkidle0']
            });

            await page.evaluate(async() => {
                await window.makeAjax();
            });

            // Ожидаем записи счётчиков
            await new Promise(resolve => {
                setTimeout(resolve, 1500);
            });

            this.counters = await getCounters(reqId);
        });

        after(async function() {
            await this.page.close();
        });

        it('should produce correct tech.ajax counter', async function() {
            const requiredTimings = [
                '1201.906', // ajax.total
                '1201.689', // ajax.action
                '1201.2154', // ajax.start
                '1201.3103', // ajax.complete
                '1201.789', // ajax.before
                '1201.1310', // ajax.dom
                '1036', // wait
                '1037', // dns
                '1038', // tcp
                '1039', // ttfb
                '1040', // html
                '1040.906', // html.total
                '2111', // fetchStart
                '2112', // domainLookupStart
                '2113', // domainLookupEnd
                '2114', // connectStart
                '2116', // connectEnd
                '2117', // requestStart
                '2119', // responseStart
                '2120' // responseEnd
            ];

            const tCounters = getCountersByPath(this.counters, '690.1201');
            assert.lengthOf(tCounters, 1, 'There must be the only one counter');

            const counter = tCounters[0];

            assert.ok(counter.vars['2437'], 'Connection type is invalid');
            assert.ok(counter.vars['2870'], 'Effective connection type is invalid');

            const deviceMemory = parseInt(counter.vars['3140']);
            assert.isNotNaN(deviceMemory, 'Device memory is invalid');
            assert.isAbove(deviceMemory, 0, 'Device memory <= 0');

            const hardwareConcurrency = parseInt(counter.vars['3141']);
            assert.isNotNaN(hardwareConcurrency, 'Hardware concurrency is invalid');
            assert.isAbove(hardwareConcurrency, 0, 'Hardware concurrency <= 0');

            const transferSize = parseInt(counter.vars['2323']);
            assert.isNotNaN(transferSize, 'Transfer size is invalid');
            assert.isAbove(transferSize, 0, 'Transfer size is too small');

            const encodedBodySize = parseInt(counter.vars['2887']);
            assert.isNotNaN(encodedBodySize, 'Encoded body size is invalid');
            assert.isAbove(encodedBodySize, 0, 'Encoded body size is too small');

            const decodedBodySize = parseInt(counter.vars['2886']);
            assert.isNotNaN(decodedBodySize, 'Decoded body size is invalid');
            assert.isAbove(decodedBodySize, 0, 'Decoded body size is too small');

            const longTaskValue = counter.vars['2796.2797']; // 2796.2797 – long-task.value
            assert.ok(longTaskValue, 'Long-task value is absent');
            assert.match(longTaskValue, /^s-\d+-\d+$/, 'Long task value is invalid');

            checkCounter(counter, requiredTimings);
        });
    });

    describe('resource timing', () => {
        const sleep = time => new Promise(resolve => setTimeout(resolve, time));

        before(async function() {
            const reqId = Math.random();

            const page = this.page = await this.browser.newPage();

            await page.goto(`http://localhost:${httpPort}/?reqid=${reqId}`, {
                waitUntil: ['load', 'networkidle0']
            });

            await page.evaluate(async() => {
                await window.fetchJsonApi();
                await window.fetchJsonApi();
            });

            // Ожидаем записи счётчиков
            await sleep(1500);

            this.counters = await getCounters(reqId);
        });

        after(async function() {
            await this.page.close();
        });

        it('should correctly send same sequential resource timings', async function() {
            const tCounters = getCountersByPath(this.counters, '690.2096.2044')
                .filter(x => x.vars['1701'] === 'ajax_json');
            assert.lengthOf(tCounters, 2, 'There must be only two counters');

            const fetchStart = '2111';

            assert.isAbove(Number(tCounters[1].vars[fetchStart]), Number(tCounters[0].vars[fetchStart]), 'Sequential requests must have increasing fetchStart timing');
        });
    });
});
