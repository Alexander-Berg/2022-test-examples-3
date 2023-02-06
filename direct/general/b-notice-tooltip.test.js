describe('b-notice-tooltip', function() {
    var sandbox,
        salt = function(key) {
            return key + '~' + u.consts('uid') + ':' + u.consts('ulogin');
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });

        sandbox.stub(u, 'consts')
            .withArgs('uid').returns(12345)
            .withArgs('ulogin').returns('user-login');
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('.utils.js', function() {

        it('setCookie выставляет куки', function() {
            // prepare
            var spy = sandbox.spy($, 'cookie');

            // action
            u['b-notice-tooltip'].setCookie('key');

            // check
            expect(spy.calledOnce).to.be.true;
            expect(spy.getCall(0).args[0]).to.be.equal(salt('key'));
            expect(spy.getCall(0).args[1]).to.be.equal(1);
        });

        it('hasCookie возвращает установленное значение', function() {
            // prepare
            var spy = sandbox.stub($, 'cookie')
                .withArgs(salt('test-key')).returns(1);

            // action
            var cookie = u['b-notice-tooltip'].hasCookie('test-key');

            // check
            expect(cookie).to.be.true;
        });

        it('hasCookie без аргументов возвращает false', function() {
            // action
            var cookie = u['b-notice-tooltip'].hasCookie();

            // check
            expect(cookie).to.be.false;
        });

        describe('поддержка куки при серверной шаблонизации', function() {
            var storeData;

            beforeEach(function() {
                window.data && (storeData = window.data);

                window.data = { COOKIES: {} };
                window.data.COOKIES[salt('known')] = 1;
            });

            afterEach(function() {
                window.data = storeData;
            });

            it('hasCookie возвращает false при незаданной куке', function() {
                expect(u['b-notice-tooltip'].hasCookie('unknown')).to.be.false;
            });

            it('hasCookie возвращает true при заданной куке', function() {
                expect(u['b-notice-tooltip'].hasCookie('known')).to.be.true;
            });

        });
    });

    describe('Блокирующая показ кука выставлена', function() {
        var key;

        beforeEach(function() {
            key = u._.uniqueId('informed_this_notice_cookie_key');

            sandbox.stub(u['b-notice-tooltip'], 'hasCookie')
                .withArgs(key).returns(true);
        });

        describe('при шаблонизации пустой результат для блока', function() {

            it('с указанной блокированной кукой', function() {
                expect(BEMHTML.apply({
                    block: 'b-notice-tooltip',
                    key: key,
                    content: 'content text'
                })).to.be.equal('');
            });

        });

        describe('при шаблонизации не пустой результат для блока', function() {

            it('с указанной кукой, отличной от блокированной', function() {
                expect(BEMHTML.apply({
                    block: 'b-notice-tooltip',
                    key: u._.uniqueId('informed_this_notice_cookie_key'),
                    content: 'content text'
                })).to.not.be.equal('');
            });

            it('без указания куки', function() {
                expect(BEMHTML.apply({
                    block: 'b-notice-tooltip',
                    content: 'content text'
                })).to.not.be.equal('');
            });

        });

    });

    describe('Блокирующая показ кука не выставлен', function() {
        var block;

        afterEach(function() {
            block && block.destruct() && (block = null);
        });

        it('при шаблонизации не пустой результат', function() {
            expect(BEMHTML.apply({
                block: 'b-notice-tooltip',
                key: u._.uniqueId('informed_this_notice_cookie_key'),
                content: 'content text'
            })).to.not.be.equal('');
        });

        it('в параметрах блока передан ключ для куки', function() {
            var key = u._.uniqueId('informed_this_notice_cookie_key');

            block = u.createBlock({
                block: 'b-notice-tooltip',
                key: key,
                content: 'content text'
            });

            expect(block.params.key).to.be.equal(key);
        });

        describe('при клике на крестик', function() {

            var createBlock = function(key) {
                return u.createBlock({
                    block: 'b-notice-tooltip',
                    key: key,
                    content: 'content text'
                }, { inject: true });
            }

            it('выставлятся блокирующая кука', function() {
                // prepare
                var spy = sandbox.spy(u['b-notice-tooltip'], 'setCookie');
                var TEST_KEY = 'test-key';
                block = createBlock(TEST_KEY);

                // action
                block.elem('close-link').click();
                sandbox.clock.tick(1);

                // check
                expect(spy.calledWith(TEST_KEY)).to.be.true;
            });

            it('выставлятся _hidden_yes', function() {
                // prepare
                block = createBlock();

                // action
                block.elem('close-link').click();
                sandbox.clock.tick(1);

                expect(block).to.haveMod('hidden', 'yes');
            });

        });

    });


});
