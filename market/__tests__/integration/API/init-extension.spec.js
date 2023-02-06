/* eslint-disable max-len */

const request = require('supertest');
const express = require('express');
const bodyParser = require('body-parser');
const cookieParser = require('cookie-parser');
const crypto = require('../../../utils/crypto');

const routes = require('../../../src/routes/init-extension');
const API = require('../../../src/API');

const UI = '{80444BD9-EB00-C8DA-AE97-37CD9BC50DB6}';
const VERSION = '3.1.12.66';
const DEFAULT_SETTINGS = {
    applicationName: 'Яндекс.Советник',
    affId: '1008',
    clid: 2210590,
    sovetnikExtension: true,
    withButton: true,
    extensionStorage: true,
};

const CLID_ADMIT_AD = 2320476;
const SETTINGS_WITH_ADMITAD_CLID = { ...DEFAULT_SETTINGS, clid: CLID_ADMIT_AD };

describe('/init-extension', () => {
    // TODO ↓↓↓ don't use this block in future
    const app = express();

    app.use(bodyParser.urlencoded({ limit: '3mb', extended: true }));
    app.use(bodyParser.json({ limit: '3mb' }));
    app.use(cookieParser('lJLJewojr3289434jlJfdsklnklsfjr42389434894923jsdfzzzj324'));
    app.use('/', routes);
    // TODO ↑↑↑ don't use this block in future

    const agent = request.agent(app);

    describe('GET /init-extension', () => {
        it('should return links and settings', (done) => {
            agent
                .get('/init-extension')
                .query({
                    settings: DEFAULT_SETTINGS,
                })
                .expect(200)
                .then(({ body }) => {
                    expect(body.searchUrl).toBeDefined();
                    expect(body.yandexUrl).toBeDefined();
                    expect(body.suggestUrl).toBeDefined();
                    expect(body.marketLogoUrl).toBeDefined();
                    expect(body.emptySearchUrl).toBeDefined();
                    expect(body.marketCategoryUrlTemplate).toBeDefined();
                    expect(body.settings).toBeDefined();

                    done();
                })
                .catch(done);
        });

        it('should not generate new client_id if it already exists', (done) => {
            const CLIENT_ID = '735268249-360c-4f4b-8f5f-d2485de8d35f';

            agent
                .get('/init-extension')
                .query({
                    settings: DEFAULT_SETTINGS,
                })
                .set('Cookie', [`svt-extension-client-id=${CLIENT_ID}`])
                .expect(200)
                .then(({ body }) => {
                    expect(body.settings).toBeDefined();
                    expect(body.settings.clientId).toBe(CLIENT_ID);

                    done();
                })
                .catch(done);
        });

        describe("with 'first-request'", () => {
            let spyDistribution;
            let spyMarketatorInstall;

            beforeEach(() => {
                spyDistribution = jest.spyOn(API.yandex, 'distribution');
                spyMarketatorInstall = jest.spyOn(API.partners.marketator, 'install');
            });

            afterEach(() => {
                spyDistribution.mockReset();
                spyDistribution.mockRestore();

                spyMarketatorInstall.mockReset();
                spyMarketatorInstall.mockRestore();
            });

            it.skip('should make request to soft-export with uuid', (done) => {
                const expectedParams = {
                    ip: '::ffff:127.0.0.1',
                    clid: undefined,
                    version: VERSION,
                    cookieHeader: '',
                    uuid: UI,
                    // fileNo: 62,
                    stat: 'install',
                    yasoft: 'sovetnik',
                };

                agent
                    .get('/init-extension')
                    .query({
                        settings: DEFAULT_SETTINGS,
                        'first-request': true,
                        ui: UI,
                        version: VERSION,
                    })
                    .set('Cookie', ['svt-partner=true'])
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(spyDistribution).toHaveBeenCalledWith(expectedParams);

                        done();
                    })
                    .catch(done);
            });

            it('should make request to soft-export with ui', (done) => {
                const expectedParams = {
                    ip: '::ffff:127.0.0.1',
                    clid: undefined,
                    version: VERSION,
                    cookieHeader: '',
                    ui: UI,
                    stat: 'install',
                    yasoft: 'sovetnik',
                };

                agent
                    .get('/init-extension')
                    .query({
                        settings: DEFAULT_SETTINGS,
                        'first-request': true,
                        ui: UI,
                        version: VERSION,
                    })
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(spyDistribution).toHaveBeenCalledWith(expectedParams);

                        done();
                    })
                    .catch(done);
            });

            it('should make request to partner callback without value', (done) => {
                const CLID = 2312228;
                const VID = 777;

                agent
                    .get('/init-extension')
                    .query({
                        settings: JSON.stringify({ ...DEFAULT_SETTINGS, clid: `${CLID}-${VID}` }),
                        'first-request': true,
                        ui: UI,
                        version: VERSION,
                        locale: 'RU',
                    })
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(spyMarketatorInstall).toHaveBeenCalledWith(CLID, VID, 'RU', undefined);

                        done();
                    })
                    .catch(done);
            });

            it('should make request to partner callback with value', (done) => {
                const CLID = 2312228;
                const VID = 777;

                const ppCookie = {
                    last_partner: {
                        clid: `${CLID}-${VID}`,
                        aff_id: '7',
                    },
                    install_id: '9bf15a27-2d6a-4f2b-b874-fb49d6162591',
                    install_id_update_time: 1583228152972,
                    hid: 'kKnl865HHkh',
                    ad: {
                        count: {
                            show: 1,
                            check: 0,
                            click: 1,
                        },
                        partner: {
                            clids: [`${CLID}-${VID}`],
                            aff_ids: ['7'],
                        },
                    },
                };

                agent
                    .get('/init-extension')
                    .query({
                        settings: JSON.stringify({ ...DEFAULT_SETTINGS, clid: `${CLID}-${VID}` }),
                        'first-request': true,
                        ui: UI,
                        version: VERSION,
                        locale: 'RU',
                    })
                    .set('Cookie', `svt-pp=${crypto.encrypt(JSON.stringify(ppCookie))}`)
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(spyMarketatorInstall).toHaveBeenCalledWith(CLID, VID, 'RU', ppCookie.hid);

                        done();
                    })
                    .catch(done);
            });

            it('should make request to soft-export with custom clid AdmitAd', (done) => {
                const expectedParams = {
                    ip: '::ffff:127.0.0.1',
                    clid: CLID_ADMIT_AD,
                    version: VERSION,
                    cookieHeader: '',
                    ui: UI,
                    stat: 'install',
                    yasoft: 'sovetnik',
                };

                agent
                    .get('/init-extension')
                    .query({
                        settings: JSON.stringify(SETTINGS_WITH_ADMITAD_CLID),
                        ui: UI,
                        version: VERSION,
                        'first-request': true,
                        custom_install_event: true,
                    })
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(API.yandex.distribution).toHaveBeenCalledWith(expectedParams);

                        done();
                    })
                    .catch(done);
            });

            it('should make request to soft-export with custom clid after an another extension has been installed first', (done) => {
                const expectedParams = {
                    ip: '::ffff:127.0.0.1',
                    clid: CLID_ADMIT_AD,
                    version: VERSION,
                    cookieHeader: '',
                    ui: UI,
                    stat: 'install',
                    yasoft: 'sovetnik',
                };

                // SaveFrom's cookies
                const cookiePp =
                    'svt-pp=6d3031a50e46e2ebb6cdaf2001990e907aac5c33d61749be6d219471f7b52abed3c639c6c3369b7ed88f4f61e950627541d518efe428647a21512e9f69fccf8bfba55536e9068926c14e653c7cb7b12d05018ba00724976a2f834224d6170ac9a9aed50a97d026c0afe562a42076874d43d487aea054ff3517752d7cf4b4b3fa1504c0e1de3bfac16aa7b24606e80a125518fd7528903757fc0b10284e5052918654aa2d20a93159cea794df9ad1a4a99710a1dc12c19fe8717c56da70ddfe8b16eb9f055f67f98f98cdd19a30f4c343';
                const cookiePartner =
                    'svt-partner=a0b7287b0ba3b9a50272e7ce3647f34075477c75baf2c6b9da16eebf03a8bb535609b3142686dd4c5d3e4fc54385494eb4062d63b4f531feaa16a854b7ae98cdedef3f57d96060aa56352cb4e4c9067026512cbc0d229be8ce7fca2affec2c41d2285007b900c869cabaecc10f48d31af6ee1f0482f1210ed2d48134d226aa41aec02143040d12cd181d59bb3e47aaf1063755fc375c84f2e306ab571baced1154794a9008f9e1ad611c3fdb0f1f3a5480c13e82ca1c60662cf916784f04d9564f14e0ba4fb49726ba24adde48157a0b';
                const cookieUser =
                    'svt-user=760a424591a443b5d4ee0253654218beb7553a026f5a1f94080d731f0c5b211cd1200c964a8883a30668ca43a1423ffe3460806704e1dcb70513cfccc9781d4b4d1a78e44eb3e0eb8dd314996e1c80bf97dd4b8f56923e6664bd91ec29f2159ca90acb64e0c773bf98a82627b4dfdf3dcda53cfbbbf12cd8cf997092f358cfb543576b8fd934905e03a048bfbc75d1281d6c10d2f78674c1e24eab07a98fb34b602fb965917976abebba0623d9a16fd617cb7bd255ac22a716eedc29b2673ee4ec0d43ca748dd5f17dd54602b39510f1098ff1ffeca93b4344adf0ca0c1b383da75af530872b4e57d0ab7af008d5b5604cda51e9f55ddfa09580be801b9d996b6077bdf48ba15add050b5258cd5fd272da4d001a4d256025ad7a81dc8d40d83c11b0e678952a5db0274da150ec2d6bc94b7f8456d454c5a07a5f2be9b346380ff7db97b476bb6ae6d40c0c4da1d114f18bcc06ecd9c26f00f00de565da1874fbbd95f601556d9900e83bf4595471315510d076dac1a477383c09ce8e15e2948e0b91d9604ee2e529fd08e1f5db415c9c5248eefa3d331db59bc856f0d0b907a7fefcbf278eef42eb00730483dc04f00f60f7151e28a771dc2953c014275cbe56f2e0f354ff6d67a645fd0668a040be500b60e70fbbd8a1f2f98a3f7d3753b18eb3fd1e3b205e17c9e64978c69eb5a412e7c12fabb48bce373554c0946749e431b82409ff13ec6cf9086007ca8cf87b72cdecc9c414d423c9bb10f0339420f4db3e63cbb738d7a7c65e5266e2bbead0ea77ebc5f59b81486c040bb85e8c3a6e007201590236b5ed8a87b489fe57ca3e3245b6e363cd957d0f4ae3913dd447a3a70a4029841ac26ccdda87b29cad0cc90961fb151dca0849b0c6c40fe172e98fea7a1802bc98d81e371096e3c5311cee614282d5f4e5100922019b5dd26a0f5f60bd8953c38eea55abeda36e77eeb0d99f9b5d1d80f8b0ac43dc34847cb759e258a36b6e8b1fa16460a311ee803ed8d02a1713351c832b6d20f69ff63aa2d6f7f47a0b9bd9121c599c3038db045d23b440142b29fe9bf52b25a73eccd88e5c40f909e8664e6a99783457b1552b155c2648ac9e0ede089cf32e2453b433b420df286848b8f9f6c9a57267510469209fd59909d468e11e17defafa0cad3a4706068099ad501417d28b65a0700bc79d1189cf57859647b479445135242df81ffa0ffe';

                agent
                    .get('/init-extension')
                    .query({
                        settings: JSON.stringify(SETTINGS_WITH_ADMITAD_CLID),
                        ui: UI,
                        version: VERSION,
                        custom_install_event: true,
                    })
                    .set('Cookie', [cookiePp, cookiePartner, cookieUser].join(';'))
                    .expect(200)
                    .then(({ body }) => {
                        expect(body).toBeDefined();
                        expect(API.yandex.distribution).toHaveBeenCalledWith(expectedParams);

                        done();
                    })
                    .catch(done);
            });
        });
    });
});
