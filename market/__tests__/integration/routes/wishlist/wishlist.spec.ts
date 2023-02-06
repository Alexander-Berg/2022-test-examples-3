import supertest from 'supertest';
import nock, { Scope } from 'nock';

import app from '../../../../src/app';
import raiseMocks from '../../helpers/raise-mocks';
import Mock from '../../../types/mock';

/* Mocks */
const wishlistAdd1Mock: Mock = require('./__mocks__/wishlist-add-1732210983');
const wishlistAdd2Mock: Mock = require('./__mocks__/wishlist-add-117391029');
const wishlistGet0Mock: Mock = require('./__mocks__/wishlist-get-0');
const wishlistGet1Mock: Mock = require('./__mocks__/wishlist-get-1');
const wishlistGet2Mock: Mock = require('./__mocks__/wishlist-get-2');
const wishlistRemove: Mock = require('./__mocks__/wishlist-remove-117391029');

/* Stubs */
const wishlistAdd1Stub = require('./__stubs__/wishlist-add-1732210983');
const wishlistAdd2Stub = require('./__stubs__/wishlist-add-117391029');
const wishlistGet0Stub = require('./__stubs__/wishlist-get-0');
const wishlistGet1Stub = require('./__stubs__/wishlist-get-1');
const wishlistGet2Stub = require('./__stubs__/wishlist-get-2');
const wishlistRemoveStub = require('./__stubs__/wishlist-remove-117391029');

type MockResponseType = { text: string; body: any };

const request = supertest(app);

let mockScopes: Array<Scope> = raiseMocks();

/**
 * @description returns parsed response body
 * @param {Object} response
 * @param {Object|Function} cb - `done` callback
 * @returns {Object}
 */
function getResponseBody(response: MockResponseType, cb: Function | { fail: Function }) {
    let body;
    try {
        body = JSON.parse(response.text);
    } catch (e) {
        (cb as { fail: Function }).fail(e);
    }
    return body;
}

describe.skip('Checking wishlist functionality', () => {
    test('got 0 items from empty wishlist', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(wishlistGet0Mock);

        request
            .get('/wishlist/get')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);
                expect(err).toBeNull();
                expect(body).toEqual(wishlistGet0Stub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });

    test('added model 1732210983', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(Object.assign(wishlistAdd1Mock, { method: 'post' }));

        request
            .get('/wishlist/add?model_id=1732210983')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);

                expect(err).toBeNull();
                expect(body).toEqual(wishlistAdd1Stub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });

    test('added model 117391029', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(Object.assign(wishlistAdd2Mock, { method: 'post' }));

        request
            .get('/wishlist/add?model_id=117391029')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);

                expect(err).toBeNull();
                expect(body).toEqual(wishlistAdd2Stub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });

    test('got 2 items from wishlist (1732210983 and 117391029)', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(wishlistGet2Mock);

        request
            .get('/wishlist/get')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);
                expect(err).toBeNull();
                expect(body).toEqual(wishlistGet2Stub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });

    test('remove 1 item from wishlist (117391029)', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(Object.assign(wishlistRemove, { method: 'delete' }));

        request
            .get('/wishlist/remove?model_id=117391029')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);
                expect(err).toBeNull();
                expect(body).toEqual(wishlistRemoveStub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });

    test('got 1 item from wishlist (1732210983)', (done: Function) => {
        const cookie = ['Session_id=100500'];
        mockScopes = raiseMocks(wishlistGet1Mock);
        request
            .get('/wishlist/get')
            .set('Cookie', cookie)
            .expect(200)
            .end((err: Error, response) => {
                const body = getResponseBody(response, done);
                expect(err).toBeNull();
                expect(body).toEqual(wishlistGet1Stub);

                mockScopes.forEach((scope) => scope.done());
                mockScopes.length = 0;
                nock.cleanAll();
                done();
            });
    });
});
