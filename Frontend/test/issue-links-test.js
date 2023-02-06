/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues/WEATHER-3300/links')
    .reply(200, [{ id: '100001' }, { id: '100002' }])
    .post('/issues/WEATHER-3300/links', { relationship: 'relates', issue: 'WEATHER-3302' })
    .reply(201, { type: { id: 'relates' }, object: { key: 'WEATHER-3302' } })
    .get('/issues/WEATHER-3300/links/100001')
    .reply(200, { id: 100001, object: { key: 'WEATHER-3302' } })
    .delete('/issues/WEATHER-3300/links/100001')
    .reply(204)

    .get('/issues/WEATHER-3300/links/100002')
    .reply(404)
    .delete('/issues/WEATHER-3300/links/100002')
    .reply(404);

test('issue links', function(t) {
    t.plan(9);

    client.issueLinks('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of links');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createIssueLink('WEATHER-3300', { relationship: 'relates', issue: 'WEATHER-3302' })
        .then(function(data) {
            t.ok(data, 'should get link');
            t.equal(data.object.key, 'WEATHER-3302', 'should carry object key');
            t.equal(data.type.id, 'relates', 'should carry the relationship');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueLink('WEATHER-3300', '100001')
        .then(function(data) {
            t.ok(data, 'should get link');
            t.equal(data.id, 100001, 'should match link id');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.deleteIssueLink('WEATHER-3300', '100001')
        .then(function() {
            t.pass('should delete link');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueLink('WEATHER-3300', '100002')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.deleteIssueLink('WEATHER-3300', '100002')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});
