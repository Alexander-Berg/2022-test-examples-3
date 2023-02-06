'use strict';

const config = require('yandex-cfg');
const nock = require('nock');
const { expect } = require('chai');

const CommentsModel = require('../../server/model/news/comments');

describe('Comments model', () => {
    const comments = new CommentsModel({});

    afterEach(() => {
        nock.cleanAll();
    });

    it('should break cmntIds into chunks', done => {
        const firstQuery = [1, 2, 3, 4, 5]
            .map(num => `entityId=${num}`)
            .join('&');

        const firstRequest = nock(new URL(config.cmnt.api).origin)
            .get(`/cmnt/v1/brief?${firstQuery}`)
            .times(1)
            .reply(200, {
                feed: {
                    1: { count: 10 },
                    2: { count: 20 },
                    3: { count: 30 },
                    4: { count: 40 },
                    5: { count: 50 }
                }
            });

        const secondRequest = nock(new URL(config.cmnt.api).origin)
            .get('/cmnt/v1/brief?entityId=6')
            .times(1)
            .reply(200, {
                feed: {
                    6: { count: 60 }
                }
            });

        comments
            .getCommentsCountByCmntIds(['1', '2', '3', '4', '5', '6'])
            .then(data => {
                expect(data).to.deep.equal({
                    1: 10,
                    2: 20,
                    3: 30,
                    4: 40,
                    5: 50,
                    6: 60
                });

                firstRequest.done();
                secondRequest.done();

                done();
            })
            .catch(err => {
                done(err);
            });
    });
});
