describe('b-group-interests', function() {
    var ctx = {
            block: 'b-group-interests',
            interests: {
                41: {
                    name: "Не определено",
                    id: "41",
                    orderIndex: "2500",
                    available: true
                },
                42: {
                    name: "Бизнес",
                    id: "42",
                    orderIndex: "100",
                    available: false
                },
                43: {
                    name: "Связь",
                    id: "43",
                    orderIndex: "200",
                    available: true
                },
                44: {
                    name: "Образование",
                    id: "44",
                    orderIndex: "300",
                    available: true
                }
            },
            selectedInterestsIds: ['43', '44'],
            modelParams: {
                name: "dm-mobile-content-group",
                id: 865770052
            },
            readOnly: false
        },
        block,
        groupModel,
        sandbox;

    beforeEach(function() {
        u.stubCurrencies();
        groupModel = BEM.MODEL.create(ctx.modelParams);
        block = u.createBlock(ctx);
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        u.restoreCurrencies();
        block.destruct();
        sandbox.restore();
    });

    describe('Корректность инициализации', function() {
        it('Если блок не ридонли, то есть кнопка редактирования', function() {
            expect(block.elem('edit-param-button').length).not.to.equal(0);
        });

        it('Если блок ридонли, то нет кнопки редактирования', function() {
            var roBlock = u.createBlock({
                block: 'b-group-interests',
                js: false,
                readOnly: true
            });

            expect(roBlock.elem('edit-param-button').length).to.equal(0);

            roBlock.destruct();
        });
    });

    describe('Обработка нажатий на кнопки', function() {
        var popup,
            editInterestsTargetingBlock;

        beforeEach(function() {
            block._editParamButton.trigger('click');

            popup = block._popup;
            editInterestsTargetingBlock = popup.findBlockInside('b-edit-interests-targeting');

            sandbox.spy(popup, 'hide');
        });

        afterEach(function() {
            editInterestsTargetingBlock.destruct();
        });

        it('При нажатии на кнопку ОТМЕНА попап закрывается без предупреждения об изменениях во внутреннем блоке', function() {
            editInterestsTargetingBlock.trigger('cancel');

            expect(popup.hide.calledWith({ force: true })).to.be.true;
        });

        it('При отмене не происходит сохранения данных в модель', function() {
            var targetInterestsData = block._groupModel.get('target_interests');

            editInterestsTargetingBlock.trigger('cancel');

            expect(block._groupModel.get('target_interests')).to.deep.equal(targetInterestsData);
        });

        it('При сохранении обновляется модель группы', function() {
            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41'] });

            expect(block._groupModel.get('target_interests')[0]['target_category_id']).to.equal('41');
        });

        it('Если в модели интереса есть другие поля (от сервера), они не должны затираться при сохранении', function() {
            block._groupModel.set('target_interests', [{
                ret_id: '6507821',
                target_category_id: '41'
            }]);

            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41'] });

            expect(block._groupModel.get('target_interests')[0]['target_category_id']).to.equal('41');
            expect(block._groupModel.get('target_interests')[0]['ret_id']).to.equal('6507821');
        });

        it('При сохранении обновляется текст подписи', function() {
            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41'] });

            expect(block._editParamButton.elem('params-sign').text()).to.equal('Не определено');
        });

        it('При сохранении попап закрывается без дополнительных сообщений', function() {
            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41'] });

            expect(popup.hide.calledWith({ force: true })).to.be.true;
        });
    });

    describe('Обновление данных про таргетинг на инетерсы в модели группы', function() {
        var popup,
            editInterestsTargetingBlock;

        beforeEach(function() {
            block._editParamButton.trigger('click');

            popup = block._popup;
            editInterestsTargetingBlock = popup.findBlockInside('b-edit-interests-targeting');
        });

        afterEach(function() {
            editInterestsTargetingBlock.destruct();
        });

        [
            {
                type: 'full',
                text: 'полном обновлении данных',
                length: 3
            },
            {
                type: 'add',
                text: 'добавлении нового интереса',
                length: 4
            },
            {
                type: 'delete',
                text: 'удалении всех инетерсов',
                length: 0
            }
        ].forEach(function(test) {
            describe('При ' + test.text + ' в модели результат валидный', function() {
                beforeEach(function() {
                    switch (test.type) {
                        case 'full':
                            block._groupModel.set('target_interests', []);
                            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41', '42', '43'] });
                            break;
                        case 'add':
                            block._groupModel.set('target_interests', [
                                { target_category_id: '41' },
                                { target_category_id: '42' },
                                { target_category_id: '43' }
                            ]);
                            editInterestsTargetingBlock.trigger('save', { selectedIds: ['41', '42', '43', '44'] });
                            break;
                        case 'delete':
                            block._groupModel.set('target_interests', [
                                { target_category_id: '41' },
                                { target_category_id: '42' },
                                { target_category_id: '43' }
                            ]);
                            editInterestsTargetingBlock.trigger('save', { selectedIds: [] });
                    }
                });

                it('Данные в модели записываются в массив', function() {
                    expect(u._.isArray(block._groupModel.get('target_interests'))).to.equal(true);
                });

                it('Все данные записываются в модели в массив', function() {
                    expect(block._groupModel.get('target_interests').length).to.equal(test.length);
                });

                it('Данные в модели имеют структуру target_category_id:idNum', function() {
                    var data = block._groupModel.get('target_interests'),
                        result = data.every(function(interest) {
                            return interest.target_category_id != 'undefined' && typeof interest.target_category_id == 'string';
                    });

                    expect(result).to.equal(true);
                });
            });
        });
    });
});
