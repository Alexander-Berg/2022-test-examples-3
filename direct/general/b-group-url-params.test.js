describe('b-group-url-params', function() {
    var ctx = {
            "block": "b-group-url-params",
            "group": {
                "cid": "21284964",
                "pid": "1891143165",
                "adgroup_id": 1891143165,
                "href_params":null
            },
            "parentModelParams": {
                "name": "dm-dynamic-group",
                "id": 1891143165
            }
        },
        block,
        dm,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        dm = BEM.MODEL.create(ctx.parentModelParams);
        block = u.createBlock(ctx);
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct();
    });

    describe('Логика работы кнопки ОТМЕНА', function() {
        beforeEach(function() {
            block.model.set('params', 'fff');
            block._cancel.trigger('click');
        });

        it('При нажатии на ОТМЕНА затирается поле params VM-модели', function() {
            expect(block.model.get('params')).to.equal('');
        });

        it('При нажатии на ОТМЕНА данные попадают в поле tmpParams VM-модели', function() {
            expect(block.model.get('tmpParams')).to.equal('fff');
        });

        it('При нажатии на ОТМЕНА данные удаляются из DM-модели', function() {
            expect(block.model.getDM().get('href_params')).to.equal('')
        });

        it('При нажатии на ОТМЕНА список ошибок пуст', function() {
            expect(block._message.text()).to.equal('');
        });
    });

    describe('Логика работы кнопки ДОБАВИТЬ', function() {
        describe('Если в input пусто', function() {
            function setData() {
                block.findBlockInside('input').val('');
                block._switcher.trigger('click');
                sandbox.clock.tick(1000);
            }

            it('Если есть данные в tmpParams, они пишутся в VM-модель', function() {
                block.model.set('tmpParams', 'fff');
                setData();

                expect(block.model.get('params')).to.equal('fff');
            });

            it('Если есть данные в tmpParams, они пишутся в DM-модель', function() {
                block.model.set('tmpParams', 'fff');
                setData();

                expect(block.model.getDM().get('href_params')).to.equal('fff');
            });

            it('Если нет данных в tmpParams, в VM-модель не пишется ничего', function() {
                block.model.set('tmpParams', '');
                setData();

                expect(block.model.get('params')).to.equal('');
            });

            it('Если нет данных в tmpParams, в DM-модель не пишется ничего', function() {
                block.model.set('tmpParams', '');
                setData();

                expect(block.model.getDM().get('href_params')).to.equal('');
            });
        });

        describe('Если в input есть данные', function() {
            beforeEach(function() {
                block.findBlockInside('input').val('fff');
                block._switcher.trigger('click');
                sandbox.clock.tick(1000);
            });
            it('Данные пишутся в VM-модель', function() {
                expect(block.model.get('params')).to.equal('fff');
            });

            it('Данные пишутся в DM-модель', function() {
                expect(block.model.getDM().get('href_params')).to.equal('fff');
            });
        });
    });

    describe('При изменении поля params VM-модели', function() {
        beforeEach(function() {
            block.model.set('params', 'fff');
            sandbox.clock.tick(1000);
        });

        it('Очищается список ошибок', function() {
            expect(block._message.text()).to.equal('');
        });

        it('Данные синхронизируются с DM-моделью', function() {
            expect(block.model.getDM().get('href_params')).to.equal('fff');
        });
    });

    describe('При окончании валидации VM-модели', function() {
        it('Если данные валидны, очищается список ошибок', function() {
            block.model.trigger('validated', { valid: true });

            expect(block._message.text()).to.equal('');
        });
        it('Если данные невалидны, ошибки высвечиваются в панели ошибок', function() {
            block.model.trigger('validated', { valid: false, errors: [{ text: 'Ошибка' }] });

            expect(block._message.text()).to.equal('Ошибка');
        });
    });


    describe('Валидация', function() {
        it('Параметры длиной меньше 1024 символов валидны', function() {
            block.model.set('params', 'fff');

            expect(block.model.validate('params').valid).to.equal(true);
        });

        it('Параметры длиной ровно 1024 символов невалидны', function() {
            block.model.set('params', new Array(1025).join('x'));

            expect(block.model.validate('params').valid).to.equal(true);
        });

        it('Параметры длиной больше 1024 символов невалидны', function() {
            block.model.set('params', new Array(1026).join('x'));

            expect(block.model.validate('params').errors[0].text).to.equal('Превышена максимальная длина строки параметров URL, 1024 символа.');
        });
    })
});
