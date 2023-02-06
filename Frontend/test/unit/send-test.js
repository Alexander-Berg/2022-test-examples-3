const assert = require('assert');

function reloadModule(moduleName) {
    delete require.cache[require.resolve(moduleName)];
    require(moduleName);
}

describe('RUM send', function() {
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
    });

    beforeEach(function() {
        sentData = null;
        window.Ya = Object.assign({}, origYa);
    });

    after(function() {
        ['Ya', 'XMLHttpRequest'].forEach(name => delete window[name]);
        delete global.window;
    });

    it('should add Ya.Rum.send function', function() {
        assert.equal(typeof Ya.Rum.send, 'function');
    });

    it('should send single data entry', function(done) {
        send('path', 'vars', ['param1', 'param2']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'path=path', 'slots=', 'vars=vars', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });

    it('should join sequential calls in a batch', function(done) {
        send('path1', 'vars1', ['param1', 'param2']);
        send('path2', 'vars2', ['param3', 'param4']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'path=path1', 'slots=', 'vars=vars1', 'cts=1234567890000', '*'],
                    ['', 'reqid=', 'param3', 'param4', 'path=path2', 'slots=', 'vars=vars2', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });

    it('should not join calls separated with more than 15ms', function(done) {
        send('path1', 'vars1', ['param1']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'path=path1', 'slots=', 'vars=vars1', 'cts=1234567890000', '*']
                ]);

            send('path2', 'vars2', ['param2']);
            setTimeout(function() {
                assert.deepEqual(splitData(sentData),
                    [
                        ['', 'reqid=', 'param2', 'path=path2', 'slots=', 'vars=vars2', 'cts=1234567890000', '*']
                    ]);
                done();
            }, 20);
        }, 20);
    });

    it('should work with empty slots', function(done) {
        delete window.Ya.Rum._settings.slots;

        send('path', 'vars', ['param1', 'param2']);
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'path=path', 'vars=vars', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });

    it('should work with baobab-events', function(done) {
        delete window.Ya.Rum._settings.slots;

        send(null, null, ['param1', 'param2'], '{event:"click",id:"385739294"}');
        setTimeout(function() {
            assert.deepEqual(splitData(sentData),
                [
                    ['', 'reqid=', 'param1', 'param2', 'events={event:"click",id:"385739294"}', 'cts=1234567890000', '*']
                ]);
            done();
        }, 20);
    });
});
