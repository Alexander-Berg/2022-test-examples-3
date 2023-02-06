describe('b-banner-href', function() {
    var block,
        bannerData  = {
            id: "1057460680",
            name: "m-banner",
            parentId: 801502979,
            parentName: "m-group"
        },
        ctx = {
            block: 'b-banner-href',
            name: 'domain',
            value: 'https://vk.com',
            canEditDomain: true,
            modelParams: bannerData,
            banner: {
                href: 'vk.com',
                url_protocol: 'https://'
            },
            urlLengthLimit: 1024,
            protocolReadOnly: false
        },
        bannerModel,
        vm,
        hrefControl,
        hrefInput,
        sandbox;

    beforeEach(function() {
        bannerModel = BEM.MODEL.create(bannerData, {
            href_model: {
                href: 'vk.com',
                url_protocol: 'https://',
                isParentBannerNew: false,
                domain: 'vk.com'
            }
        });
        block = u.createBlock(ctx);
        vm = block.model;
        hrefControl = block.findBlockInside('href', 'b-href-control');
        hrefInput =  hrefControl.findBlockOn('href', 'input');
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
    });

    afterEach(function() {
        bannerModel.destruct();
        block.destruct();
        sandbox.restore();
    });

    it('При изменении данных триггерится ajax-запрос ajaxCheckUrlMass', function() {
        hrefInput.val('nna.ru');
        sandbox.clock.tick(2100);

        expect(sandbox.server.requests[0].requestBody.indexOf('ajaxCheckUrlMass')).not.to.equal(-1);
    });

    describe('HTML->VM', function() {
        it('Если измененные данные валидны, они записываются в модель', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru',
                    protocol: 'http://',
                    url: 'http://stol.ru'
                }
            });
            hrefControl.trigger('state:changed', {
                isReady: true,
                validatedData: {
                    domain: 'stol.ru',
                    domain_sign: '12345',
                    domain_redir: '123',
                    domain_redir_sign: '123456',
                    market_rating: '1'
                }
            });

            expect(vm.get('href')).to.equal('stol.ru');
            expect(vm.get('url_protocol')).to.equal('http://');
            expect(vm.get('domain_sign')).to.equal('12345');
            expect(vm.get('domain_redir')).to.equal('123');
            expect(vm.get('domain_redir_sign')).to.equal('123456');
            expect(vm.get('market_rating')).to.equal(1);
        });

        it('Если измененные данные невалидны, модель остается неизменна', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru',
                    protocol: 'http://',
                    url: 'http://stol.ru',
                    errors: [
                        'Плохая ошибка'
                    ]
                }
            });
            hrefControl.trigger('state:changed', { isReady: false });

            expect(vm.get('href')).to.equal('vk.com');
            expect(vm.get('url_protocol')).to.equal('https://');
        });

        it('Если поля ввода очищены, в модель пишутся пустые строки, кроме протокола', function() {
            vm.set('protocol', 'https://');
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return undefined;
            });

            hrefControl.trigger('state:changed', { isReady: true });

            expect(vm.get('href')).to.equal('');
            expect(vm.get('domain')).to.equal('');
            expect(vm.get('url_protocol')).to.equal('https://');
        });
    });

    describe('Ошибки модели', function() {
        it('Если серверная проверка вернула ошибку, триггерится событие url-check-error с переданными ошибками', function() {

            sandbox.spy(vm, 'trigger');

            hrefControl.trigger('state:changed', {
                isReady: false,
                alerts: [
                    'Плохая ошибка'
                ] });
            sandbox.clock.tick(100);

            expect(vm.trigger.calledWith('url-check-error', { messages: ['Плохая ошибка'] })).to.equal(true);
        });

        it('Если в урле есть параметры, триггерится событие url-check-error с ошибкой hrefParams', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru/?param1={param1}',
                    protocol: 'http://',
                    url: 'http://stol.ru/?param1={param1}'
                }
            });
            sandbox.spy(vm, 'trigger');

            hrefControl.trigger('state:changed', {
                isReady: true,
                validatedData: {
                    domain: 'stol.ru',
                    domain_sign: '12345',
                    domain_redir: '123',
                    domain_redir_sign: '123456',
                    market_rating: '1'
               }
            });
            sandbox.clock.tick(100);

            var hasHrefParamsError = vm.trigger.args.some(function(arg) {
                return arg[1] && arg[1].messages && arg[1].messages.indexOf('hrefParams') != 1;
            });

            expect(hasHrefParamsError).to.equal(true);
        });

        it('Если в домен не урл, триггерится событие url-check-error с ошибкой hrefFormat', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru',
                    protocol: 'http://',
                    url: 'http://stol.ru'
                }
            });
            sandbox.spy(vm, 'trigger');

            hrefControl.trigger('state:changed', {
                isReady: true,
                validatedData: {
                    domain: 'stol',
                    domain_sign: '12345',
                    domain_redir: '123',
                    domain_redir_sign: '123456',
                    market_rating: '1'
               }
            });
            sandbox.clock.tick(100);

            var hasHrefFormatError = vm.trigger.args.some(function(arg) {
                return arg[1] && arg[1].messages && arg[1].messages.indexOf('hrefFormat') != 1;
            });

            expect(hasHrefFormatError).to.equal(true);
        });

        it('Если в домен слишком длинный, триггерится событие url-check-error с ошибкой hrefLength', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru',
                    protocol: 'http://',
                    url: 'http://stol.ru'
                }
            });
            sandbox.spy(vm, 'trigger');

            hrefControl.trigger('state:changed', {
                isReady: true,
                validatedData: {
                    domain: new Array(10).join('stol') + '.ru',
                    domain_sign: '12345',
                    domain_redir: '123',
                    domain_redir_sign: '123456',
                    market_rating: '1'
               }
            });
            sandbox.clock.tick(100);

            var hasHrefLengthError = vm.trigger.args.some(function(arg) {
                return arg[1] && arg[1].messages && arg[1].messages.indexOf('hrefLength') != 1;
            });

            expect(hasHrefLengthError).to.equal(true);
        });

        it('Если в ссылка изменилась, триггерится событие url-check-error с ошибкой hrefChanged', function() {
            sandbox.stub(hrefControl, 'val').callsFake(function() {
                return {
                    href: 'stol.ru',
                    protocol: 'http://',
                    url: 'http://stol.ru'
                }
            });
            sandbox.spy(vm, 'trigger');

            hrefControl.trigger('state:changed', {
                isReady: true,
                validatedData: {
                    domain: 'stol.ru',
                    domain_sign: '12345',
                    domain_redir: '123',
                    domain_redir_sign: '123456',
                    market_rating: '1'
               }
            });
            sandbox.clock.tick(100);

            var hasHrefChangedError = vm.trigger.args.some(function(arg) {
                return arg[1] && arg[1].messages && arg[1].messages.indexOf('hrefChanged') != 1;
            });

            expect(hasHrefChangedError).to.equal(true);
        });
    })
});
