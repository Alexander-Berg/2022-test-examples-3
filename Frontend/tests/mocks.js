const config = require('yandex-cfg');
const nock = require('nock');
const _ = require('lodash');
const { fakeRenderPreview } = require('lib/sender');

function nockTvmtool() {
    nock(`${config.tvm.serverUrl}/`)
        .get('/tvm/tickets')
        .query(data => Boolean(data.dsts))
        .reply(200, {
            [config.blackbox.tvmDst]: { ticket: '123' },
        });
}

function nockBlackbox(login = 'solo', uid = 5) {
    nock(`http://${config.blackbox.api}/`)
        .get('/blackbox')
        .query(data => data.method === 'sessionid')
        .reply(200, { login, uid: { value: uid }, status: { id: 0 } });
}

function nockSpamCheckSuccess() {
    nock(config.so.host)
        .get(/.*/)
        .reply(200, '<spam>0</spam>');
}

function nockSpamCheckFail() {
    nock(config.so.host)
        .get(/.*/)
        .reply(200, '<spam>1</spam>');
}

function nockUploadAvatar(options = {}) {
    const { groupId = 1313, imageId = 12 } = options;

    nock(config.avatars.uploadHost)
        .post(/^\/put-.+/)
        .reply(200, {
            'group-id': groupId,
            imagename: imageId,
            sizes: {
                preview: {
                    height: 32,
                    width: 512,
                },
                orig: {
                    height: 640,
                    width: 1024,
                },
            },
        });
}

function nockUploadAvatarByUrl(options = {}) {
    const { imageUrl = 'https://solo.jpg', groupId = 1313, imageId = 12 } = options;

    return nock(config.avatars.uploadHost)
        .get(/^\/put-.+/)
        .query(data => data.url === imageUrl)
        .reply(200, {
            'group-id': groupId,
            imagename: imageId,
            sizes: {
                preview: {
                    height: 32,
                    width: 512,
                },
                orig: {
                    height: 640,
                    width: 1024,
                },
            },
        });
}

function nockDeleteAvatar() {
    return nock(config.avatars.uploadHost)
        .post(/^\/delete-[^/]*\/[0-9a-zA-Z-]+/)
        .reply(200);
}

function nockYoutubeApi(options = {}) {
    const { videoId = 'zB4I68XVPzQ', title = 'Star Wars: The Last Jedi Official Teaser' } = options;

    return nock(config.youtube.api)
        .get(/.*/)
        .query(data => data.id === videoId)
        .reply(200, {
            items: [{
                id: videoId,
                snippet: {
                    title,
                    thumbnails: {
                        high: {
                            url: 'https://i.ytimg.com/vi/zB4I68XVPzQ/hqdefault.jpg',
                            width: 480,
                            height: 360,
                        },
                    },
                },
                contentDetails: {
                    duration: 'PT1M32S',
                    definition: 'hd',
                },
            }],
        });
}

function nockSenderCreatePromoDistribution(options = {}) {
    const { accountSlug } = options;

    return nock(config.sender.host)
        .post(
            `/api/0/${accountSlug}/automation/promoletter`,
        )
        .reply(200, {
            result: {
                slug: 'promp-distrib-slug',
                id: '1ID2IN3SENDER',
            },
        });
}

function nockSenderSendEmail(options = {}) {
    const { to = 'luck@starwars.com', expectedVariables } = options;

    return nock(config.sender.host)
        .post(
            /\/api\/0\/.+\/transactional\/.+\/send/i,
            ({ args }) => expectedVariables ? _.isEqual(JSON.parse(args), expectedVariables) : true,
        )
        .query(data => data.to_email === to)
        .reply(200, {
            result: { status: 'OK' },
        });
}

function nockSenderRenderPreview({ title = '', text = '' } = {}) {
    return nock(config.sender.host)
        .post(/\/api\/0\/.+\/render\/campaign\/.+\/letter\/.+/i)
        .reply(200, (res, body) => {
            const params = JSON.parse(body.params);

            return {
                subject: fakeRenderPreview(title, params),
                result: fakeRenderPreview(text, params),
            };
        });
}

function nockFormsApiCopyForm(id) {
    return nock(config.forms.api)
        .post(`/surveys/${id}/copy/`)
        .reply(200, {
            errors: [],
            // eslint-disable-next-line camelcase
            survey_id: id + 1,
        });
}

function nockFormsApiPatchForm(id) {
    return nock(config.forms.api)
        .patch(`/surveys/${id}/`)
        .reply(200, {
            errors: [],
        });
}

function nockFormApiGetSettings(id) {
    return nock(config.forms.api)
        .get(`/surveys/${id}/`)
        .reply(200, {
            errors: [],
            // eslint-disable-next-line camelcase
            hashed_id: `${id}.5e5e5fdgjydgw767skasjd70sjlknlk8`,
            profile: {
                yandex_username: 'testauthorlogin',
            },
        });
}

function nockFormsApiRemove() {
    return nock(config.forms.api)
        .delete(/surveys\/\d+\/$/)
        .reply(204, '');
}

function nockLpcTemplateCopy(path, dbType) {
    return nock(config.lpc.host)
        .put(`/api/pages/${path}`)
        .query({
            src: config.lpc.turboTemplates[dbType],
            version: 2,
        })
        .reply(200, { data: {} });
}

function nockLpcGetNodes(path, exists = false) {
    return nock(config.lpc.host)
        .get(`/api/nodes/${path}`)
        .reply(exists ? 200 : 404);
}

function nockLpcSetPageData(path) {
    return nock(config.lpc.host)
        .put(`/api/page/${path}`)
        .reply(200, { data: { id: 1 } });
}

function nockLpcGetPageByPath(path) {
    const templateSections = {
        version: '1.60',
        children: [{
            type: 'Speakers',
            props: {
                speakers: [],
            },
        }, {
            type: 'EventsProgram',
            props: {
                items: [],
            },
        }],
    };

    return nock(config.lpc.host)
        .get(`/api/page/${path}`)
        .reply(200, {
            data: {
                id: 1,
                name: 'test-page',
                path: ['.sandbox/events'],
                latest: { data: templateSections, version: 2 },
            },
        });
}

function nockLpcNoPageByPath(path) {
    return nock(config.lpc.host)
        .get(`/api/page/${path}`)
        .reply(404);
}

function nockLpcCreateFolderByPath(path) {
    return nock(config.lpc.host)
        .put(`/api/folders/${path}`)
        .reply(200);
}

function nockLpcPublishing(path, withError) {
    const request = nock(config.lpc.host)
        .put(`/api/publish/${path}`)
        .query({
            version: 2,
            releaseVersion: '1.60',
        });

    if (!withError) {
        return request.reply(200);
    }

    return request.reply(400, { error: { code: 'errorVersionIsAlreadyPublished' } });
}

function nockLpcUnpublishing(path) {
    return nock(config.lpc.host)
        .post(`/api/unpublish/${path}`)
        .reply(200);
}

function nockTvmCheckTicket({ src = 1234, dst = 224 } = {}) {
    return nock(`${config.tvm.serverUrl}/`)
        .get('/tvm/checksrv')
        .query(data => {
            return Boolean(data);
        })
        .reply(200, { src, dst, scopes: null });
}

module.exports = {
    nockTvmtool,
    nockBlackbox,
    nockUploadAvatar,
    nockUploadAvatarByUrl,
    nockDeleteAvatar,
    nockYoutubeApi,
    nockSenderSendEmail,
    nockSenderCreatePromoDistribution,
    nockSenderRenderPreview,
    nockFormsApiCopyForm,
    nockFormsApiPatchForm,
    nockFormApiGetSettings,
    nockFormsApiRemove,
    nockLpcTemplateCopy,
    nockLpcGetNodes,
    nockLpcSetPageData,
    nockLpcGetPageByPath,
    nockLpcNoPageByPath,
    nockLpcCreateFolderByPath,
    nockLpcPublishing,
    nockLpcUnpublishing,
    nockTvmCheckTicket,
    nockSpamCheckSuccess,
    nockSpamCheckFail,
};
