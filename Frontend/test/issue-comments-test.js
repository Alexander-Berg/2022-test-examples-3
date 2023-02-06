/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues/WEATHER-3300/comments')
    .reply(200, [{ text: 'first comment' }, { text: 'second comment' }])
    .post('/issues/WEATHER-3300/comments', { text: 'third comment' })
    .reply(201, { text: 'third comment' })

    .get('/issues/WEATHER-3300/comments/100001')
    .reply(200, { id: 100001, text: 'first comment' })
    .patch('/issues/WEATHER-3300/comments/100001', { text: 'first comment!' })
    .reply(200, { id: 100001, text: 'first comment!' })
    .delete('/issues/WEATHER-3300/comments/100001')
    .reply(204)

    .get('/issues/WEATHER-3300/comments/100002')
    .reply(404)
    .patch('/issues/WEATHER-3300/comments/100002')
    .reply(404)
    .delete('/issues/WEATHER-3300/comments/100002')
    .reply(404);

// for deprecated methods
nock(config.endpoint)
    .get('/issues/WEATHER-3300/comments')
    .reply(200, [{ text: 'first comment' }, { text: 'second comment' }])
    .post('/issues/WEATHER-3300/comments', { text: 'third comment' })
    .reply(201, { text: 'third comment' });

test('issue comments', function(t) {
    t.plan(11);

    client.issueComments('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of comments');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createIssueComment('WEATHER-3300', { text: 'third comment' })
        .then(function(data) {
            t.ok(data, 'should get comment');
            t.equal(data.text, 'third comment', 'should create new comment');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueComment('WEATHER-3300', '100001')
        .then(function(data) {
            t.ok(data, 'should get comment');
            t.equal(data.id, 100001, 'should match comment id');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateIssueComment('WEATHER-3300', '100001', { text: 'first comment!' })
        .then(function(data) {
            t.ok(data, 'should get comment');
            t.equal(data.text, 'first comment!', 'should update comment');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.deleteIssueComment('WEATHER-3300', '100001')
        .then(function() {
            t.pass('should delete comment');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueComment('WEATHER-3300', '100002')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.updateIssueComment('WEATHER-3300', '100002', { text: 'first comment!' })
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });

    client.deleteIssueComment('WEATHER-3300', '100002')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue');
        });
});

test('issue comments (deprecated methods)', function(t) {
    t.plan(3);

    client.getIssueComments('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of comments');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.addIssueComment('WEATHER-3300', { text: 'third comment' })
        .then(function(data) {
            t.ok(data, 'should get comment');
            t.equal(data.text, 'third comment', 'should create new comment');
        })
        .catch(function(err) {
            t.fail(err.message);
        });
});
