var fs = require('fs');
var nock = require('nock');
var test = require('tape');
var fsmock = require('mock-fs');
var stdmock = require('std-mocks');
var config = require('./config');
var tanker = require('../lib/tanker');

nock(config.endpoint)
    .get('/projects/export/json/')
    .query({
        'project-id': 'project_1',
        'keyset-id': 'keyset_11,keyset_12',
        'branch-id': 'master',
        'flat-keyset': 1,
        'all-forms': 1,
    })
    .reply(200, {
        ru: {
            key1: 'value1',
            key2: 'value2',
        },
        en: {
            key1: 'value1',
            key2: 'value2',
        },
    })

    .get('/projects/export/json/')
    .query({
        'project-id': 'project_1',
        'keyset-id': 'keyset_11',
        'branch-id': 'master',
        'flat-keyset': 1,
        'all-forms': 1,
    })
    .reply(200, {
        ru: {
            key1: 'value1',
            key2: 'value2',
        },
        en: {
            key1: 'value1',
            key2: 'value2',
        },
        ua: {
            key1: 'value1',
            key2: 'value2',
        },
    })

    .get('/projects/export/json/')
    .query({
        'project-id': 'project_2',
        'keyset-id': 'keyset_21',
        'branch-id': 'master',
        'flat-keyset': 1,
        'all-forms': 1,
    })
    .reply(200, {
        ru: {
            key1: 'value1',
            key2: 'value2',
        },
        en: {
            key1: 'value1',
            key2: 'value2',
        },
        ua: {
            key1: 'value1',
            key2: 'value2',
        },
    });

test('setup', t => {
    fsmock({
        './block': {},
    });
    t.end();
});

test('one project, no bem', t => {
    var params = {
        host: config.host,
        port: config.port,
        endpoint: './block',
        projects: [
            {
                project: 'project_1',
                keysets: [
                    'keyset_11',
                    'keyset_12',
                ],
            },
        ],
        verbose: false,
        noBemify: true,
    };

    stdmock.use();

    tanker(params, () => {
        stdmock.restore();

        t.equal(fs.readdirSync('./block').length, 1, 'should contain one .json file per project');
        t.ok(fs.existsSync('./block/project_1.json'), 'should create file with correct name');

        var flash = stdmock.flush();

        t.equal(flash.stdout.length, 2, 'should contain 2 lines in stdout');
        t.notEqual(flash.stdout[0].indexOf('keyset_11,keyset_12'), -1, 'should contain keysets');
        t.notEqual(flash.stdout[0].indexOf('project_1'), -1, 'should contain project');
        t.notEqual(flash.stdout[1].indexOf('./block'), -1, 'should contain endpoint');

        t.end();
    });
});

test('teardown', t => {
    fsmock.restore();
    t.end();
});

test('setup', t => {
    fsmock({
        './block': {},
    });

    t.end();
});

test('two projects, no bem', t => {
    var params = {
        host: config.host,
        port: config.port,
        endpoint: './block',
        projects: [
            {
                project: 'project_1',
                keysets: [
                    'keyset_11',
                ],
            },
            {
                project: 'project_2',
                keysets: [
                    'keyset_21',
                ],
            },
        ],
        verbose: false,
        noBemify: true,
    };

    stdmock.use();

    tanker(params, () => {
        stdmock.restore();

        t.equal(fs.readdirSync('./block').length, 2, 'should contain two .json files (one per project)');
        t.ok(fs.existsSync('./block/project_1.json'), 'should create file for first project');
        t.ok(fs.existsSync('./block/project_2.json'), 'should create file for second project');

        var flash = stdmock.flush();

        t.equal(flash.stdout.length, 4, 'should contain 4 lines in stdout');
        t.notEqual(flash.stdout[0].indexOf('keyset_11'), -1, 'should contain keysets');
        t.notEqual(flash.stdout[0].indexOf('project_1'), -1, 'should contain project');
        t.notEqual(flash.stdout[1].indexOf('./block'), -1, 'should contain endpoint');
        t.notEqual(flash.stdout[2].indexOf('keyset_21'), -1, 'should contain keysets');
        t.notEqual(flash.stdout[2].indexOf('project_2'), -1, 'should contain project');
        t.notEqual(flash.stdout[3].indexOf('./block'), -1, 'should contain endpoint');

        t.end();
    });
});

test('teardown', t => {
    fsmock.restore();
    t.end();
});
