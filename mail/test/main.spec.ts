import * as chai from 'chai';
import 'mocha';
import {Router} from "../core/router";
import {MockServer} from "./mock-server";
import {PublicConfigurationsProvider} from "../configurations/registry";

chai.use(require('chai-http'));
const expect = chai.expect;

describe('X-Proxy acceptance tests before auto deploy', () => {
    let proxyPort = 9999;
    let localhost = `http://localhost:${proxyPort}`;
    let mockServer = new MockServer('{}');
    let mockServer2 = new MockServer('pizza');
    let mockServerPort = 3000;
    let mockServerPort2 = 3001;
    let router = new Router({
        defaultForwardHost: `http://localhost:${mockServerPort}`,
        pathConfigurations: {
            '/custom_prefix': {
                forwardHost: `http://localhost:${mockServerPort2}`,
                configurationName: 'random500',
                parameters: [
                    "-1"
                ]
            }
        }
    });

    before(() => {
        mockServer.start(mockServerPort);
        mockServer2.start(mockServerPort2);
        router.start(proxyPort);
    });

    it('should work without configuration', () => {
        return chai.request(localhost)
            .get('/api')
            .set('Authorization', 'olala')
            .set('Cookie', 'abyrvalg')
            .then(res => {
                expect(res.status).to.eql(200);
            });
    });

    let blacklisted = ['random500', 'rps', 'infinite', 'response500', 'image', 'periodicallyInfiniteResponse'];
    PublicConfigurationsProvider.allConfigurations().forEach(configurationName => {
        if (blacklisted.indexOf(configurationName) >= 0) {
            return;
        }
        it(`${configurationName}: configuration should work on mock backend`, () => {
            return chai.request(localhost)
                .get(`/c/${configurationName}`)
                .then(res => {
                    expect(res.status).to.eql(200);
                })
        });
    });

    it('pizza: should return 500 if no configuration with such name', () => {
        return chai.request(localhost)
            .get('/c/pizza')
            .then(res => {
                expect(res.status).to.eql(500);
            });
    });

    it('random500: should return 500 if 100% fail probability', () => {
        return chai.request(localhost)
            .get('/c/random500/100')
            .then(res => {
                expect(res.status).to.eql(500);
            });
    });

    it('random500: should return 200 if 0% fail probability', () => {
        return chai.request(localhost)
            .get('/c/random500/-1')
            .then(res => {
                expect(res.status).to.eql(200);
            });
    });

    it('random500: should return 500 if invalid fail probability', () => {
        return chai.request(localhost)
            .get('/c/random500/abyrvalg')
            .then(res => {
                expect(res.status).to.eql(500);
            });
    });

    it('readonly: should return 503 if post request', () => {
        return chai.request(localhost)
            .post('/c/readonly')
            .then(res => {
                expect(res.status).to.eql(503);
            });
    });

    it('rps: should return 429 if too much requests', () => {
        let path = '/c/rps/1';
        return chai.request(localhost).post(path).then(res1 => {
            expect(res1.status).to.eql(200);
            chai.request(localhost).post(path).then(res2 => {
                expect(res2.status).to.eql(429);
            });
        });
    });

    it('infinite: should not response', done => {
        setTimeout(() => done(), 1500);
        chai.request(localhost)
            .post('/c/infinite')
            .then(() => done(new Error("Received response")));
    });

    it('response500: should return 500 on selected handler', () => {
        return chai.request(localhost)
            .post('/c/response500/api%2Fmobile%2Fv1%2Fsettings/api/mobile/v1/settings')
            .then(res => {
                expect(res.status).to.eql(500);
            });
    });

    it('response500: should return 500 on other handler', () => {
        return chai.request(localhost)
            .post('/c/response500/api_mobile_v1_settings/api/mobile/v1/search')
            .then(res => {
                expect(res.status).to.eql(200);
            });
    });

    it('simple_mailbox: messages smoke', () => {
        return chai.request(localhost)
            .post('/c/simple_mailbox/api/mobile/v1/messages')
            .send({
                "requests": [
                    {
                        "fid": "1",
                        "first": 0,
                        "last": 20,
                        "md5": "",
                        "returnIfModified": true,
                        "threaded": true
                    }
                ]
            })
            .then(res => {
                expect(res.status).to.eql(200);
            });
    });

    it('should use custom configuration by custom prefix in config', () => {
       return chai.request(localhost)
           .post('/custom_prefix')
           .then(res => {
               expect(res.text).to.eql('pizza');
           })
    });

    it('should work with default configuration after configuration with custom prefix', () => {
        return chai.request(localhost)
            .post('/custom_prefix')
            .then(res => {
                expect(res.text).to.eql('pizza');
                return chai.request(localhost)
                    .post('/pizza')
                    .then(res => {
                        expect(res.text).to.eql('{}');
                    })
            })
    });

    after(() => {
        mockServer.stop();
        mockServer2.stop();
        router.stop();
    });
});
