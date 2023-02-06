/* eslint func-names: "off" */

var test = require('tape');
var Client = require('..');

test('init', function(t) {
    t.plan(15);

    t.throws(Client.init.bind(null, { token: 1 }),
        /token option must be a string/, 'should throw with invalid token option');
    t.throws(Client.init.bind(null, { token: true }),
        /token option must be a string/, 'should throw with invalid token option');
    t.throws(Client.init.bind(null, { token: {} }),
        /token option must be a string/, 'should throw with invalid token option');
    t.throws(Client.init.bind(null, { token: console.log }),
        /token option must be a string/, 'should throw with invalid token option');

    t.throws(Client.init.bind(null, { sessionId: 1 }),
        /sessionId option must be a string/, 'should throw with invalid sessionId option');
    t.throws(Client.init.bind(null, { sessionId: true }),
        /sessionId option must be a string/, 'should throw with invalid sessionId option');
    t.throws(Client.init.bind(null, { sessionId: {} }),
        /sessionId option must be a string/, 'should throw with invalid sessionId option');
    t.throws(Client.init.bind(null, { sessionId: console.log }),
        /sessionId option must be a string/, 'should throw with invalid sessionId option');

    t.throws(Client.init.bind(null, { tvm: 1 }),
        /tvm tickets must be a string/, 'should throw with invalid tvm option');
    t.throws(Client.init.bind(null, { tvm: true }),
        /tvm tickets must be a string/, 'should throw with invalid tvm option');
    t.throws(Client.init.bind(null, { tvm: {} }),
        /tvm tickets must be a string/, 'should throw with invalid tvm option');
    t.throws(Client.init.bind(null, { tvm: console.log }),
        /tvm tickets must be a string/, 'should throw with invalid tvm option');

    t.doesNotThrow(Client.init.bind(null, { token: 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx' }),
        'should not throw with valid token option');

    t.doesNotThrow(Client.init.bind(null, { sessionId: 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx' }),
        'should not throw with valid sessionId option');

    t.doesNotThrow(Client.init.bind(null, { tvm: { clientTicket: 'xx', serviceTicket: 'xx' } }),
        'should not throw with valid tvm option');
});
