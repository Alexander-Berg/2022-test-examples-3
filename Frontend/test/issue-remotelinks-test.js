/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues/WEATHER-3300/remotelinks')
    .reply(200, [{ id: 1, direction: 'inward' }, { id: 2, direction: 'outward' }])
    .get('/issues/WEATHER-9999/remotelinks')
    .reply(404)
    .get('/issues/WEATHER-3300/remotelinks/1')
    .reply(200, { id: 1, direction: 'outward', object: { key: '1590', application: { key: 'app' } } })
    .get('/issues/WEATHER-3300/remotelinks/3')
    .reply(404)
    .post('/issues/WEATHER-3300/remotelinks', { relationship: 'relates', key: '1590', origin: 'app' })
    .reply(201, { type: { id: 'relates' }, direction: 'outward', object: { key: '1590', application: { id: 'app' } } })
    .delete('/issues/WEATHER-3300/remotelinks/2')
    .reply(204)
    .delete('/issues/WEATHER-3300/remotelinks/3')
    .reply(404);

// For deprecated methods
nock(config.endpoint)
    .get('/issues/WEATHER-3300/remotelinks')
    .reply(200, [{ id: 1, direction: 'inward' }, { id: 2, direction: 'outward' }])
    .get('/issues/WEATHER-9999/remotelinks')
    .reply(404);

test('issue remotelinks', function(t) {
    t.plan(14);

    client.issueRemoteLinks('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of comments');
            t.equal(data.length, 2, 'should get list with length 2');
            t.equal(data[0].direction, 'inward', 'should contain first inward remote link');
            t.equal(data[1].direction, 'outward', 'should contain second outward remote link');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.issueRemoteLinks('WEATHER-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.getIssueRemoteLink('WEATHER-3300', '1')
        .then(function(data) {
            t.ok(data, 'should get remote link');
            t.equal(data.id, 1, 'should match remote link id');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueRemoteLink('WEATHER-3300', '3')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing remote link');
        });

    client.createIssueRemoteLink('WEATHER-3300', { relationship: 'relates', key: '1590', origin: 'app' })
        .then(function(data) {
            t.ok(data, 'should get remote link');
            t.equal(data.object.key, '1590', 'should carry object key');
            t.equal(data.type.id, 'relates', 'should carry the relationship');
            t.equal(data.object.application.id, 'app', 'should carry the application id');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.deleteIssueRemoteLink('WEATHER-3300', '2')
        .then(function() {
            t.pass('should delete link');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.deleteIssueRemoteLink('WEATHER-3300', '3')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing link');
        });
});

test('issue remotelinks (deprecated methods)', function(t) {
    t.plan(5);

    client.getIssueRemoteLinks('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of comments');
            t.equal(data.length, 2, 'should get list with length 2');
            t.equal(data[0].direction, 'inward', 'should contain first inward remote link');
            t.equal(data[1].direction, 'outward', 'should contain second outward remote link');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueRemoteLinks('WEATHER-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});
