const assert = require('assert');

describe('RUM retries', function() {
    var sentCounters = [];
    var origYa;

    before(function() {
        global.window = global;
        window.Ya = {
            Rum: {
                enabled: true,
                _vars: {
                    '143': '123.123'
                },
                getVarsList: () => ['143=123.123'],
                sendCounter: (path, varsArr) => {
                    var vars = varsArr.filter(Boolean).join(',').replace(/=\d{4,}/g, '=12345');
                    sentCounters.push({ path, vars });
                }
            }
        };

        origYa = window.Ya;

        require('../../src/bundle/retries');
    });

    beforeEach(function() {
        window.Ya = Object.assign({}, origYa);
    });

    after(function() {
        ['Ya'].forEach(name => delete window[name]);
        delete global.window;
    });

    it('should add Ya.Rum.sendRetriesCount function', function() {
        assert.equal(typeof Ya.Rum.sendRetriesCount, 'function');
    });

    it('should send vars', function(done) {
        Ya.Rum.sendRetriesCount('test.ru', 1);

        setTimeout(function() {
            assert.deepEqual(sentCounters,
                [
                    {
                        path: '690.2096.3037',
                        vars: '143=123.123,13=test.ru,1385=1'
                    }
                ]);
            done();
        }, 20);
    });
});
