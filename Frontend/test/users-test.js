/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/users')
    .reply(200, [])

    .get('/users/torvalds')
    .reply(200, { login: 'torvalds' })

    .get('/users/FOOBAR-9999')
    .reply(404);

test('users', function(t) {
    t.plan(4);

    client.users()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of users');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getUser('torvalds')
        .then(function(data) {
            t.ok(typeof data === 'object', 'should get user');
            t.equal(data.login, 'torvalds', 'should match user key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getUser('FOOBAR-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing user');
        });
});
