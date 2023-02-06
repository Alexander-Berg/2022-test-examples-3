const nock = require('nock');

const testpalmApi = 'http://testpalm-api';
const seleniumApi = 'http://sg.yandex-team.ru:4444';

function mockTestRun(testRun) {
    return nock(testpalmApi)
        .matchHeader('Authorization', 'OAuth oauth-token')
        .get('/api/testrun/project-id/run-id')
        .reply(200, testRun);
}

function mockTestStatus(uuid, status) {
    return nock(testpalmApi)
        .matchHeader('Authorization', 'OAuth oauth-token')
        .post(`/api/testrun/project-id/run-id/${uuid}/${status}`)
        .reply(200, {});
}

function mockTestComment(uuid, comment) {
    return nock(testpalmApi)
        .matchHeader('Authorization', 'OAuth oauth-token')
        .post(`/api/runtestcase/project-id/run-id/${uuid}/comments`, {
            text: comment,
        })
        .reply(200, {});
}

function mockTestAttachment(id) {
    return nock(testpalmApi)
        .matchHeader('Authorization', 'OAuth oauth-token')
        .matchHeader('Content-Length', '659')
        .post(`/api/testcases/project-id/${id}/attachment`)
        .reply(200, {
            url: '/static/screenshot-url',
        });
}

function mockSessionCreate(browser, id = 1) {
    return nock(seleniumApi, { encodedQueryParams: true })
        .post('/wd/hub/session', body => {
            return body.desiredCapabilities.browserName === browser;
        })
        .reply(200, {
            value: {
                sessionId: `session-id-${browser}-${id}`,
                capabilities: {
                    browserName: browser,
                    browserVersion: '123.0.0',
                    platformName: 'linux',
                },
            },
        });
}

function mockSessionDelete(browser, id = 1) {
    return nock(seleniumApi, { encodedQueryParams: true })
        .delete(`/wd/hub/session/session-id-${browser}-${id}`)
        .reply(200, {
            value: {},
        });
}

function mockSessionScreenshot(browser, id = 1) {
    return nock(seleniumApi)
        .get(`/wd/hub/session/session-id-${browser}-${id}/screenshot`)
        .reply(200, {
            value: 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
        });
}

module.exports = {
    testpalmApi,
    seleniumApi,

    mockTestRun,
    mockTestStatus,
    mockTestComment,
    mockTestAttachment,

    mockSessionCreate,
    mockSessionDelete,
    mockSessionScreenshot,
};
