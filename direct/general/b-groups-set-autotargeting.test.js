describe('b-groups-set-autotargeting', function() {
    var block,
        campaign,
        groupsIds = ['1', '2', '3'],
        sandbox,
        popup,
        popupCtx = {
            block: 'popup',
            mods: { animate: 'no' },
            content: [
                {
                    elem: 'content',
                    content: ''
                }
            ]
        },
        ctx = {
            block: 'b-groups-set-autotargeting',
            js: {
                campModelParams: { name: 'm-campaign', id: '1' },
                fromTests: true
            },
            mixBlock: 'block',
            strategy: {
                name: 'default'
            }
        },
        createBlock = function(mod) {
            popup = u.createBlock(popupCtx);
            popup.setContent(BEMHTML.apply(u._.extend(ctx, { mods: { view: mod } }))).show($('body'));
            block = popup.findBlockInside('b-groups-set-autotargeting');
            sandbox.stub(block, '_getMainBid').callsFake(function() {
                return 1;
            });
        };

    before(function() {
        campaign = BEM.MODEL.create({ name: 'm-campaign', id: '1' }, { mediaType: 'text', cid: '1' });
        groupsIds.forEach(function(groupId) {
            BEM.MODEL.create({ name: 'm-group', id: groupId }, { adgroup_id: groupId });
        });
        campaign.set('selectedAdgroupIds', groupsIds);
    });

    after(function() {
        groupsIds.forEach(function(groupId) {
            BEM.MODEL.getOne({ name: 'm-group', id: groupId }).destruct();
        });
        campaign.destruct();
    });

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true, useFakeServer: true });
        u.stubCurrencies2(sandbox);
    });

    afterEach(function() {
        sandbox.restore();
        block && block.destruct();
        popup && popup.destruct();
    });

    describe('DOM', function() {
        it('В ручном режиме есть контрол ставки', function() {
            createBlock('manual-beta');

            expect(block.findBlockInside('input')).not.to.be.null;
        });
    });

    it('По кнопке отмена попап прячется', function() {
        createBlock('manual-beta');
        block.findBlockInside('cancel', 'button').trigger('click');

        expect(popup.isShown()).to.be.false;
    });

    describe('Отправка запроса', function() {
        var accept;
        beforeEach(function() {
            createBlock('auto');
            sandbox.stub(block, '_getChangedData').callsFake(function() {
                return {};
            });

            accept = block.findBlockInside('accept', 'button');
        });

        describe('перед отправкой запроса', function() {
            beforeEach(function() {
                accept.trigger('click');
            });

            it('дизейблится кнопка сохранения', function() {
                expect(accept).to.haveMod('disabled', 'yes');
            });

            it('меняется текст на кнопке сохранения', function() {
                expect(accept.domElem.text()).to.equal('Сохраняется...');
            });

            it('показывается спиннер', function() {
                expect(block).not.to.haveMod(block.elem('spinner'), 'hidden');
            });
        });

        describe('При успешном ajax-запросе', function() {
            beforeEach(function() {
                accept.trigger('click');
            });

            describe('если есть ошибки', function() {
                function respond() {
                    sandbox.server.respondWith(
                        'POST',
                        '/registered/main.pl',
                        [200, { "Content-Type": "application/json" }, '{"error":["Ошибка номер 1"]}']
                    );
                    sandbox.server.respond();
                }

                it('они показываются в попапе', function() {
                    sandbox.spy(BEM.blocks['b-user-dialog'], 'alert');

                    respond();

                    expect(BEM.blocks['b-user-dialog'].alert.args[0][0].message[1][0])
                        .to.include('Ошибка номер 1');
                    BEM.blocks['b-user-dialog'].alert.restore();
                });

                it('по кнопке ок попап закрывается', function() {
                    expect(popup.isShown()).to.be.true;

                    sandbox.stub(BEM.blocks['b-user-dialog'], 'alert').callsFake(function(options) {
                        options.onCancel.apply(options.callbackCtx);
                        sandbox.clock.tick(1000);
                    });

                    respond();

                    expect(popup.isShown()).to.be.false;
                });

            });

            it('Если нет ошибок, попап с алертом не показывается и сам попап не закрывается', function() { // потому что ожидается перезагрузка страницы
                sandbox.spy(BEM.blocks['b-user-dialog'], 'alert');
                sandbox.server.respondWith(
                    'POST',
                    '/registered/main.pl',
                    [200, { "Content-Type": "application/json" }, '{"success":"1"}']
                );
                sandbox.server.respond();

                expect(BEM.blocks['b-user-dialog'].alert.called).to.be.false;
                expect(popup.isShown()).to.be.true;
            })
        });

        describe('При ошибке в ajax-запросе', function() {
            beforeEach(function() {
                accept.trigger('click');
            });

            function respond() {
                sandbox.server.respondWith('POST', '/registered/main.pl', [404, {}, '']);
                sandbox.server.respond();
            }

            it('они показываются в попапе', function() {
                sandbox.spy(BEM.blocks['b-user-dialog'], 'alert');

                respond();

                expect(BEM.blocks['b-user-dialog'].alert.args[0][0].message)
                    .to.include('Неизвестная ошибка. Попробуйте позднее');
                BEM.blocks['b-user-dialog'].alert.restore();
            });

            it('по кнопке ок попап закрывается', function() {
                expect(popup.isShown()).to.be.true;

                sandbox.stub(BEM.blocks['b-user-dialog'], 'alert').callsFake(function(options) {
                    options.onCancel.apply(options.callbackCtx);
                    sandbox.clock.tick(1000);
                });

                respond();

                expect(popup.isShown()).to.be.false;
            });

        });
    });
});
