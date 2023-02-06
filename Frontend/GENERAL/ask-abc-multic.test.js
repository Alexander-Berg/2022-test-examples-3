const assert = require('assert');
const { format: urlFormat } = require('url');

const express = require('express');
const supertest = require('supertest');
const cookieParser = require('cookie-parser');
const fakeBack = require('../../../tools/fake-back');

describe('express/middlewares/ask-abc-multic', () => {
    const AskAbcMultic = require('./ask-abc-multic');

    it('Should send multic params to backend', done => {
        fakeBack(
            back => {
                const expect = [{
                    _type: '_type',
                }];

                back.get('/multic/', (req, res) => {
                    const query = req.query;

                    assert.strictEqual(query.format, 'json');
                    assert.strictEqual(query.q, 'sss');
                    assert.strictEqual(query.types, 'staff|role');
                    assert.strictEqual(query.staff_fields, 'default_service_role');
                    assert.strictEqual(query._service_id, 'serviceId');
                    assert.strictEqual(query.role__service, 'serviceId');

                    res.json(expect);
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.use(AskAbcMultic.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/?type=staff&type=role&text=sss&serviceId=serviceId')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should send not multic params and empty q to backend', done => {
        fakeBack(
            back => {
                back.get('/multic/', (req, res) => {
                    const query = req.query;

                    assert.ok(!query.staff_fields);
                    assert.ok(!query._service_id);
                    assert.ok(!query.role__service);
                    assert.strictEqual(query.format, 'json');
                    assert.strictEqual(query.q, ' ');
                    assert.strictEqual(query.types, 'notstaff|notrole');

                    res.json();
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.use(AskAbcMultic.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/?type=notstaff&type=notrole&serviceId=serviceId')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should correctly format staff data', done => {
        fakeBack(
            back => {
                const ans = [{
                    _text: 'text',
                    _type: 'staff',
                    login: 'login',
                    _id: 666,
                    isDismissed: true,
                    default_service_role: 'default_service_role',
                }];

                back.get('/multic/', (req, res) => {
                    const query = req.query;

                    assert.strictEqual(query.format, 'json');
                    assert.strictEqual(query.q, 'sss');
                    assert.strictEqual(query.types, 'staff');
                    assert.strictEqual(query.staff_fields, 'default_service_role');
                    assert.strictEqual(query._service_id, 'serviceId');

                    res.json(ans);
                });
            },
            (port, hostname, close) => {
                const app = express();

                const expect = [[
                    'staff',
                    'text',
                    {
                        _text: 'text',
                        _type: 'staff',
                        _id: 666,
                        isDismissed: true,
                        person: {
                            fullName: 'text',
                            login: 'login',
                            id: 666,
                            isDismissed: false,
                        },
                        role: 'default_service_role',
                        default_service_role: 'default_service_role',
                        login: 'login',
                        fromDepartment: null,
                    },
                ]];

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.use(AskAbcMultic.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    assert.deepEqual(res.locals.multic, expect);
                    res.sendStatus(555);
                });

                supertest(app)
                    .get('/?type=staff&text=sss&serviceId=serviceId')
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });

    it('Should support type related params', done => {
        fakeBack(
            back => {
                back.get('/multic/', ({ query }, res) => {
                    try {
                        expect(query.format).toBe('json');
                        expect(query.q).toBe('sss');
                        expect(query.types).toBe('staff|role|resource|resource_tag|resource_form_type|type_category');

                        expect(query._service_id).toBe('service-id');
                        expect(query.staff_fields).toBe('default_service_role');
                        expect(query.resource__type).toBe('resource-type');
                        expect(query.resource_tag__service).toBe('resource-tag-service');
                        expect(query.resource_tag__resource_type).toBe('resource-tag-resource-type');
                        expect(query.resource_form_type__category).toBe('resource-form-type-category');
                        expect(query.type_category__with_types).toBe('type-category-with-types');
                    } catch (err) {
                        console.error(err);
                    }

                    res.json([]);
                });
            },
            (port, hostname, close) => {
                const app = express();

                app.use(cookieParser());

                app.use((req, res, next) => {
                    req.logger = {
                        child() {
                            return {
                                info() {},
                                debug() {},
                            };
                        },
                    };
                    next();
                });

                app.use(AskAbcMultic.create({
                    port,
                    hostname,
                }));

                app.use((req, res) => {
                    expect(res.locals.multic).toEqual([]);
                    res.sendStatus(555);
                });

                supertest(app)
                    .get(urlFormat({
                        pathname: '/',
                        query: {
                            type: ['staff', 'role', 'resource', 'resource_tag', 'resource_form_type', 'type_category'],
                            text: 'sss',
                            serviceId: 'service-id',
                            resource__type: 'resource-type',
                            resource_tag__service: 'resource-tag-service',
                            resource_tag__resource_type: 'resource-tag-resource-type',
                            resource_form_type__category: 'resource-form-type-category',
                            type_category__with_types: 'type-category-with-types',
                        },
                    }))
                    .set('cookie', 'Session_id=Sessid')
                    .expect(555)
                    .end((...args) => {
                        close();
                        done(...args);
                    });
            },
        );
    });
});
