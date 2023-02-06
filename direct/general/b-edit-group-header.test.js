describe('b-edit-group-header', function() {
    var block,
        ctx = {
            block: 'b-edit-group-header',
            group_name: 'Тестовая группа',
            groupNumber: '12345',
            errorPath: '',
            text: 'Тестовый текст'
        },
        js = {
            modelParams: {
                id: 1644061276,
                name: "m-group"
            },
            banners_quantity: 3
        },
        modelData =  {
            adgroup_id: 1644061276,
            banners_quantity: js.banners_quantity,
            group_name: 'Другое имя группы',
            adgroup_type: 'text',
            banners: [
                {
                    bid: 1,
                    title: '1',
                    isNewBanner: true
                },
                {
                    bid: 2,
                    title: '2'
                }
            ]
        },
        sandbox,
        model;

    describe('DOM', function() {
        before(function () {
            block = u.createBlock(ctx);
        });

        after(function() {
            block.destruct();
        });

        ['label', 'control', 'info'].forEach(function(elem) {
            it('Есть элемент ' + elem, function() {
                expect(block).to.haveElem(elem);
            });
        });

        describe('Элемент control', function() {
            it('Есть элемент с номеров группы', function() {
                expect(block.elem('group-number').text()).to.equal('№ ' + ctx.groupNumber);
            });

            it('Есть инпут с названием группы', function() {
                expect(block.findBlockInside(block.elem('name-input'), 'input').val()).to.equal(ctx.group_name);
            });
        });

        it('Есть элемент с текстом подсказки', function() {
            expect(block.elem('info-content').text()).to.equal('Тестовый текст');
        });
    });

    describe('Публичные методы', function() {
        beforeEach(function() {
            model = BEM.MODEL.create(js.modelParams, modelData);
            [1, 2].forEach(function(bannerNo) {
                BEM.MODEL.create(
                    {
                        name: 'm-banner',
                        id: bannerNo,
                        parentName: 'm-group',
                        parentId: '1644061276'
                    },
                    {
                        isNewBanner: bannerNo == 2
                    }
                );
            });
            block = u.createBlock(u._.extend(ctx, { js: js }));
        });

        afterEach(function() {
            block.destruct();
            model.destruct();
        });

        it('Метод getGroupModelParams возвращает параметры модели', function() {
            expect(block.getGroupModelParams()).to.deep.equal(ctx.js.modelParams);
        });

        describe('Метод setGroupModelParams', function() {
            it('устанавливает название группы из модели', function() {
                expect(block.findBlockInside(block.elem('name-input'), 'input').val()).to.equal('Другое имя группы');
            });

            describe('устанавливает подсказку про количество объявлений из модели', function() {
                it('Если группа была скопирована, то смотрит на количество баннеров в группе', function() {
                    model.set('isCopyGroup', true);
                    block.setGroupModelParams(js.modelParams);

                    expect(block.elem('info-content').text()).to.equal('2 из 3 объявлений')
                });

                it('Если группа не скопированная, то смотрит на все НЕ новые баннеры', function() {
                    expect(block.elem('info-content').text()).to.equal('1 из 3 объявлений')
                });
            });

            it('Если у группы нет номера (новая группа), то элемент с номером группы не показывается', function() {
                model.set('adgroup_id', null);
                block.setGroupModelParams(js.modelParams);

                expect(block).not.to.haveMod('show-inner-row');
            });

            it('Если у группа скопирована, то элемент с номером группы не показывается', function() {
                model.set('isCopyGroup', true);
                block.setGroupModelParams(js.modelParams);

                expect(block).not.to.haveMod('show-inner-row');
            });

            it('Если у группы есть номер и она не скопирована, то элемент с номером группы показывается', function() {
                expect(block).to.haveMod('show-inner-row', 'yes');
            });

            it('Если у группы есть номера и она не скопирована, устанавливает номер группы из модели', function() {
                expect(block.elem('group-number').text()).to.equal('№ 1644061276');
            });
        });
    });

    describe('Cобытия', function() {
        beforeEach(function() {
            model = BEM.MODEL.create(js.modelParams, modelData);
            [1, 2].forEach(function(bannerNo) {
                BEM.MODEL.create(
                    {
                        name: 'm-banner',
                        id: bannerNo,
                        parentName: 'm-group',
                        parentId: '1644061276'
                    },
                    {
                        isNewBanner: bannerNo == 2
                    }
                );
            });
            block = u.createBlock(u._.extend(ctx, { js: js }));
        });

        afterEach(function() {
            block.destruct();
            model.destruct();
        });

        it('При смене текста в инпуте с именем группы меняется поле в модели', function() {
            block.findBlockInside(block.elem('name-input'), 'input').val('Моя новая группа');

            expect(model.get('group_name')).to.equal('Моя новая группа');
        });

        describe('При изменении поля group_name в модели', function() {
            it('Если поле ввода в фокусе, то значение в поле не меняется', function() {
                block.findBlockInside(block.elem('name-input'), 'input').setMod('focused');
                model.set('group_name', 'Моя совсем новая группа');

                expect(block.findBlockInside(block.elem('name-input'), 'input').val()).to.equal('Другое имя группы');
            });

            it('Если поле ввода не в фокусе, меняется значение в поле ввода', function() {
                model.set('group_name', 'Моя совсем новая группа');

                expect(block.findBlockInside(block.elem('name-input'), 'input').val()).to.equal('Моя совсем новая группа');
            });
        });
    });

    describe('Утилита getInfoContentText', function() {
        describe('Если количество выбранных объявлений в равно количеству объявлений в группе', function() {
            ['performance', 'dynamic', 'mobile_content', 'text'].forEach(function(type) {
                describe('Если тип кампании - ' + type, function() {
                    [
                        {
                            count: 1,
                            text: type == 'performance' ? '1 баннер' : '1 объявление'
                        },
                        {
                            count: 2,
                            text: type == 'performance' ? '2 баннера' : '2 объявления'
                        },
                        {
                            count: 3,
                            text: type == 'performance' ? '3 баннера' : '3 объявления'
                        },
                        {
                            count: 5,
                            text: type == 'performance' ? '5 баннеров' : '5 объявлений'
                        },
                        {
                            count: 11,
                            text: type == 'performance' ? '11 баннеров' : '11 объявлений'
                        }
                    ].forEach(function(testSet) {
                        it('Если выбран ' + testSet.count + ' баннер, то текст будет "' + testSet.text + '"', function() {
                            var text = u['b-edit-group-header'].getInfoContentText(testSet.count, testSet.count, type);

                            expect(text).to.equal(testSet.text);
                        })
                    })
                });
            });
        });

        describe('Если количество выбранных объявлений в группе не равно количеству объявлений в группе', function() {
            ['performance', 'dynamic', 'mobile_content', 'text'].forEach(function(type) {
                describe('Если тип кампании - ' + type, function() {
                    [
                        {
                            count: 0,
                            total: 1,
                            text: type == 'performance' ? '0 из 1 баннера' : '0 из 1 объявления'
                        },
                        {
                            count: 1,
                            total: 2,
                            text: type == 'performance' ? '1 из 2 баннеров' : '1 из 2 объявлений'
                        },
                        {
                            count: 1,
                            total: 3,
                            text: type == 'performance' ? '1 из 3 баннеров' : '1 из 3 объявлений'
                        },
                        {
                            count: 3,
                            total: 5,
                            text: type == 'performance' ? '3 из 5 баннеров' : '3 из 5 объявлений'
                        },
                        {
                            count: 4,
                            total: 11,
                            text: type == 'performance' ? '4 из 11 баннеров' : '4 из 11 объявлений'
                        }
                    ].forEach(function(testSet) {
                        it('Если выбран ' + testSet.count + ' баннер, то текст будет "' + testSet.text + '"', function() {
                            var text = u['b-edit-group-header'].getInfoContentText(testSet.count, testSet.total, type);

                            expect(text).to.equal(testSet.text);
                        })
                    })
                });
            });
        });
    })
});
