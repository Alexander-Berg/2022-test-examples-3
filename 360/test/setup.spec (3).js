'use strict';

const glob = require('glob');

glob.sync('./helpers/stub-*.js', { cwd: __dirname, absolute: true }).forEach((file) => require(file));

glob.sync('**/*.spec.js', { ignore: [ 'node_modules/**/*', 'server-infra/**/*' ], absolute: true }).forEach(
    (file) => require(file)
);

const httpMock = require('node-mocks-http');
const Config = require('../server-configs');
const ExtraCore = require('../routes/helpers/core/extra-core.js');

const mailLib = require('@ps-int/mail-lib');
const configsHelpers = mailLib.serviceConfigs.helpers;

const createProjectConfig = configsHelpers['create-project-config'];
const createServiceOptionsGetters = configsHelpers['create-options-getters'];
const createServiceUrlsConfigFactory = configsHelpers['create-urls-config-factory'];

const localProjectConfigs = mailLib.serviceConfigs.base(require('../server-configs/services/base'));

const projectConfigs = createProjectConfig(localProjectConfigs);
const servicesOptionsGetters = createServiceOptionsGetters(projectConfigs);
const serviceUrlsConfigFactory = createServiceUrlsConfigFactory(projectConfigs.services);

const customModels = require('../models/index.js');
const customServices = require('../services/index.js');

const models = Object.assign({}, mailLib.models, customModels);
const services = Object.assign({}, mailLib.services, customServices);

beforeEach(function() {
    // nock debugging
    // process.env.DEBUG = 'nock.*';

    this.sinon = require('sinon').createSandbox();
    this.nock = require('nock');
    this.nock.disableNetConnect();

    // Хэлпер метод для стаба свойств объекта
    this.stubProperty = (obj, name, value) => {
        this.__stubbedProps__ = this.__stubbedProps__ || [];

        if (name in obj) {
            this.sinon.stub(obj, name).value(value);
        } else {
            obj[name] = undefined;
            this.sinon.stub(obj, name).value(value);
            this.__stubbedProps__.push(() => {
                delete obj[name];
            });
        }
    };

    return stubMl(this);
});

afterEach(function() {
    this.sinon.restore();
    this.nock.cleanAll();

    clearTestContext(this);
});

/**
 * Стабим
 */
function stubMl(sandbox) {
    const Core = ExtraCore;

    const request = httpMock.createRequest({
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/u2709/api/models',
            'x-real-ip': '2a02:6b8::25',
            'x-https-request': 'yes',
            'cookie': '123'
        },
        body: {
            _connection_id: '1',
            _ckey: 'Gb1ZeTCNDfadbGuBWOzOzQ=='
        }
    });

    request.uatraits = {
        BrowserEngine: 'Gecko',
        BrowserEngineVersion: '92.0',
        BrowserName: 'Firefox',
        BrowserVersion: '92.0',
        OSFamily: 'MacOS',
        OSName: 'macOS Catalina',
        OSVersion: '10.15',
        isBrowser: true,
        isMobile: false,
        isTouch: false
    };

    const response = httpMock.createResponse();

    sandbox.core = new Core(request, response);
    sandbox.coreConfigs = new Config(sandbox.core);

    sandbox.coreConfigs.getServiceOptionsGetter = function(serviceName) {
        return servicesOptionsGetters[serviceName] || servicesOptionsGetters.default;
    };
    sandbox.coreConfigs.getServicesUrls = function() {
        return serviceUrlsConfigFactory(this.core);
    };

    Object.assign(sandbox.core, {
        services: services,
        models: models,
        config: sandbox.coreConfigs
    });

    // sinon не умеет стабить методы, доступ к которым происходит через get/set
    /*
    Object.keys(Object.getPrototypeOf(sandbox.core.services)).forEach(function(key) {
        Object.defineProperty(sandbox.core.services, key, {
            value: sandbox.core.services[key],
            writable: true,
            configurable: true,
            enumerable: true
        });
    });
    */

    // эмулируем авторизованность
    sandbox.core.auth.set({
        mdb: 'mdb1',
        suid: '34',
        timezone: 'Europe/Moscow',
        tz_offset: -180,
        uid: '12'
    });
    sandbox.sinon.stub(sandbox.core.ckey, 'check');
}

/**
 * Очищает контекст после тестов
 */
function clearTestContext(context) {
    if (!context || typeof context !== 'object') {
        return;
    }

    if (context.__stubbedProps__) {
        context.__stubbedProps__.forEach((restoreCallback) => restoreCallback());
        delete context.__stubbedProps__;
    }

    for (const property in context) {
        if (context.hasOwnProperty(property)) {
            delete context[property];
        }
    }
}
