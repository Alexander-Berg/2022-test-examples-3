/* global describe, it, beforeEach */

let path = require('path');
let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');

describe('CoreTemplatedGate', function() {
    let app = require('../app');
    let CoreTemplatedGate = app.TemplatedGate;
    let CoreGate = app.Gate;
    let Resource = app.Resource;
    let privPath = path.resolve(__dirname, 'mocks/priv');

    it('should be an inheritor of CoreGate with mixed TemplateMixin', function() {
        assert.isTrue(CoreGate.isParentOf(CoreTemplatedGate));

        assert.isMixed(CoreTemplatedGate, app.TemplateMixin, 'CoreTemplateMixin',
            ['name', 'prototype', 'super_', 'create', '__super', '__objexOnMixing'],
            ['constructor']);
    });

    describe('method', function() {
        describe('.setEnv()', function() {
            let TestTemplatedGate;

            beforeEach(function() {
                TestTemplatedGate = CoreTemplatedGate.create('TestTemplatedGate');
            });

            it('should set __templateConfig property if opts.config passed', function() {
                let config = require('./fakes/config');
                let setConfig = sinon.spy(TestTemplatedGate, 'setConfig');

                TestTemplatedGate.setEnv({ config: config });

                assert.isTrue(setConfig.calledWith(config));

                setConfig.restore();
            });

            it('should set __resource property if opts.resource was passed', function() {
                let ResourceExecutor = Resource.create();
                let setResourceExecutor = sinon.spy(TestTemplatedGate, 'setResourceExecutor');

                TestTemplatedGate.setEnv({ resource: ResourceExecutor });

                assert.isTrue(setResourceExecutor.calledWith(ResourceExecutor));

                setResourceExecutor.restore();
            });

            it('should set _templatePriv property if opts.priv and opts.config were passed', function() {
                let setPriv = sinon.spy(TestTemplatedGate, 'setPriv');

                TestTemplatedGate.setEnv({
                    config: { debug: true },
                    priv: privPath,
                });

                assert.isTrue(setPriv.calledWith(privPath, true));

                setPriv.restore();
            });
        });
    });
});
