const api = require('api');
const request = require('co-supertest').agent(api.callback());

const nock = require('nock');
const ReadableStream = require('stream').Readable;
const fs = require('fs');
const { expect } = require('chai');
const _ = require('lodash');

const { s3 } = require('yandex-config');

const nockTvm = require('tests/helpers/nockTvm');
const binaryParser = require('tests/helpers/binaryParser');
const dbHelper = require('tests/helpers/clear');

const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const mockPhoto = fs.readFileSync('tests/models/userIdentification/mock-photo');
const mockPhotoArrayBuffer = [].slice.call(mockPhoto, 0);

describe('Takeout file stream controller', () => {
    beforeEach(function *() {
        nockTvm.checkTicket({ src: 1234 });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    it('should stream file', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        nock(s3.endpoint)
            .get('/expert/testing/faces/123456789_5678930.jpg')
            .reply(200, () => {
                const stream = new ReadableStream();
                const chunks = _.chunk(mockPhotoArrayBuffer, 20);

                chunks.forEach(chunk => {
                    stream.push(Buffer.from(chunk));
                });
                stream.push(null);

                return stream;
            });

        yield request
            .get('/v1/user/takeout/faces/123456789_5678930.jpg')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .parse(binaryParser)
            .expect(200)
            .expect('Content-Disposition', 'attachment; filename="5678930.jpg"')
            .expect(({ body }) => {
                expect(Buffer.isBuffer(body)).to.be.true;
                expect(body).to.deep.equal(mockPhoto);
            })
            .end();
    });

    it('should return 400 when directory to file is invalid', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        yield request
            .get('/v1/user/takeout/photos/123456789_5678930.jpg')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .expect(400)
            .expect({
                message: 'Directory is invalid',
                internalCode: '400_DII',
                dir: 'photos'
            })
            .end();
    });

    it('should return 400 when file name is invalid', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        yield request
            .get('/v1/user/takeout/faces/123456789_5678930.png')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .expect(400)
            .expect({
                message: 'File name is invalid',
                internalCode: '400_FNI',
                name: '123456789_5678930.png'
            })
            .end();
    });

    it('should return 403 when client is not takeout', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'testTvmClient' });

        yield request
            .get('/v1/user/takeout/faces/123456789_5678930.jpg')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .expect(403)
            .expect({
                message: 'Client has no access',
                internalCode: '403_CNA',
                tvmClient: 'testTvmClient'
            })
            .end();
    });
});
