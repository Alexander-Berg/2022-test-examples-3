/* globals global:false Timings */

describe('profile', () => {
    let sandbox;
    let data;
    let oldTimings;
    let markStub;

    beforeEach(() => {
        sandbox = sinon.createSandbox();

        data = stubData('experiments');

        oldTimings = global.Timings;

        markStub = { publish: sinon.spy() };

        global.Timings = {
            start: sinon.spy(() => markStub),
            stop: sinon.spy(() => markStub),

            mark: sinon.spy(),
            add_meta: sinon.spy()
        };
    });

    afterEach(() => {
        sandbox.restore();

        global.Timings = oldTimings;
    });

    describeBlock('profile__get-base-meta', block => {
        let bemhtmlVersion;

        beforeEach(() => {
            bemhtmlVersion = BEMHTML._version;

            sandbox.stub(blocks, 'profile__get-project-name').returns('fake_project');
        });

        afterEach(() => {
            BEMHTML._version = bemhtmlVersion;
        });

        it('should return correct base info', () => {
            BEMHTML._version = 'bemhtml_go';

            let res = block(data);

            assert.propertyVal(res, 'project', 'fake_project');
            assert.propertyVal(res, 'page', 'search');
            assert.propertyVal(res, 'engine', 'bemhtml_go');

            assert.notProperty(res, 'ajax');
        });

        it("should add ajax flag when it's AJAX", () => {
            data.ajax = true;

            let res = block(data);

            assert.propertyVal(res, 'ajax', 1);
        });

        it("should add ajax flag when it's pre handler", () => {
            RequestCtx.GlobalContext.preHandler = true;

            let res = block(data);

            assert.propertyVal(res, 'project', 'fake_project_pre_handler');
        });
    });

    describeBlock('profile__add-meta', block => {
        it('should pass correct info to Timings.add_meta', () => {
            block(data, { foo: 'bar', baz: 'quux' });

            assert.calledWith(Timings.add_meta, 'foo=bar', 'baz=quux');
        });
    });

    describeBlock('profile__add-entry-point-meta', block => {
        stubBlocks('profile__get-base-meta', 'profile__add-meta');

        beforeEach(() => {
            data.entry = 'post-search';
            blocks['profile__get-base-meta'].returns({ foo: 'bar' });
        });

        it('should add base meta', () => {
            block(data);

            assert.callCount(blocks['profile__add-meta'], 1);

            let args = blocks['profile__add-meta'].args;

            assert.nestedPropertyVal(args, '0.1.foo', 'bar');
        });

        it('should mix additional meta', () => {
            block(data, { bar: 'baz' });

            let args = blocks['profile__add-meta'].args;

            assert.nestedPropertyVal(args, '0.1.bar', 'baz');
        });
    });

    describeBlock('profile__start', block => {
        it('should pass correct info to Timings.add_meta', () => {
            block(data, 'test_asserting');

            assert.calledWith(Timings.start, 'test_asserting');
            assert.calledWith(Timings.mark, 'before_test_asserting');
        });

        it('should add pre-search prefix', () => {
            data.entry = 'pre-search';

            block(data, 'test_asserting');

            assert.calledWith(Timings.start, 'test_asserting');
            assert.calledWith(Timings.mark, 'pre_search_before_test_asserting');
        });
    });

    describeBlock('profile__stop', block => {
        it('should pass correct info to Timings.add_meta', () => {
            block(data, 'test_asserting');

            assert.calledWith(Timings.stop, 'test_asserting');
            assert.calledWith(Timings.mark, 'after_test_asserting');
            assert.calledOnce(markStub.publish);
        });

        it('should add pre-search prefix', () => {
            data.entry = 'pre-search';

            block(data, 'test_asserting');

            assert.calledWith(Timings.stop, 'test_asserting');
            assert.calledWith(Timings.mark, 'pre_search_after_test_asserting');
            assert.calledOnce(markStub.publish);
        });
    });
});
