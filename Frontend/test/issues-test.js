/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues')
    .reply(200, [])
    .get('/issues').query({ query: 'Queue: WEATHER Key: WEATHER-1, WEATHER-2' })
    .reply(200, [{ key: 'WEATHER-1' }, { key: 'WEATHER-2' }])
    .post('/issues', { summary: 'New issue' })
    .reply(201, { summary: 'New issue' })

    .get('/issues/WEATHER-3300')
    .reply(200, { key: 'WEATHER-3300' })
    .patch('/issues/WEATHER-3300', { summary: 'Updated issue!' })
    .reply(200, { key: 'WEATHER-3300', summary: 'Updated issue!' })

    .get('/issues/FOOBAR-9999')
    .reply(404)
    .patch('/issues/FOOBAR-9999')
    .reply(404);

test('issues', function(t) {
    t.plan(11);

    client.issues()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issues');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.issues({ query: 'Queue: WEATHER Key: WEATHER-1, WEATHER-2' })
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issues');
            t.equal(data.length, 2, 'should get list with length 2');
            t.equal(data[0].key, 'WEATHER-1', 'should contain first issue key');
            t.equal(data[1].key, 'WEATHER-2', 'should contain second issue key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createIssue({ summary: 'New issue' })
        .then(function(data) {
            t.equal(data.summary, 'New issue', 'should create new issue');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssue('WEATHER-3300')
        .then(function(data) {
            t.ok(typeof data === 'object', 'should get issue');
            t.equal(data.key, 'WEATHER-3300', 'should match issue key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateIssue('WEATHER-3300', { summary: 'Updated issue!' })
        .then(function(data) {
            t.equal(data.summary, 'Updated issue!', 'should update issue');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssue('FOOBAR-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.updateIssue('FOOBAR-9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});
