/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/queues')
    .reply(200, [])
    .post('/queues', { key: 'WEATHER' })
    .reply(409)
    .post('/queues', { key: 'WEATHERAPP' })
    .reply(201, { key: 'WEATHERAPP' })

    .get('/queues/WEATHER')
    .reply(200, { key: 'WEATHER' })
    .patch('/queues/WEATHER', { name: 'Weather' })
    .reply(200, { name: 'Weather' })
    .delete('/queues/WEATHER')
    .reply(204)

    .get('/queues/FOOBAR').reply(404)
    .patch('/queues/FOOBAR').reply(404)
    .delete('/queues/FOOBAR').reply(404)

    .get('/queues/WEATHER/components')
    .reply(200, [{ queue: { key: 'WEATHER' } }])
    .get('/queues/WEATHER/versions')
    .reply(200, [{ queue: { key: 'WEATHER' } }])
    .get('/queues/WEATHER/projects')
    .reply(200, [{ queue: { key: 'WEATHER' } }]);

// for deprecated methods
nock(config.endpoint)
    .get('/queues/WEATHER/components')
    .reply(200, [{ queue: { key: 'WEATHER' } }])
    .get('/queues/WEATHER/versions')
    .reply(200, [{ queue: { key: 'WEATHER' } }])
    .get('/queues/WEATHER/projects')
    .reply(200, [{ queue: { key: 'WEATHER' } }]);

test('queues', function(t) {
    t.plan(16);

    client.queues()
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queues');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createQueue({ key: 'WEATHER' })
        .catch(function(err) {
            t.equal(err.statusCode, 409, 'should get `409 Conflict` on existing queue');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.createQueue({ key: 'WEATHERAPP' })
        .then(function(data) {
            t.ok(typeof data === 'object', 'should return new queue');
            t.equal(data.key, 'WEATHERAPP', 'should match queue key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getQueue('WEATHER')
        .then(function(data) {
            t.ok(typeof data === 'object', 'should get queue');
            t.equal(data.key, 'WEATHER', 'should match queue key');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.updateQueue('WEATHER', { name: 'Weather' })
        .then(function(data) {
            t.equal(data.name, 'Weather', 'should match changed field');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.deleteQueue('WEATHER')
        .then(function() {
            t.pass('should be deleted');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getQueue('FOOBAR')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing queue');
        });

    client.updateQueue('FOOBAR')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing queue');
        });

    client.deleteQueue('FOOBAR')
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing queue');
        });

    client.queueComponents('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue components');
            t.ok(data.every(function(item) {
                return item.queue.key === 'WEATHER';
            }), 'should match queue key of every component');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.queueVersions('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue versions');
            t.ok(data.every(function(item) {
                return item.queue.key === 'WEATHER';
            }), 'should match queue key of every version');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.queueProjects('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue projects');
        })
        .catch(function(err) {
            t.fail(err.message);
        });
});

test('queues (deprecated methods)', function(t) {
    t.plan(5);

    client.getQueueComponents('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue components');
            t.ok(data.every(function(item) {
                return item.queue.key === 'WEATHER';
            }), 'should match queue key of every component');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getQueueVersions('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue versions');
            t.ok(data.every(function(item) {
                return item.queue.key === 'WEATHER';
            }), 'should match queue key of every version');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getQueueProjects('WEATHER')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of queue projects');
        })
        .catch(function(err) {
            t.fail(err.message);
        });
});
