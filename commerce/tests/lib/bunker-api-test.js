const config = require('yandex-cfg');
const { expect } = require('chai');
const mockery = require('mockery');
const nock = require('nock');
const sinon = require('sinon');

const catchError = require('../helper/catch-error');
const mockCache = require('../helper/cache');

const { api, project } = config.bunker;
const { baseUrl, paths } = config.bunkerApi;

let BunkerApi = require('../../server/lib/bunker-api');

describe('Bunker API Lib', () => {
    const cookie = 'test-cookie';
    const csrfToken = 'test-csrf-token';

    afterEach(nock.cleanAll);

    describe('get', () => {
        it('should correctly get node', done => {
            const node = 'product-cta-48';
            const mime = 'application/json; charset=utf-8; schema="bunker:/adv-www/.schema/product-price#"';
            const body = { pattern: node };

            nock(api)
                .get(paths.cat)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(200, body, {
                    'Content-Type': mime
                });

            const bunkerApi = new BunkerApi();

            bunkerApi.get(node)
                .then(actual => {
                    expect(actual).to.deep.equal({ body, mime });
                    done();
                })
                .catch(err => done(err));
        });

        it('should get node of specified version', done => {
            const node = 'product-cta-48';
            const version = '1';
            const mime = 'application/json; charset=utf-8; schema="bunker:/adv-www/.schema/product-price#"';
            const body = { pattern: node };

            nock(api)
                .get(paths.cat)
                .query({ node, version })
                .times(Infinity)
                .reply(200, body, {
                    'Content-Type': mime
                });

            const bunkerApi = new BunkerApi();

            bunkerApi.get(node, { version })
                .then(actual => {
                    expect(actual).to.deep.equal({ body, mime });
                    done();
                })
                .catch(err => done(err));
        });

        it('should log `Нода не найдена` when API respond 404', done => {
            const node = 'product-cta-48';

            nock(api)
                .get(paths.cat)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(404);

            sinon.spy(console, 'log');

            const bunkerApi = new BunkerApi();

            bunkerApi.get(node)
                .then(actual => {
                    expect(actual).to.be.undefined;
                    expect(console.log.withArgs('Нода не найдена', node).calledOnce).to.be.true;
                    done();
                })
                .catch(err => done(err))
                .finally(console.log.restore);
        });

        it('should log `Нода удалена` when API respond 410', done => {
            const node = 'product-cta-48';

            nock(api)
                .get(paths.cat)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(410);

            sinon.spy(console, 'log');

            const bunkerApi = new BunkerApi();

            bunkerApi.get(node)
                .then(actual => {
                    expect(actual).to.be.undefined;
                    expect(console.log.withArgs('Нода удалена', node).calledOnce).to.be.true;
                    done();
                })
                .catch(err => done(err))
                .finally(console.log.restore);
        });

        it('should throw error when bunker is not available', done => {
            const node = 'product-cta-48';

            nock(api)
                .get(paths.cat)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi();

            bunkerApi.get('product-cta-48')
                .then(() => done('should throw error'))
                .catch(() => done());
        });
    });

    describe('list', () => {
        it('should correctly get child nodes', done => {
            const node = 'product-cta-48';
            const expected = [
                { number: 1 },
                { number: 2 }
            ];

            nock(api)
                .get(paths.ls)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(200, expected);

            const bunkerApi = new BunkerApi();

            bunkerApi.list(node)
                .then(actual => {
                    expect(actual).to.deep.equal(expected);
                    done();
                })
                .catch(err => done(err));
        });

        it('should get nodes of specified version', done => {
            const node = 'product-cta-48';
            const version = 'stable';
            const expected = [
                { number: 1 },
                { number: 2 }
            ];

            nock(api)
                .get(paths.ls)
                .query({ node, version })
                .times(Infinity)
                .reply(200, expected);

            const bunkerApi = new BunkerApi();

            bunkerApi.list(node, { version })
                .then(actual => {
                    expect(actual).to.deep.equal(expected);
                    done();
                })
                .catch(err => done(err));
        });

        it('should throw error when bunker is not available', done => {
            const node = 'product-cta-48';

            nock(api)
                .get(paths.ls)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi();

            bunkerApi.list('product-cta-48')
                .then(() => done('should throw error'))
                .catch(() => done());
        });
    });

    describe('list recursive', () => {
        it('should correctly get nodes tree', done => {
            const node = 'product-cta-48';
            const expected = [
                { number: 1 },
                { number: 2 }
            ];

            nock(api)
                .get(paths.tree)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(200, expected);

            const bunkerApi = new BunkerApi();

            bunkerApi.list(node, { recursive: true })
                .then(actual => {
                    expect(actual).to.deep.equal(expected);
                    done();
                })
                .catch(err => done(err));
        });

        it('should get specified version of tree', done => {
            const node = 'product-cta-48';
            const version = 'stable';
            const expected = [
                { number: 1 },
                { number: 2 }
            ];

            nock(api)
                .get(paths.tree)
                .query({ node, version })
                .times(Infinity)
                .reply(200, expected);

            const bunkerApi = new BunkerApi();

            bunkerApi.list(node, { version, recursive: true })
                .then(actual => {
                    expect(actual).to.deep.equal(expected);
                    done();
                })
                .catch(err => done(err));
        });

        it('should throw error when bunker is not available', done => {
            const node = 'product-cta-48';

            nock(api)
                .get(paths.tree)
                .query({ node, version: 'latest' })
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi();

            bunkerApi.list('product-cta-48', { recursive: true })
                .then(() => done('should throw error'))
                .catch(() => done());
        });
    });

    describe('store', () => {
        const node = 'product-features';
        const data = { test: 'data' };

        afterEach(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        it('should correctly store node to bunker', done => {
            const bunkerResponse = { 0: { node, version: 2 } };

            mockCache({
                has: () => true,
                get: () => csrfToken
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.store)
                .times(Infinity)
                .reply(200, bunkerResponse);

            const bunkerApi = new BunkerApi({ cookie, csrfToken });

            bunkerApi.store(node, data)
                .then(actual => {
                    expect(actual).to.deep.equal(bunkerResponse);
                    done();
                })
                .catch(err => done(err));
        });

        it('should respond CSRF-Token when not specified', done => {
            const bunkerResponse = { 0: { node, version: 2 } };

            mockCache({
                set: () => ({}),
                del: () => ({})
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .get(`/${project}`)
                .times(Infinity)
                .reply(200, `csrf-token&quot;:&quot;${csrfToken}&quot;`);

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.store)
                .times(Infinity)
                .reply(200, bunkerResponse);

            const bunkerApi = new BunkerApi({ cookie });

            bunkerApi.store(node, data)
                .then(actual => {
                    expect(actual).to.deep.equal(bunkerResponse);
                    done();
                })
                .catch(err => done(err));
        });

        it('should throw error when Cookie is not specified', () => {
            const bunkerApi = new BunkerApi();

            const error = catchError(() => bunkerApi.store(node, data));

            expect(error.message).to.equal('Для записи ноды требуются Cookie');
        });

        it('should throw error when can not receive CSRF-Token', done => {
            mockCache({
                has: () => false,
                get: () => ({}),
                set: () => ({}),
                del: () => ({})
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .get(`/${project}`)
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi({ cookie });

            bunkerApi.store(node, data)
                .then(() => done('should throw error'))
                .catch(error => {
                    expect(error.statusCode).to.equal(500);
                    done();
                });
        });

        it('should throw error when bunker is not available', done => {
            const body = { test: 'data' };

            mockCache({
                has: () => true,
                get: () => csrfToken
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.store)
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi({ cookie });

            bunkerApi.store(node, body)
                .then(() => done('should throw error'))
                .catch(() => done());
        });
    });

    describe('publish', () => {
        const node = 'test-node';

        afterEach(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        it('should correctly publish bunker node', done => {
            const bunkerResponse = { 0: { node, version: 2 } };

            mockCache({
                has: () => true,
                get: () => csrfToken
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.publish)
                .times(Infinity)
                .reply(200, bunkerResponse);

            const bunkerApi = new BunkerApi({ cookie, csrfToken });

            bunkerApi.publish(node)
                .then(actual => {
                    expect(actual).to.deep.equal(bunkerResponse);
                    done();
                })
                .catch(err => done(err));
        });

        it('should respond CSRF-Token when not specified', done => {
            const bunkerResponse = { 0: { node, version: 2 } };

            mockCache({
                set: () => ({}),
                del: () => ({})
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .get(`/${project}`)
                .times(Infinity)
                .reply(200, `csrf-token&quot;:&quot;${csrfToken}&quot;`);

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.publish)
                .times(Infinity)
                .reply(200, bunkerResponse);

            const bunkerApi = new BunkerApi({ cookie });

            bunkerApi.publish(node)
                .then(actual => {
                    expect(actual).to.deep.equal(bunkerResponse);
                    done();
                })
                .catch(err => done(err));
        });

        it('should publish node by version', done => {
            const version = '42';
            const bunkerResponse = { 0: { node, version } };

            mockCache({
                has: () => true,
                get: () => csrfToken
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.publish)
                .times(Infinity)
                .reply(200, bunkerResponse);

            const bunkerApi = new BunkerApi({ cookie, csrfToken });

            bunkerApi.publish(node, version)
                .then(actual => {
                    expect(actual).to.deep.equal(bunkerResponse);
                    done();
                })
                .catch(err => done(err));
        });

        it('should throw error when Cookie is not specified', () => {
            const bunkerApi = new BunkerApi({ csrfToken });

            const error = catchError(() => bunkerApi.publish(node));

            expect(error.message).to.equal('Для публикации ноды требуются Cookie');
        });

        it('should throw error when can not receive CSRF-Token', done => {
            mockCache({
                has: () => false,
                get: () => ({}),
                set: () => ({}),
                del: () => ({})
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .get(`/${project}`)
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi({ cookie });

            bunkerApi.publish(node)
                .then(() => done('should throw error'))
                .catch(error => {
                    expect(error.statusCode).to.equal(500);
                    done();
                });
        });

        it('should throw error when bunker is not available', done => {
            mockCache({
                has: () => true,
                get: () => csrfToken
            });

            BunkerApi = require('../../server/lib/bunker-api');

            nock(baseUrl)
                .defaultReplyHeaders({
                    Cookie: cookie,
                    'X-CSRF-Token': csrfToken
                })
                .post(paths.publish)
                .times(Infinity)
                .reply(500);

            const bunkerApi = new BunkerApi({ cookie, csrfToken });

            bunkerApi.publish(node)
                .then(() => done('should throw error'))
                .catch(() => done());
        });
    });
});
