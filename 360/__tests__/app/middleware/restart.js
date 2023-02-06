import express from 'express';
import request from 'supertest';
import restart from '../../../app/middleware/restart';

jest.mock('../../../app/render');
const popRenderCalls = require('../../../app/render').popCalls;

describe('middleware restart', function() {
    beforeEach(() => {
        this.app = express();

        this.app.use((req, res, next) => {
            req.token = {
                uid: 0
            };

            next();
        });

        this.agent = request(this.app);
    });

    it('should return 404 status if no token provided', (done) => {
        this.app.use((req, res, next) => {
            delete req.token;
            next();
        });
        restart(this.app);
        this.agent.get('/restart')
            .end(() => {
                const renderCalls = popRenderCalls();
                expect(renderCalls.length).toEqual(1);
                expect(renderCalls[0].code === 404);
                done();
            });
    });

    it('should proxy url and title from token ', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                title: 'yoyo',
                url: 'url',
                noiframe: 1
            };
            next();
        });
        restart(this.app);
        this.agent.get('/restart')
            .expect((res) => {
                expect(res.headers['x-accel-redirect']).toEqual('/?url=url&noiframe=1&name=yoyo');
            })
            .expect(204)
            .end(done);
    });

    it('should proxy date from token ', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                url: 'url',
                date: '1556109733'
            };
            next();
        });
        restart(this.app);
        this.agent.get('/restart')
            .expect((res) => {
                expect(res.headers['x-accel-redirect']).toEqual('/?url=url&date=1556109733&name=');
            })
            .expect(204)
            .end(done);
    });

    it('should proxy embed, noiframe and lang params from query', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                title: 'yoyo',
                url: 'url'
            };
            next();
        });
        restart(this.app);
        this.agent.get('/restart?noiframe=1&embed=embed&lang=tr')
            .expect((res) => {
                expect(res.headers['x-accel-redirect']).toEqual('/?url=url&name=yoyo&noiframe=1&embed=embed&lang=tr');
            })
            .expect(204)
            .end(done);
    });
});
