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
    })

    .get('/projects/export/json/')
    .query({
        'project-id': 'project_err',
        'keyset-id': 'keyset',
        'branch-id': 'master',
        'flat-keyset': 1,
        'all-forms': 1,
    })
    .reply(404, '<error><![CDATA[Unknown project project_err]]></error>');

test('setup', t => {
    fsmock({
        './block': {},
    });
    t.end();
});

test('one project, bem', t => {
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
        noBemify: false,
    };

    stdmock.use();

    tanker(params, () => {
        stdmock.restore();

        var files1 = fs.readdirSync('./block/__key1/block__key1.i18n');

        t.equal(files1.length, 2, 'should create 2 files for key1');
        t.notEqual(files1.indexOf('en.js'), -1, 'should contain en.js');
        t.notEqual(files1.indexOf('ru.js'), -1, 'should contain ru.js');

        var files2 = fs.readdirSync('./block/__key2/block__key2.i18n');

        t.equal(files2.length, 2, 'should create 2 files for key2');
        t.notEqual(files2.indexOf('en.js'), -1, 'should contain en.js');
        t.notEqual(files2.indexOf('ru.js'), -1, 'should contain ru.js');

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

test('two projects, bem', t => {
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
        noBemify: false,
    };

    stdmock.use();

    tanker(params, () => {
        stdmock.restore();

        var files1 = fs.readdirSync('./block/__key1/block__key1.i18n');

        t.equal(files1.length, 3, 'should create 2 files for key1');
        t.notEqual(files1.indexOf('en.js'), -1, 'should contain en.js');
        t.notEqual(files1.indexOf('ru.js'), -1, 'should contain ru.js');
        t.notEqual(files1.indexOf('ua.js'), -1, 'should contain ua.js');

        var files2 = fs.readdirSync('./block/__key2/block__key2.i18n');

        t.equal(files2.length, 3, 'should create 2 files for key2');
        t.notEqual(files2.indexOf('en.js'), -1, 'should contain en.js');
        t.notEqual(files2.indexOf('ru.js'), -1, 'should contain ru.js');
        t.notEqual(files2.indexOf('ua.js'), -1, 'should contain ua.js');

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

test('invalid project', t => {
    var params = {
        host: config.host,
        port: config.port,
        endpoint: './block',
        projects: [
            {
                project: 'project_err',
                keysets: [
                    'keyset',
                ],
            },
        ],
        verbose: false,
        noBemify: false,
    };

    stdmock.use();

    tanker(params, err => {
        stdmock.restore();

        t.ok(err instanceof Error, 'should be an Error');

        var flash = stdmock.flush();

        t.equal(flash.stdout.length, 1, 'should contain 1 line in stdout');
        t.notEqual(flash.stdout[0].indexOf('project_err'), -1, 'should contain project');
        t.equal(flash.stderr.length, 2, 'should contain 2 lines in stderr');
        t.notEqual(flash.stderr[0].indexOf('project_err'), -1, 'should contain project');
        t.notEqual(flash.stderr[1].indexOf('Reason'), -1, 'should contain error reason');

        t.end();
    });
});
