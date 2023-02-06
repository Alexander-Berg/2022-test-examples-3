describe('i-counters', function() {
    describe('__error', function() {
        describe('Ya.isExternalError', function() {
            window.Ya.serverHostname = 'yandex.ru';
            window.Ya.clientHostname = 'yandex.ru';
            window.Ya.isAndroidWebview = false;
            window.Ya.isIOSWebview = false;

            var isExternalError = window.Ya.isExternalError;

            function assertIsExternal(url, message, stack) {
                assert.isTrue(isExternalError(url, message, stack),
                    'Error should be external:' +
                    '\nurl: ' + url +
                    '\nmessage: ' + message +
                    '\nstack: ' + stack);
            }

            function assertIsNotExternal(url, message, stack) {
                assert.isFalse(isExternalError(url, message, stack),
                    'Error should not be external:' +
                    '\nurl: ' + url +
                    '\nmessage: ' + message +
                    '\nstack: ' + stack);
            }

            it('should return false if empty url given', function() {
                assertIsNotExternal(undefined);
            });

            it('should return true if external url given', function() {
                assertIsExternal('https://gc.kis.v2.scr.kaspersky-labs.com/ololo/script.main.js');
                assertIsExternal('file:///C:/My Documents/ALetter.html');
            });

            it('should return true if extension url given', function() {
                assertIsExternal('miscellaneous_bindings:236');
                assertIsExternal('extension_bindings:ololo');
                assertIsExternal('chrome://savefrom/content/main.js');
            });

            it('should return false if unknown url given', function() {
                assertIsNotExternal('https://yandex.ru/search/?text=chrome:');
            });

            it('should return false if unknown url and message given', function() {
                assertIsNotExternal('https://localhost:3443/search/?text=ucapi', 'Script error.');
            });

            it('should return true if AdGuard error given', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: window.__adgRemoveDirect is not a function');

                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'adUsageStorageVars\' of undefined');

                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Object.values is not a function',
                    'TypeError: Object.values is not a function\n' +
                    '    at findAds (AdGuard%20Extra.user.js:71:7335)\n' +
                    '    at hideYaDirectAds (AdGuard%20Extra.user.js:71:7938)\n' +
                    '    at MutationObserver.eval (AdGuard%20Extra.user.js:71:8766)');

                assertIsExternal(
                    'localhost',
                    'Uncaught ReferenceError: GM_addStyle is not defined',
                    'ReferenceError: GM_addStyle is not defined\n' +
                    '    at loadStyle (Adguard%20Assistant.user.js:84:440)\n' +
                    '    at afterContentLoaded (Adguard%20Assistant.user.js:117:1)');
            });

            it('should return true if UCBrowser error given', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught ReferenceError: ucapi is not defined');

                assertIsExternal(
                    'localhost',
                    'Uncaught ReferenceError: __show__deepen is not defined');
            });

            it('should return true if YaBro error given', function() {
                assertIsExternal(
                    'localhost',
                    'Can\'t find variable: __gCrWeb');
            });

            it('should return true for Firefox extension error', function() {
                assertIsExternal(
                    'moz-extension://91932229-d4de-4f71-ae0f-f3ae0d9f0899/userscript.html?id=9ab3eea8-7098-4e25-9895-5003d16e95a0',
                    'NS_ERROR_FILE_CORRUPTED: ', // именно так, с пробелом
                    'tms_9ab3eea8_7098_4e25_9895_5003d16e95a0/</<@moz-extension://91932229-d4de-4f71-ae0f-f3ae0d9f0899/userscript.html?id=9ab3eea8-7098-4e25-9895-5003d16e95a0:999:13\n' +
                    'g</<@eval:2:479\n' +
                    'ja/c[d]@eval line 1 > Function:52:483\n'
                );
            });

            it('should return true for Chrome extension error', function() {
                assertIsExternal(
                    'https://www.yandex.ru/search/?text=hd%201080',
                    'Uncaught SyntaxError: Unexpected token F',
                    'SyntaxError: Unexpected token F\n' +
                    '    at Object.parse (native)\n' +
                    '    at CDStorage._handleMessage (chrome-extension://hhdjegdllbnhgdbkamlghkfcjnkilinm/extension-chrome.js?channel=CPI_coin32_1214:9452:29)\n' +
                    '    at chrome-extension://hhdjegdllbnhgdbkamlghkfcjnkilinm/extension-chrome.js?channel=CPI_coin32_1214:9366:74'
                );
            });

            it('should return true if KasperskyLab JSON syntax error given', function() {
                assertIsExternal(
                    'localhost',
                    'SyntaxError: JSON syntax error',
                    'Object.ThrowError (https://yandex.ru/FD126C42-EBFA-4E12-B309-BB3FDD723AC1/main.js?attr=uYw:58:19)\n' +
                    'Object.Parse (https://yandex.ru/FD126C42-EBFA-4E12-B309-BB3FDD723AC1/main.js?attr=uYw:171:22)\n' +
                    'Object.exports.parse [as JSONParse] (https://yandex.ru/FD126C42-EBFA-4E12-B309-BB3FDD723AC1/main.js?attr=uYw:176:23)\n' +
                    'https://yandex.ru/FD126C42-EBFA-4E12-B309-BB3FDD723AC1/main.js?attr=uYw:1104:42\n' +
                    'XMLHttpRequest.AsyncCall.request.onload (https://yandex.ru/FD126C42-EBFA-4E12-B309-BB3FDD723AC1/main.js?attr=uYw:1044:29)');

                assertIsExternal(
                    'localhost',
                    'SyntaxError: JSON syntax error',
                    'Object.a (https://yandex.ru/C04F3D4D1A054D43B13CDEEFDD2B24BC/51307101-02CC-EB47-A5AB-F15983AF8CA4/main.js:4:388)\n' +
                    'Object.e (https://yandex.ru/C04F3D4D1A054D43B13CDEEFDD2B24BC/51307101-02CC-EB47-A5AB-F15983AF8CA4/main.js:7:50)\n' +
                    'Object.g.JSONParse (https://yandex.ru/C04F3D4D1A054D43B13CDEEFDD2B24BC/51307101-02CC-EB47-A5AB-F15983AF8CA4/main.js:7:147)\n' +
                    'XMLHttpRequest.e.onload (https://yandex.ru/C04F3D4D1A054D43B13CDEEFDD2B24BC/51307101-02CC-EB47-A5AB-F15983AF8CA4/main.js:19:149)');

                assertIsExternal(
                    'localhost',
                    'SyntaxError: JSON syntax error',
                    'a@https://yandex.ru/DC329F50986D4401A3A10463F4B6E0E7/DB546121-3B15-1D40-9E55-2C1CC19C948A/main.js:4:388\n' +
                    'e@https://yandex.ru/DC329F50986D4401A3A10463F4B6E0E7/DB546121-3B15-1D40-9E55-2C1CC19C948A/main.js:7:50\n' +
                    'KasperskyLab</g.JSONParse@https://yandex.ru/DC329F50986D4401A3A10463F4B6E0E7/DB546121-3B15-1D40-9E55-2C1CC19C948A/main.js:7:147\n' +
                    'u/e.onload@https://yandex.ru/DC329F50986D4401A3A10463F4B6E0E7/DB546121-3B15-1D40-9E55-2C1CC19C948A/main.js:19:149');
            });

            it('should return true if Oppo getReadMode* errors given', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'getReadModeConfig\' of undefined',
                    'TypeError: Cannot read property \'getReadModeConfig\' of undefined\n at <anonymous>:1:76'
                );
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'getReadModeExtract\' of undefined',
                    'TypeError: Cannot read property \'getReadModeExtract\' of undefined\n at <anonymous>:1:77'
                );
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'getReadModeRender\' of undefined',
                    'TypeError: Cannot read property \'getReadModeRender\' of undefined\n at <anonymous>:1:76'
                );

                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'getReadModeSomething\' of undefined',
                    'TypeError: Cannot read property \'getReadModeSomething\' of undefined\n at <anonymous>:1:76'
                );
            });

            it('should return true if topMsg is not defined error given', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught ReferenceError: topMsg is not defined',
                    'ReferenceError: topMsg is not defined\n' +
                    'at HTMLImageElement.onload (https://yandex.ru/search/?text=%D1%81%D0%BB%D0%BE%D0%B1%D0%BE%D0%B4%D0%B0%20%D0%BB%D1%8F%D0%BD%D0%B3%D0%B0%D1%81%D1%8B&clid=2270455&banerid=220209301469453624527664125101&win=239&&lr=46:1:1)'
                );
            });

            it('should return true if instantSearchSDKJSBridgeClearHighlight is not found', function() {
                assertIsExternal(
                    'localhost',
                    'ReferenceError: Can\'t find variable: instantSearchSDKJSBridgeClearHighlight',
                    'at global code (https://yandex.ru/search/touch/?text=561577a2-8807-4184-b341-886d7a2d5961%7CK1V1165610&clid=2411726&lr=55:1:106'
                );
            });

            it('should return true if window.cb* is not a function error given', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: window.cb16273997954303926 is not a function',
                    'TypeError: window.cb16273997954303926 is not a function\n' +
                    'at <anonymous>:1:30'
                );
            });

            it('should return true if error produced on non-Serp page', function() {
                var oldClientHostname = window.Ya.clientHostname;
                window.Ya.clientHostname = 'not-yandex.ru';
                assertIsExternal('https://yandex.ru/search/?text=test');
                window.Ya.clientHostname = oldClientHostname;
            });

            it('should return true for Adblock Plus extension error (Firefox)', function() {
                assertIsExternal(
                    'localhost',
                    'SecurityError: Permission denied to access property "Element" on cross-origin object',
                    'wrapPropertyAccess@blob:https://yandex.ru/dcbfb716-ec5c-4064-a9b5-fc71f0aa2a49:224:15\n' +
                    'abortOnRead@blob:https://yandex.ru/dcbfb716-ec5c-4064-a9b5-fc71f0aa2a49:108:21\n' +
                    'queryAndProxyIframe@blob:https://yandex.ru/dcbfb716-ec5c-4064-a9b5-fc71f0aa2a49:304:22\n' +
                    'get/<@blob:https://yandex.ru/dcbfb716-ec5c-4064-a9b5-fc71f0aa2a49:352:26\n' +
                    'hAb@https://yastatic.net/s3/web4static/_/v2/Rse3T1tVB9l5GRjvTwSWuKF2atM.js:3:171950\n' +
                    '_secondLaunch@https://yastatic.net/s3/web4static/_/v2/Rse3T1tVB9l5GRjvTwSWuKF2atM.js:3:175138'
                );
            });

            it('should return true for Adblock Plus extension error (Chromium)', function() {
                assertIsExternal(
                    'localhost',
                    'Error: Blocked a frame with origin "https://yandex.ru" from accessing a cross-origin frame',
                    'at wrapPropertyAccess (<anonymous>:224:21)\n' +
                    'at abortOnRead (<anonymous>:108:3)\n' +
                    'at queryAndProxyIframe (<anonymous>:304:11)\n' +
                    'at HTMLHeadElement.<anonymous> (<anonymous>:352:26)\n' +
                    'at u.loadMetrikaScript (https://yastatic.net/s3/web4static/_/v2/GNBu9u2002Yy2tEHo37_toEN5Io.js:3:99226)'
                );
            });

            it('should return true if error is produced by browser internals', function() {
                assertIsExternal(
                    'localhost',
                    'Uncaught TypeError: Cannot read property \'disconnect\' of null',
                    "TypeError: Cannot read property 'disconnect' of null\n at <anonymous>:1:825"
                );
            });

            it('should return true if error is produced by unknown browser extension', function() {
                assertIsExternal(
                    'localhost',
                    "Uncaught SyntaxError: Failed to execute 'querySelectorAll' on 'Element': 'iframe:not([src]:not([data-clndr]))' is not a valid selector.",
                    "Error: Failed to execute 'querySelectorAll' on 'Element': 'iframe:not([src]:not([data-clndr]))' is not a valid selector.\n" +
                    'at findIframes (clickcnt.js:1777:36)\n' +
                    'at Function.findElements (clickcnt.js:1800:38)\n' +
                    'at MutationObserver.<anonymous> (clickcnt.js:1475:22)'
                );
            });

            it('should return true if error is produced by page in WebView', function() {
                window.Ya.isAndroidWebview = true;
                assertIsExternal('localhost', 'Uncaught TypeError');
                window.Ya.isAndroidWebview = false;

                window.Ya.isIOSWebview = true;
                assertIsExternal('localhost', 'Uncaught TypeError');
                window.Ya.isIOSWebview = false;
            });

            it('should return true if error stacktrace have only anonymous functions', function() {
                assertIsExternal(
                    'localhost',
                    "Uncaught InvalidCharacterError: Failed to execute 'setAttribute' on 'Element': '' is not a valid attribute name.",
                    "Error: Failed to execute 'setAttribute' on 'Element': '' is not a valid attribute name.\n" +
                    'at Object.t (<anonymous>:1:34746)\n' +
                    'at Object.l (<anonymous>:1:828)'
                );
            });

            it('should return false if error stacktrace have both declared and anonymous functions', function() {
                assertIsNotExternal(
                    'localhost',
                    "Uncaught InvalidCharacterError: Failed to execute 'setAttribute' on 'Element': '' is not a valid attribute name.",
                    "Error: Failed to execute 'setAttribute' on 'Element': '' is not a valid attribute name.\n" +
                    'at Object.t (<anonymous>:1:34746)\n' +
                    'at Object.l (<anonymous>:1:828)\n' +
                    'at Object.d (script.js:1:1)'
                );
            });
        });

        describe('Ya.onerror', function() {
            beforeEach(function() {
                sinon.spy(window, 'wb');
            });

            afterEach(function() {
                wb.restore();
            });

            it('should accept plain arguments', function() {
                Ya.onerror('message', 'url', 11, 22, undefined, undefined);

                assert.calledOnce(wb);

                var vars = wb.args[0][2];
                assert.include(vars, '-msg=message', 'should log message');
                assert.include(vars, '-url=url', 'should log url');
                assert.include(vars, '-line=11', 'should log line');
                assert.include(vars, '-col=22', 'should log column');

                assert.deepEqual(
                    Ya.jserrors[Ya.jserrors.length - 1],
                    {
                        message: 'message',
                        url: 'url',
                        line: 11,
                        col: 22,
                        error: undefined,
                        additional: undefined
                    },
                    'should add error to Ya.jserrors'
                );
            });

            it('should escape asterisks', function() {
                Ya.onerror('message * with * asterisks',
                    'url * with * asterisks', 11, 22,
                    { stack: 'stack * with * asterisks\n  at __mod_js_*' },
                    undefined);

                assert.calledOnce(wb);

                var vars = wb.args[0][2];
                assert.include(vars, '-msg=message%20%2A%20with%20%2A%20asterisks',
                    'should escape asterisks in message');
                assert.include(vars, '-url=url%20%2A%20with%20%2A%20asterisks',
                    'should escape asterisks in url');
                assert.include(vars, '-stack=stack%20%2A%20with%20%2A%20asterisks%0A%20%20at%20__mod_js_%2A',
                    'should escape asterisks in stack');
            });

            function getErrorVars(cbName) {
                Ya.onerror(
                    'Uncaught TypeError: ' + cbName + ' is not a function',
                    'url',
                    11,
                    22,
                    { stack: '<unknown> (https://pass.yandex.ru/accounts?callback=' +
                            cbName + '&yu=8665591751539626863:1:1)' },
                    undefined
                );

                assert.calledOnce(wb);
                return wb.args[0][2];
            }

            it('should replace callback name in error message when callback does not exist', function() {
                // Имя коллбэка не существует в window
                var vars = getErrorVars('c795226969740');

                assert.include(vars,
                    '-msg=' + encodeURIComponent('TypeError: JSONP callback is not set'),
                    'should replace unique callback name in error message');
            });

            it('should replace callback name in error message when callback is undefined', function() {
                // Имя коллбэка существует, но значение равно undefined вместо function
                var cbName = 'c11111';
                window[cbName] = undefined;
                var vars = getErrorVars(cbName);
                delete window[cbName];

                assert.include(vars,
                    '-msg=' + encodeURIComponent('TypeError: JSONP callback is set to undefined'),
                    'should replace unique callback name in error message');
            });

            it('should replace callback name in error message when callback is null', function() {
                // Имя коллбэка существует, но значение равно null вместо function
                var cbName = 'c22222';
                window[cbName] = null;
                var vars = getErrorVars(cbName);
                delete window[cbName];

                assert.include(vars,
                    '-msg=' + encodeURIComponent('TypeError: JSONP callback is set to null'),
                    'should replace unique callback name in error message');
            });

            it('should replace callback name for window.cb% errors', function() {
                Ya.onerror(
                    'Uncaught TypeError: window.cb16279737314186608 is not a function',
                    'url',
                    11,
                    22,
                    undefined,
                    undefined
                );

                assert.calledOnce(wb);

                var vars = wb.args[0][2];
                assert.include(vars,
                    '-msg=' + encodeURIComponent('TypeError: callback (cb\\d{15,17}) is not a function'),
                    'should replace unique callback name in error message');
            });

            describe('static version', function() {
                var oldBEM;

                beforeEach(function() {
                    oldBEM = window.BEM;

                    window.BEM = {
                        blocks: {
                            'i-global': {
                                param: sinon.stub().withArgs('staticVersion').returns('my_static_version')
                            }
                        }
                    };
                });

                afterEach(function() {
                    window.BEM = oldBEM;
                });

                it('should add server static version in additionals if BEM exists', function() {
                    Ya.onerror('message', 'url', 11, 22, undefined, undefined);

                    assert.calledOnce(wb);

                    var vars = wb.args[0][2];
                    assert.include(
                        vars,
                        '-additional=%7B%22sameStaticVersion%22%3Afalse%2C%22serverStaticVersion%22%3A%22my_static_version%22%7D',
                        'should add version to additionals'
                    );
                });
            });

            describe('loggedin', function() {
                var oldBEM;

                beforeEach(function() {
                    oldBEM = window.BEM;

                    window.BEM = {
                        blocks: {
                            'i-global': {
                                param: sinon.stub().withArgs('login').returns('test-login')
                            }
                        }
                    };
                });

                afterEach(function() {
                    window.BEM = oldBEM;
                });

                it('should add loggedin if BEM exists', function() {
                    Ya.onerror('message', 'url', 11, 22, undefined, undefined);

                    assert.calledOnce(wb);

                    var vars = wb.args[0][2];

                    assert.include(vars, '-loggedin=true');
                });
            });
        });

        describe('Ya.logSerpErrorMessagesMap', function() {
            beforeEach(function() {
                window.Ya.jserrors = [];
            });

            it('should limit the max number of same errors', function() {
                for (var i = 0; i < 20; i++) {
                    Ya.onerror('Error message', 'Error url', 1, 2, undefined, undefined);
                }

                assert.isTrue(window.Ya.jserrors.length === 10, 'Should be only 10 errors');
            });
        });

        describe('Ya.isClientError', function() {
            it('should return true if label exists', function() {
                var result = Ya.isClientError(null, 'testMessage', null, 'testLabel');

                assert.isTrue(result, 'Should return true');
            });

            it('should return true on ResizeObserver errors', function() {
                var commonResult = Ya.isClientError(null, 'ResizeObserver loop limit exceeded', null, null);
                // ошибка, специфичная для firefox
                var firefoxResult = Ya.isClientError(null, 'ResizeObserver loop completed with undelivered notifications.', null, null);

                assert.isTrue(commonResult, 'Should return true on common error');
                assert.isTrue(firefoxResult, 'Should return true on firefox specific error');
            });
        });
    });
});
