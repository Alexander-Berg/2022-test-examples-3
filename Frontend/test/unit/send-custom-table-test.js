const assert = require('assert');

function reloadModule(moduleName) {
    delete require.cache[require.resolve(moduleName)];
    require(moduleName);
}

describe('RUM send to custom table', function() {
    var sentData = null;
    var origYa;

    function splitData(data) {
        return (data || '')
            .replace(/cts=\d+/g, 'cts=1234567890000')
            .split('\r\n')
            .map(entry => entry.split('/'));
    }

    function send(path, vars, params, events) {
        Ya.Rum.send('unused-link', path, vars, 'unused-clck', events, 'unused-sts', params);
    }

    before(function() {
        // mock required browser environment
        global.window = global;
        window.Ya = {
            Rum: {
                _settings: {
                    clck: 'clck',
                    slots: []
                },
                getSetting: function(settingName) {
                    var setting = this._settings[settingName];

                    return setting === null ? null : setting || '';
                }
            }
        };

        origYa = window.Ya;

        window.XMLHttpRequest = function() {
        };
        XMLHttpRequest.prototype = {
            open: function() {
            },
            send: function(data) {
                sentData = data;
            }
        };

        window.navigator = {
            sendBeacon: function() {}
        };

        reloadModule('../../src/bundle/send');
        reloadModule('../../src/bundle/send-custom-table');
    });

    beforeEach(function() {
        sentData = null;
        window.Ya = Object.assign({}, origYa);
    });

    after(function() {
        ['Ya', 'XMLHttpRequest'].forEach(name => delete window[name]);
        delete global.window;
    });

    it('should throw if table prefix is not set', function() {
        assert.throws(() => {
            send('690.1033', 'vars', ['param1', 'param2']);
        });
    });

    it('should add table parameter if table prefix is set', function(done) {
        Ya.Rum._settings.tablePrefix = 'prefix';

        send('690.1033', 'vars', ['param1', 'param2']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'table=prefix_rum_timi', 'path=690.1033', 'slots=', 'vars=vars', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });

    it('should not add table parameter if path is unknown', function(done) {
        Ya.Rum._settings.tablePrefix = 'prefix';

        send('unknown-path', 'vars', ['param1', 'param2']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'path=unknown-path', 'slots=', 'vars=vars', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });
});
