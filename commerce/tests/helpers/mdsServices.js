const config = require('yandex-config');
const nock = require('nock');
const ReadableStream = require('stream').Readable;

const key = '603/1468925144742_555555.pdf';
const xmlSuccessResponse = `<?xml version="1.0" encoding="utf-8"?>
        <post obj="expert.cert114" id="0:20a07f97f350...aa18bd3cf1e0" groups="2" size="741925" key="${key}">
        </post>`;

const { avatarsService, mdsService } = config;
const avatarsHost = `${avatarsService.write.protocol}//${avatarsService.write.hostname}:` +
    `${avatarsService.write.port}/`;
const mdsWriteHost = `${mdsService.write.protocol}://${mdsService.write.hostname}:${mdsService.write.port}/`;
const mdsReadHost = `http://${mdsService.read.hostname}/get-${mdsService.namespace}/`;
const imagePath = `/get-${avatarsService.namespace}/603/1468925144742_555555/orig`;
const avatarsResponse = path => ({
    sizes: {
        orig: {
            height: 640,
            path,
            width: 1024
        }
    }
});

module.exports.avatars = {
    success() {
        return nock(avatarsHost)
            .post(/\/\d+_\d+$/)
            .times(Infinity)
            .reply(200, avatarsResponse(imagePath));
    },

    retry(data) {
        const reg = new RegExp(`${data.certId}$`);

        return nock(avatarsHost)
            .post(reg)
            .once()
            .reply(data.code, {})
            .post(reg)
            .reply(200, avatarsResponse(imagePath));
    },

    failedId() {
        return nock(avatarsHost)
            .post(/failedId$/)
            .times(Infinity)
            .reply(401, {});
    }
};

module.exports.mds = {
    success() {
        return nock(mdsWriteHost)
            .post(/\/\d+_\d+\.\w+$/)
            .times(Infinity)
            .reply(200, xmlSuccessResponse, { 'content-type': 'application/xml' });
    },

    failedId() {
        return nock(mdsWriteHost)
            .post(/failedId$/)
            .times(Infinity)
            .reply(401, '', { 'content-type': 'application/xml' });
    },

    retry(data) {
        const reg = new RegExp(`${data.certId}$`);

        return nock(mdsWriteHost)
            .post(reg)
            .once()
            .reply(data.code, '', { 'content-type': 'application/xml' })
            .post(reg)
            .reply(200, xmlSuccessResponse, { 'content-type': 'application/xml' });
    },

    pdf() {
        return nock(mdsReadHost)
            .get(/\/\d+_\d+\.pdf$/)
            .times(Infinity)
            .reply(200, () => {
                const s = new ReadableStream();

                s.push('pdf content');
                s.push(null);

                return s;
            });
    }
};
