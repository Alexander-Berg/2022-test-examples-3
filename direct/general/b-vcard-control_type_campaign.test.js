describe('b-vcard-control_type_campaign', function() {
    var block,
        campaignModel,
        sandbox,
        vcardCtx = {
            "city_code": "",
            "extra_message": null,
            "country_code": "",
            "auto_point": "",
            "worktimes": [
                {
                    "m1": "00",
                    "h2": "18",
                    "h1": "10",
                    "d1": "0",
                    "m2": "00",
                    "d2": "4"
                }
            ],
            "cid": "8027269",
            "manual_bounds": "",
            "vcard_id": "37318076",
            "street": null,
            "uid": "567196",
            "auto_bounds": "",
            "is_auto_point": 0,
            "im_client": null,
            "country_geo_id": "",
            "house": null,
            "ext": "",
            "name": "ввв",
            "map_id": null,
            "im_login": null,
            "map_id_auto": null,
            "auto_precision": null,
            "city": "",
            "geo_id": "",
            "compiled_phone": "+7#86736#99-99-99#",
            "contactperson": "ввв",
            "manual_point": "",
            "country": "",
            "phone": "99-99-99",
            "contact_email": null,
            "metro": null,
            "worktime": "0#4#10#00#18#00",
            "apart": null,
            "org_details_id": null,
            "build": null
        };

    function createBlock(params) {
        var settingsBlockTree = u.getDOMTree({ block: 'b-campaign-settings', mods: { edit: 'no' } }),
            settingsBlock,
            blockTree = u.getDOMTree(u._.extend({
                block: 'b-vcard-control',
                mods: { type: 'campaign' },
                vcardHint: 'vcardHint',
                is_vcard_open: true,
                vcard: vcardCtx
            }, params));

        $('body').append(settingsBlockTree);
        settingsBlock = BEM.DOM.init(settingsBlockTree).bem('b-campaign-settings');
        settingsBlock.model = campaignModel;
        settingsBlockTree.append(blockTree);
        
        block = BEM.DOM.init(blockTree).bem('b-vcard-control');
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        campaignModel = BEM.MODEL.create({ name: 'm-campaign', id: '1' }, { vcard: vcardCtx });
    });

    afterEach(function() {
        sandbox.clock.tick(0);

        block.destruct();

        sandbox.restore();
    });

    describe('Начальное состояние', function() {
        beforeEach(function() {
            createBlock({
                errors: [ 'error1' ]
            });
        });

        it('vcardHint должен соответствовать заданному в параметрах', function() {
            expect(block.elem('hint').html()).to.be.equal('vcardHint');
        });

        it('Если заданы errors, рисуем блок с ошибками', function() {
            expect(block.elem('errors').length).to.be.equal(1);
        });

        it('Если is_vcard_open = true, то показываем ссылку "скрыть"', function() {
            expect(block).to.haveMod(block.elem('link'), 'visibility', 'visible');
        });

        it('Если is_vcard_open = true рисуем блок с визиткой', function() {
            expect(block).to.haveMod(block.elem('body'), 'opened', 'yes');
        });
    });

    describe('События', function() {
        beforeEach(function() {
            createBlock();
        });

        describe('При переключении чекбокса', function() {
            var tumbler;

            beforeEach(function() {
                tumbler = block.findBlockOn('tumbler', 'tumbler');
            });

            it('обновляется модель', function() {
                expect(block.model.get('with_ci')).to.be.true;
                tumbler.delMod('checked');
                tumbler.trigger('change', { checked: false });

                sandbox.clock.tick(1);

                expect(block.model.get('with_ci')).to.be.false;
            });

            it('Если тумблер закрыт - прячется ссылка "скрыть"', function() {
                tumbler.delMod('checked');
                tumbler.trigger('change', { checked: false });

                sandbox.clock.tick(1);

                expect(block).to.haveMod(block.elem('link'), 'visibility', 'hidden');
            });

            it('Если тумблер открыт - показывается ссылка "скрыть"', function() {
                tumbler.setMod('checked', 'yes');
                tumbler.trigger('change', { checked: true });

                sandbox.clock.tick(1);

                expect(block).to.haveMod(block.elem('link'), 'visibility', 'visible');
            });
        });

        it('При обновлении формы обновляется модель', function() {
            var vcard = block.findBlockInside('b-form-vcard');

            // снимаем lock
            sandbox.clock.tick(1);
            sandbox.stub(vcard, 'serialize').callsFake(function() {
                return { city: 'Штрана' };
            });
            vcard.trigger('update', { city: 'Штрана'});

            sandbox.clock.tick(1);

            expect(block.model.get('vcard').city).to.equal('Штрана');
        });

        describe('При обновлении модели ', function() {
            beforeEach(function() {
                var vcard = block.findBlockInside('b-form-vcard');

                // снимаем lock
                sandbox.clock.tick(1);
                sandbox.stub(vcard, 'serialize').callsFake(function() {
                    return { city: 'Штрана' };
                });

                vcard.trigger('update', { city: 'Штрана'});
            });

            it('триггерится событие change', function() {
                sandbox.spy(block, 'trigger');
                expect(block.trigger.calledWith('change'));
            });

            it('В начале обновлении модели ставится модификатор lock', function() {
                expect(block).to.haveMod('lock', 'yes');
            });

            it('В конце обновления модели снимается модификатор lock', function() {
                sandbox.clock.tick(1);

                expect(block).not.to.haveMod('lock');
            });
        });
    });
    
    describe('Модификатор lock', function() {
        beforeEach(function() {
            createBlock();
        });

        it('Если модификатор стоит, то при обновлении формы НЕ обновляется модель', function() {
            block.findBlockInside('b-form-vcard').trigger('update', { city: 'Штрана'});

            expect(block.model.get('vcard').city).not.to.equal('Штрана');
        });
    });

    describe('Публичные методы', function() {
        var vcard;

        beforeEach(function() {
            createBlock();
            vcard = block.findBlockInside('b-form-vcard');
            sandbox.clock.tick(1);
        });

        it('Метод fill не имплементирован', function() {
            expect(block.fill()).to.be.undefined;
        });

        it('Метод clear очищает форму', function() {
            block.clear();

            sandbox.clock.tick(1);

            expect(block.model.get('vcard').phone).to.equal('');
        });

        it('Метод isFormChanged возвращает true, если были изменения в визитке', function () {
            sandbox.stub(vcard, 'serialize').callsFake(function() {
                return { city: 'Штрана' };
            });

            vcard.trigger('update', { city: 'Штрана'});
            sandbox.clock.tick(1);

            expect(block.isFormChanged()).to.be.true;
        });

        it('Метод isFormChanged возвращает false, если не было изменений в визитке', function () {
            sandbox.clock.tick(1);

            expect(block.isFormChanged()).to.be.false;
        });

        it('Метод isFormChanged возвращает false, если не было изменения откатили', function () {
            sandbox.stub(vcard, 'serialize').callsFake(function() {
                return { city: 'Штрана' };
            });

            vcard.trigger('update', { city: 'Штрана'});
            sandbox.clock.tick(1);

            expect(block.isFormChanged()).to.be.true;

            sandbox.clock.tick(1);
            sandbox.restore();
            sandbox.stub(vcard, 'serialize').callsFake(function() {
                return { city: '' };
            });

            vcard.trigger('update', { city: ''});
            sandbox.clock.tick(1);

            expect(block.isFormChanged()).to.be.false;
        });

        describe('Валидация', function() {
            var tumbler;

            beforeEach(function() {
                tumbler = block.findBlockOn('tumbler', 'tumbler');
            });

            it('Если визитка выключена, возвращает true', function() {
                tumbler.delMod('checked');
                tumbler.trigger('change', { checked: false });

                sandbox.clock.tick(1);

                expect(block.validate()).to.be.true;
            });

            it('Если есть ошибки, возвращает текст ошибки', function() {
                sandbox.stub(vcard, 'validate').callsFake(function() {
                    return { error: 'error' };
                });

                tumbler.setMod('checked', 'yes');
                tumbler.trigger('change', { checked: true });

                expect(block.validate()).to.deep.equal({ error: 'error' });
            });

            it('Если все верно, возвращает true', function() {
                sandbox.stub(vcard, 'validate').callsFake(function() {
                    return true;
                });

                tumbler.setMod('checked', 'yes');
                tumbler.trigger('change', { checked: true });

                expect(block.validate()).to.be.true;
            });
        })
    });
});
