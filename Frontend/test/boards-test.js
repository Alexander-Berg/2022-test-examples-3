/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/boards/38158')
    .reply(200, { name: 'КП-ОТТ' })

    .get('/boards/38158/sprints')
    .reply(200, [{ id: '41092' }, { id: '35389' }])

    .get('/boards/404')
    .reply(404)

    .get('/boards/404/sprints')
    .reply(404);

test('boards', function(t) {
    t.plan(5);

    client.getBoard(38158)
        .then(function(data) {
            t.ok(typeof data === 'object', 'should get board');
            t.equal(data.name, 'КП-ОТТ', 'should match board name');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.getBoard(404)
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing board');
        });

    client.boardSprints(38158)
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of sprints');
        })
        .catch(function(err) {
            t.fail(err.message);
        });

    client.boardSprints(404)
        .then(function() {
            t.fail('should return `404 Not Found`');
        })
        .catch(function(err) {
            t.equal(err.statusCode, 404, 'should get `404 Not Found` on non existing board with sprint');
        });
});
