/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issuetypes')
    .reply(200, [])
    .post('/issuetypes', { key: 'new' })
    .reply(201, { key: 'new' })
    .get('/issuetypes/1')
    .reply(200, { id: 1, key: 'bug' })
    .get('/issuetypes/9999')
    .reply(404)
    .patch('/issuetypes/1', { key: 'updated' })
    .reply(200, { key: 'updated' })

    .get('/priorities')
    .reply(200, [])
    .post('/priorities', { key: 'new' })
    .reply(201, { key: 'new' })
    .get('/priorities/1')
    .reply(200, { id: 1, key: 'trivial' })
    .get('/priorities/9999')
    .reply(404)
    .patch('/priorities/1', { key: 'updated' })
    .reply(200, { key: 'updated' })

    .get('/resolutions')
    .reply(200, [])
    .post('/resolutions', { key: 'new' })
    .reply(201, { key: 'new' })
    .get('/resolutions/1')
    .reply(200, { id: 1, key: 'fixed' })
    .get('/resolutions/9999')
    .reply(404)
    .patch('/resolutions/1', { key: 'updated' })
    .reply(200, { key: 'updated' })

    .get('/statuses')
    .reply(200, [])
    .post('/statuses', { key: 'new' })
    .reply(201, { key: 'new' })
    .get('/statuses/1')
    .reply(200, { id: 1, key: 'open' })
    .get('/statuses/9999')
    .reply(404)
    .patch('/statuses/1', { key: 'updated' })
    .reply(200, { key: 'updated' });

test('issue type attribute', function(t) {
    t.plan(6);

    client.issueTypes()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issue types');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createIssueType({ key: 'new' })
        .then(function(data) {
            t.equal(data.key, 'new', 'should create new issue type');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueType('1')
        .then(function(data) {
            t.equal(typeof data, 'object', 'should get issue type');
            t.equal(data.key, 'bug', 'should match issue type key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateIssueType('1', { key: 'updated' })
        .then(function(data) {
            t.equal(data.key, 'updated', 'should update issue type');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getIssueType('9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue type');
        });
});

test('issue priority attribute', function(t) {
    t.plan(6);

    client.priorities()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issue priorities');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createPriority({ key: 'new' })
        .then(function(data) {
            t.equal(data.key, 'new', 'should create new issue priority');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getPriority('1')
        .then(function(data) {
            t.equal(typeof data, 'object', 'should get issue priority');
            t.equal(data.key, 'trivial', 'should match issue priority key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updatePriority('1', { key: 'updated' })
        .then(function(data) {
            t.equal(data.key, 'updated', 'should update issue priority');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getPriority('9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue priority');
        });
});

test('issue resolution attribute', function(t) {
    t.plan(6);

    client.resolutions()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issue resolutions');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createResolution({ key: 'new' })
        .then(function(data) {
            t.equal(data.key, 'new', 'should create new issue resolution');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getResolution('1')
        .then(function(data) {
            t.equal(typeof data, 'object', 'should get issue resolution');
            t.equal(data.key, 'fixed', 'should match issue resolution key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateResolution('1', { key: 'updated' })
        .then(function(data) {
            t.equal(data.key, 'updated', 'should update issue resolution');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getResolution('9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue resolution');
        });
});

test('issue status attribute', function(t) {
    t.plan(6);

    client.statuses()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of issue statuses');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createStatus({ key: 'new' })
        .then(function(data) {
            t.equal(data.key, 'new', 'should create new issue status');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getStatus('1')
        .then(function(data) {
            t.equal(typeof data, 'object', 'should get issue status');
            t.equal(data.key, 'open', 'should match issue status key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateStatus('1', { key: 'updated' })
        .then(function(data) {
            t.equal(data.key, 'updated', 'should update issue status');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getStatus('9999')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing issue status');
        });
});
