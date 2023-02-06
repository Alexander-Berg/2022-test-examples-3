describe('b-vcard-control_type_banner', function() {
    var bannerModel,
        groupModel,
        groupModelParams = {
            name: 'm-group',
            id: 'group-id'
        },
        bannerModelParams = {
            id: 'banner-id',
            name: 'm-banner',
            parentName: 'm-group',
            parentId: 'group-id'
        },
        bemBlock,
        sandbox;

    function createBlock(extraParams, banner) {
        bannerModel = BEM.MODEL.create(bannerModelParams, banner || {});
        groupModel = BEM.MODEL.getOrCreate(groupModelParams);

        bemBlock = u.createBlock(u._.extend({
            block: 'b-vcard-control',
            modelParams: bannerModelParams,
            mods: { type: 'banner' }
        }, extraParams));
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true,
            useFakeServer: true
        })
    });

    afterEach(function() {
        sandbox.clock.tick(0);

        bannerModel.destruct();
        groupModel.destruct();
        bemBlock.destruct();

        sandbox.restore();
    });

    describe('Начальное состояние', function() {
        beforeEach(function() {
            createBlock({
                vcardHint: 'vcardHint',
                errors: [ 'error1' ],
                is_vcard_open: true
            });
        });

        it('vcardHint должен соответствовать заданному в параметрах', function() {
            expect(bemBlock.elem('hint').html()).to.be.equal('vcardHint');
        });

        it('Если заданы errors, рисуем блок с ошибками', function() {
            expect(bemBlock.elem('errors').length).to.be.equal(1);
        });

        it('Если is_vcard_open = true, то показываем ссылку "скрыть"', function() {
            expect(bemBlock).to.haveMod(bemBlock.elem('link'), 'visibility', 'visible');
        });

        it('Если is_vcard_open = true рисуем блок с визиткой', function() {
            expect(bemBlock.elem('vcard-content').length).to.be.equal(1);
        });
    });

    describe('API', function() {
        beforeEach(function() {
            createBlock({});
            sandbox.clock.tick(1);
        });

        it('isVCardEmpty вернет true если в нее передан пустой объект или объект только с полем workitme', function() {
            expect(bemBlock.isVCardEmpty({ worktime: 'bla' })).to.be.equal(true);
        });

        it('Если визитка не пустая, то isVCardEmpty вернет false', function() {
            expect(bemBlock.isVCardEmpty({ city: 'bla-bla' })).to.be.equal(false);
        });

        ['clear', 'fill', 'isVCardEmpty'].forEach(function(name) {
            it('Должна быть функция ' + name, function() {
                expect(bemBlock[name]).to.be.an.instanceof(Function);
            });
        });

        describe('Метод fill', function() {
            it('Если визитка есть, дергает метод fill у блок визитки', function() {
                var vcard;

                // cоздаем визитку заранее, чтобы можно было повесить стаб
                bemBlock.fill({ vcard: { city: '' }, has_vcard: true }, { resolve: function() {} });
                sandbox.clock.tick(1);
                vcard = bemBlock.findBlockInside('b-form-vcard');
                sandbox.spy(vcard, 'fill');

                bemBlock.fill({ vcard: { city: 'Штрана' }, has_vcard: true }, { resolve: function() {} });

                expect(vcard.fill.called).to.be.true;
            });

            it('Если визитки нет, не открывает блок', function() {
                bemBlock.fill({ has_vcard: false }, { resolve: function() {} });

                expect(bemBlock.elem('vcard-content').length).to.be.equal(0);
            });

            it('Если визитки нет, а блок есть и в баннере визитка есть - показывает предупреждение', function() {
                sandbox.spy(BEM.blocks['b-confirm'], 'open');
                // cоздаем визитку
                bemBlock.fill({ vcard: { city: '' }, has_vcard: true }, { resolve: function() {} });
                sandbox.stub(bannerModel, 'get').withArgs('has_vcard').returns(true);
                sandbox.clock.tick(1);

                bemBlock.fill({ has_vcard: false }, { resolve: function() {} });
                sandbox.clock.tick(1);

                expect(BEM.blocks['b-confirm'].open.called).to.be.true;

                BEM.blocks['b-confirm']._buttonNo.trigger('click');
                BEM.blocks['b-confirm'].open.restore();
            });

            it('Если визитки не передано и в баннере нет, очищает форму', function() {
                sandbox.spy(bemBlock, 'clear');
                sandbox.stub(bannerModel, 'get').withArgs('has_vcard').returns(false);
                sandbox.clock.tick(1);

                bemBlock.fill({ has_vcard: false }, { resolve: function() {} });

                sandbox.clock.tick(1);

                expect(bemBlock.clear.called).to.be.true;
            });
        });

        it('Метод clear очищает форму', function() {
            createBlock({}, {
                is_vcard_open: true,
                has_vcard: true,
                vcard: { phone: '999' }
            });
            sandbox.clock.tick(1);

            expect(bannerModel.get('vcard').phone).to.equal('999');
            // cоздаем визитку
            bemBlock.fill({ vcard: { city: '' }, has_vcard: true }, { resolve: function() {} });
            sandbox.clock.tick(1);

            bemBlock.clear();
            sandbox.clock.tick(1);

            expect(bannerModel.get('vcard').phone).to.equal('');
        });
    });

    describe('Отрисовка визитки', function() {
        beforeEach(function() {
            createBlock({}, {
                is_vcard_open: false,
                has_vcard: false
            });
        });

        it('Если is_vcard_open = false визитка при инициализации не отрисовывается', function() {
            expect(bemBlock.findBlocksInside('b-form-vcard').length).to.be.equal(0);
        });

        it('Если вызываем fill c данными { has_vcard: true }, должна отрисоваться визитка', function() {
            bemBlock.fill({
                has_vcard: true,
                vcard: {
                    city: 'city'
                }
            });

            expect(bemBlock.findBlocksInside('b-form-vcard').length).to.be.equal(1);
        });
    });
});
