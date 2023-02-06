const assert = require('chai').assert;

describe('RUM implementation', () => {
    let sentData;
    let windowEvents;
    let domElements;

    const vars = '1042=Mosaic%201.0,2129=12345,1036=NaN,1037=NaN,1038=NaN,1039=NaN,1040=NaN,1040.906=NaN,' +
        '1310.2084=NaN,1310.2085=NaN,1310.1309=NaN,1310.1007=NaN,1484=1,-cdn=fake-region';

    const moduleName = '../../src/bundle/implementation';

    function reloadModule() {
        windowEvents = {};
        delete require.cache[require.resolve(moduleName)];
        require(moduleName);
    }

    beforeEach(() => {
        // mock required browser environment
        global.window = global;
        window.Ya = {
            Rum: {
                enabled: true,
                _vars: {},
                _settings: {
                    sendClientUa: true,
                    sendFirstRaf: true
                },
                _defTimes: [],
                _defRes: [],
                _deltaMarks: {},
                _markListeners: {},

                send(link, path, vars) {
                    // заменяем длинные timestamp на 12345
                    sentData.push({ path, vars: vars.replace(/=\d{5,}/g, '=12345') });
                },
                isVisibilityChanged() {
                    return false;
                },
                mark() {
                },
                getTime() {
                    return 123;
                },
                getSetting(settingName) {
                    const setting = this._settings[settingName];
                    return setting === null ? null : setting || '';
                },
                on: function(counterId, cb) {
                    (this._markListeners[counterId] = this._markListeners[counterId] || []).push(cb);
                }
            }
        };

        window.YaStaticRegion = 'fake-region';

        window.history = [];
        window.navigator = { userAgent: 'Mosaic 1.0' };
        window.performance = { timing: { navigationStart: Date.now() - 100 } };

        window.addEventListener = (name, listener) => {
            windowEvents[name] = windowEvents[name] || [];
            windowEvents[name].push(listener);
        };
        window.removeEventListener = () => {
        };

        // noinspection JSUnusedGlobalSymbols
        global.document = {
            _nodes: {},
            readyState: 'loading',
            createElement: tagName => ({ tagName: tagName.toUpperCase() }),
            getElementsByTagName: tagName => domElements[tagName.toUpperCase()],
            head: {
                appendChild: childNode => (domElements[childNode.tagName] ||
                    (domElements[childNode.tagName] = [])).push(childNode)
            }
        };

        // clean volatile data
        sentData = [];
        windowEvents = {};
        domElements = {};

        reloadModule();
    });

    afterEach(() => {
        ['Ya', 'history', 'navigator', 'performance', 'addEventListener', 'removeEventListener']
            .forEach(name => delete window[name]);
        delete global.window;
        delete global.document;
        delete global.domElements;
    });

    it('should start immediately when document.readyState === "complete"', done => {
        document.readyState = 'complete';
        reloadModule();

        Ya.Rum._settings.disableOnLoadTasks = true;

        setTimeout(() => {
            assert.deepEqual(sentData, [{
                path: '690.1033',
                vars: vars
            }]);
            done();
        }, 10);
    });

    it('should start immediately when document.readyState === "interactive"', done => {
        document.readyState = 'interactive';
        reloadModule();

        setTimeout(() => {
            assert.deepEqual(sentData, [{
                path: '690.1033',
                vars: vars
            }]);
            done();
        }, 10);
    });

    it('should wait for load event when document.readyState === "loading"', done => {
        document.readyState = 'loading';
        reloadModule();

        assert.strictEqual(windowEvents.DOMContentLoaded && windowEvents.DOMContentLoaded.length, 1,
            'Should add listener to DOMContentLoaded event');
        assert.strictEqual(windowEvents.load && windowEvents.load.length, 1,
            'Should add listener to load event');
        assert.deepEqual(sentData, [], 'Should not send any data for now');

        windowEvents.load[0]();

        setTimeout(() => {
            assert.deepEqual(sentData, [{
                path: '690.1033',
                vars: vars
            }]);
            done();
        }, 10);
    });

    it('should send page navigation data', done => {
        document.readyState = 'complete';
        window.performance.timing = {
            connectEnd: 1516376007679,
            connectStart: 1516376007679,
            domComplete: 1516376008453,
            domContentLoadedEventEnd: 1516376008449,
            domContentLoadedEventStart: 1516376008332,
            domInteractive: 1516376008332,
            domLoading: 1516376007715,
            domainLookupEnd: 1516376007679,
            domainLookupStart: 1516376007679,
            fetchStart: 1516376007679,
            loadEventEnd: 1516376008454,
            loadEventStart: 1516376008453,
            navigationStart: 1516376007670,
            redirectEnd: 0, // should not add '2110=-1516376007670' to vars
            redirectStart: 0, // should not add '2109=-1516376007670' to vars
            requestStart: 1516376007686,
            responseEnd: 1516376007704,
            responseStart: 1516376007695,
            secureConnectionStart: 0, // should not add '2115=-1516376007670' to vars
            unloadEventEnd: 1516376007704,
            unloadEventStart: 1516376007704
        };
        reloadModule();

        Ya.Rum._settings.disableOnLoadTasks = true;

        setTimeout(() => {
            assert.deepEqual(sentData, [{
                path: '690.1033',
                vars: [
                    '1042=Mosaic%201.0',
                    '2129=12345,1036=9,1037=0,1038=0,1039=16,1040=9,1040.906=25',
                    '1310.2084=20,1310.2085=637,1310.1309=117,1310.1007=637',

                    '2116=9',
                    '2114=9',
                    '2124=783',
                    '2131=779',
                    '2123=662',
                    '2770=662',
                    '2769=45',
                    '2113=9',
                    '2112=9',
                    '2111=9',
                    '2126=784',
                    '2125=783',
                    '2117=16',
                    '2120=34',
                    '2119=25',
                    '2128=34',
                    '2127=34',
                    '1484=1',
                    '-cdn=fake-region'
                ].join(',')
            }]);
            done();
        }, 10);
    });

    // VPROBLEM-492: учитываем, что в IFRAME могут быть нули вместо
    // domainLookupStart, domainLookupEnd, requestStart, responseStart
    it('should send page navigation data for an IFRAME', done => {
        document.readyState = 'complete';
        window.performance.timing = {
            connectEnd: 0,
            connectStart: 0,
            domComplete: 1612347060393,
            domContentLoadedEventEnd: 1612347060393,
            domContentLoadedEventStart: 1612347060393,
            domInteractive: 1612347060393,
            domLoading: 1612347060390,
            domainLookupEnd: 0,
            domainLookupStart: 0,
            fetchStart: 0,
            loadEventEnd: 1612347060393,
            loadEventStart: 1612347060393,
            navigationStart: 1612347060390,
            redirectEnd: 0,
            redirectStart: 0,
            requestStart: 0,
            responseEnd: 1612347060393,
            responseStart: 0,
            secureConnectionStart: 0,
            unloadEventEnd: 0,
            unloadEventStart: 0
        };
        reloadModule();

        Ya.Rum._settings.disableOnLoadTasks = true;

        setTimeout(() => {
            assert.deepEqual(sentData, [{
                path: '690.1033',
                // значение 12345 разрешено только в vars['2129'], в метриках
                // 1036, 1040, 1040.906, 1310.2084, 1310.2085, 1310.1007 должны быть валидные значения
                vars: [
                    '1042=Mosaic%201.0',
                    '2129=12345,1036=0,1037=0,1038=0,1039=0',
                    '1040=3,1040.906=3',
                    '1310.2084=0,1310.2085=3,1310.1309=0,1310.1007=3',

                    '2124=3',
                    '2131=3',
                    '2123=3',
                    '2770=3',
                    '2769=0',
                    '2126=3',
                    '2125=3',
                    '2120=3',
                    '1484=1',
                    '-cdn=fake-region'
                ].join(',')
            }]);
            done();
        }, 10);
    });

    it('should not send UA if not enabled', done => {
        document.readyState = 'interactive';
        Ya.Rum._settings.sendClientUa = false;

        reloadModule();

        setTimeout(() => {
            assert.lengthOf(sentData, 1, 'There is no counters');
            assert.deepNestedPropertyVal(sentData, '0.path', '690.1033');

            assert.notInclude(sentData[0].vars, '1042=', 'UA was sent');
            done();
        }, 10);
    });

    describe('normalize', () => {
        function assertNormalizeString(value, expected, message) {
            assert.equal(Ya.Rum.normalize(value), expected, message);
        }

        function assertNormalizeNumber(value, offset, expected, message) {
            assert.equal(Ya.Rum.normalize(value, offset), expected, message);
        }

        it('should encode string', () => {
            assertNormalizeString('', '');
            assertNormalizeString('a="b"', 'a%3D%22b%22');
        });

        it('should leave 3 decimal digits for number', () => {
            assertNormalizeNumber(0, 0, 0);
            assertNormalizeNumber(10, 0, 10);
            assertNormalizeNumber(1.123456, 0, 1.123);
            assertNormalizeNumber(1.666666, 0, 1.667);
        });

        it('should subtract offset from number', () => {
            assertNormalizeNumber(10, 0, 10);
            assertNormalizeNumber(10, 10, 0);
        });
    });

    describe('pushTimingTo', () => {
        it('should push existing entries and skip non-existent entries', () => {
            const vars = [];
            const timing = { domainLookupStart: 1516376007679 };

            Ya.Rum.pushTimingTo(vars, timing);
            assert.deepEqual(vars, ['2112=1516376007679']);
        });

        it('should allow to push zero entries in PerformanceResourceTiming', () => {
            const vars = ['1701=81'];
            const resourceTiming = {
                connectEnd: 0,
                connectStart: 0,
                decodedBodySize: 0,
                domainLookupEnd: 0,
                domainLookupStart: 0,
                duration: 31.110000000000014,
                encodedBodySize: 0,
                entryType: 'resource',
                fetchStart: 746.575,
                initiatorType: 'css',
                name: 'https://favicon.yandex.net/favicon/' +
                    'www.jv.ru/ru.m.wikipedia.org/bbf.ru/www.banktestov.ru/mtests.ru/dic.academic.ru/www.sunhome.ru/' +
                    'lyna.info/www.topglobus.ru/m.passion.ru?_wrapped=true&color=255%2C255%2C255%2C0',
                nextHopProtocol: 'http/1.1',
                redirectEnd: 0,
                redirectStart: 0,
                requestStart: 0,
                responseEnd: 777.6850000000001,
                responseStart: 0,
                secureConnectionStart: 0,
                startTime: 746.575,
                transferSize: 0
            };

            Ya.Rum.pushTimingTo(vars, resourceTiming);
            assert.deepEqual(vars.sort(), [
                '1701=81',
                '2109=0',
                '2110=0',
                '2111=746.575',
                '2112=0',
                '2113=0',
                '2114=0',
                '2115=0',
                '2116=0',
                '2117=0',
                '2119=0',
                '2120=777.685',
                '2136=31.11',
                '2322=746.575',
                '2323=0',
                '2886=0',
                '2887=0',
                '2888=resource',
                '2889=css',
                '2890=http%2F1.1'
            ]);
        });
    });

    describe('subPage', () => {
        beforeEach(() => {
            Ya.Rum._subpages = {};
        });

        function removeFn(obj) {
            const res = {};

            Object.keys(obj).forEach(paramName => {
                if (typeof obj[paramName] !== 'function') {
                    res[paramName] = obj[paramName];
                }
            });

            return res;
        }

        it('should increase index for same pages', () => {
            const page1 = Ya.Rum.makeSubPage('123');
            assert.deepEqual(removeFn(page1), {
                '2924': '123',
                '2925': 0,
                '689.2322': 123
            });

            const page2 = Ya.Rum.makeSubPage('123');
            assert.deepEqual(removeFn(page2), {
                '2924': '123',
                '2925': 1,
                '689.2322': 123
            });
        });

        it('should not increase index for different pages', () => {
            const page1 = Ya.Rum.makeSubPage('123');
            assert.deepEqual(removeFn(page1), {
                '2924': '123',
                '2925': 0,
                '689.2322': 123
            });

            const page2 = Ya.Rum.makeSubPage('124');
            assert.deepEqual(removeFn(page2), {
                '2924': '124',
                '2925': 0,
                '689.2322': 123
            });
        });

        it('should cancel subpage', () => {
            const page = Ya.Rum.makeSubPage('123');

            page.cancel();

            assert.equal(page.isCanceled(), true);
        });

        it('should not cancel subpage', () => {
            const page = Ya.Rum.makeSubPage('123');

            assert.equal(page.isCanceled(), false);
        });

        it('should create subpage with start time', () => {
            const page1 = Ya.Rum.makeSubPage('123', 1000);
            assert.deepEqual(removeFn(page1), {
                '2924': '123',
                '2925': 0,
                '689.2322': 1000
            });

            const page2 = Ya.Rum.makeSubPage('124', 0);
            assert.deepEqual(removeFn(page2), {
                '2924': '124',
                '2925': 0,
                '689.2322': 0
            });
        });
    });

    describe('setVars', () => {
        it('should use vars from setVars', done => {
            document.readyState = 'complete';
            reloadModule();

            Ya.Rum.setVars({
                '287': '1111'
            });

            Ya.Rum._settings.disableOnLoadTasks = true;

            setTimeout(() => {
                assert.deepEqual(sentData, [{
                    path: '690.1033',
                    vars: '287=1111,' + vars
                }]);
                done();
            }, 10);
        });
    });

    describe('sendTimeMark', () => {
        it('should handle subscribed mark listeners on sending', done => {
            document.readyState = 'complete';
            reloadModule();

            Ya.Rum._settings.disableOnLoadTasks = true;

            const counterId = '123';
            let savedValue;
            Ya.Rum.on(counterId, markValue => savedValue = markValue);

            setTimeout(() => {
                Ya.Rum.sendTimeMark(counterId, 123);
                assert.equal(savedValue, 123);
                done();
            }, 0);
        });
    });

    describe('sendDelta', () => {
        it('should send delta with custom params', done => {
            document.readyState = 'complete';
            reloadModule();

            Ya.Rum._settings.disableOnLoadTasks = true;

            const counterId = '123';

            setTimeout(() => {
                Ya.Rum.sendDelta(counterId, 321, { info: 'test' });

                const vars = sentData[1].vars.split(',');

                assert.include(vars, '207.1428=123');
                assert.include(vars, '2877=321');
                assert.include(vars, 'info=test');

                done();
            }, 0);
        });
    });

    describe('addCustomParamsToVars', () => {
        it('should overwrite custom params', done => {
            document.readyState = 'complete';
            reloadModule();

            const counterId = '123';
            Ya.Rum.setVars({ a: 1, b: 2 });
            Ya.Rum._settings.disableOnLoadTasks = true;

            setTimeout(() => {
                const data = sentData;
                Ya.Rum.sendTimeMark(counterId, 123, true, { a: 3 });
                assert.notInclude(data[1].vars, 'a=1');
                assert.include(data[1].vars, 'a=3');
                done();
            }, 0);
        });
    });
});
