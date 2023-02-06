/* eslint func-names: "off" */

var nock = require('nock');
var test = require('tape');
var client = require('./client');
var config = require('./config');

nock(config.endpoint)
    .get('/issues/WEATHER-3300/changelog')
    .reply(200, [{ id: 'first_change_id' }, { id: 'second_change_id' }]);

test('issue changelog', function(t) {
    t.plan(1);

    client.issueChangelog('WEATHER-3300')
        .then(function(data) {
            t.ok(Array.isArray(data), 'should get list of changelog');
        })
        .catch(function(err) {
            t.fail(err.message);
        });
});
