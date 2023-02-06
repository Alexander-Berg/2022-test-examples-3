/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues/WEATHER-3300/transitions')
    .reply(200, [{ id: 'reopen', to: { key: 'open' } }])
    .post('/issues/WEATHER-3300/transitions/reopen/_execute')
    .reply(200, [{ id: 'start_progress', to: { key: 'inProgress' } }])
    .post('/issues/WEATHER-3300/transitions/close/_execute', { resolution: 'fixed' })
    .reply(200, [{ id: 'reopen', to: { key: 'open' } }])

    .get('/issues/WEATHER-9999/transitions')
    .reply(404)
    .post('/issues/WEATHER-9999/transitions/reopen/_execute')
    .reply(404);

// For deprecated methods
nock(config.endpoint)
    .get('/issues/WEATHER-3300/transitions')
    .reply(200, [{ id: 'reopen', to: { key: 'open' } }])
    .get('/issues/WEATHER-9999/transitions')
    .reply(404);

test('issue workflow', function(t) {
    t.plan(10);

    client.issueTransitions('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of transitions');
            t.ok(data.length, 'should get list with non-zero length');
            t.ok(data[0].id, 'transition should contain current status id');
            t.ok(data[0].to, 'transition should contain target status');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.executeTransition('WEATHER-3300', 'reopen')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should contain list of the new status transitions');
            t.ok(data.length, 'should contain list with non-zero length');
            t.ok(data[0].id, 'transition should contain current status id');
            t.ok(data[0].to, 'transition should contain target status');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.issueTransitions('WEATHER-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.executeTransition('WEATHER-9999', 'reopen')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});

test('issue workflow (deprecated methods)', function(t) {
    t.plan(5);

    client.getIssueTransitions('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of transitions');
            t.ok(data.length, 'should get list with non-zero length');
            t.ok(data[0].id, 'transition should contain current status id');
            t.ok(data[0].to, 'transition should contain target status');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueTransitions('WEATHER-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});
