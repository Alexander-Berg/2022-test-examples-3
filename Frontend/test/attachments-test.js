/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .post('/attachments', function(body) {
        return /Content-Disposition: form-data; name="file"; filename="name"/.test(body) && /file-content/.test(body);
    })
    .query({ filename: 'name' })
    .reply(201, { id: 'ABC' });

nock(config.endpoint)
    .get('/issues/WEATHER-3300/attachments')
    .reply(200, [{ id: 1, name: 'first.txt' }, { id: 2, name: 'second.txt' }])
    .get('/issues/WEATHER-3300/attachments/1/first.txt')
    .reply(200, 'text from first attachment')

    .get('/issues/WEATHER-3301/attachments')
    .reply(404)
    .get('/issues/WEATHER-3300/attachments/3/third.txt')
    .reply(404);

test('upload attachment', function(t) {
    t.plan(2);

    var file = new Buffer('file-content');
    client.uploadAttachment({ file: file }, { filename: 'name' })
        .then(function(data) {
            return JSON.parse(data);
        })
        .then(function(data) {
            t.equal(typeof data, 'object', 'should get data');
            t.ok(data.id, 'should get id of uploaded file');
        })
        .catch(function(err) {
            t.fail(err.message);
        });
});

test('issue attachments', function(t) {
    t.plan(2);

    client.issueAttachments('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of attachments');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.issueAttachments('WEATHER-3301')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on empty attachments');
        });
});

test('download attachment', function(t) {
    t.plan(3);

    client.downloadAttachment('WEATHER-3300', '1', 'first.txt')
        .then(function(data) {
            t.ok(data, 'should get attachment');
            t.equal(data, 'text from first attachment', 'should match attachment content');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.downloadAttachment('WEATHER-3300', '3', 'third.txt')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing attachments');
        });
});
