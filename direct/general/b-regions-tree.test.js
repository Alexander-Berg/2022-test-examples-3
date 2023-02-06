describe('b-regions-tree', function() {

    var sandbox,
        block;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
        block = u.createBlock({
            block: 'b-regions-tree',
            disableRegions: [225, 123],
            independentRegions: [377],
            isCommon: true,
            regions: [
                {
                    id: 100,
                    name: "Россия",
                    inner: [
                        {
                            id: 201,
                            name: "Центр",
                            contrastValueGroups: [1, 2],
                            inner: [
                                {
                                    id: 301,
                                    name: "Москва и область",
                                    inner: [
                                        { id: 401, name: "Москва" },
                                        { id: 402, name: "Болошиха" },
                                        { id: 403, name: "Бронницы" }
                                    ]
                                },
                                {
                                    id: 302,
                                    name: "Белгородская область",
                                    inner: [
                                        { id: 24, name: "Белгород" }
                                    ]
                                }
                            ]
                        },
                        { id: 202, name: "Северо-запад" },
                        { id: 203, name: "Поволжье" },
                        { id: 204, name: "Юг" }
                    ]
                }
            ]
        }, { inject: true });
    });

    afterEach(function() {
        block.destruct();
        sandbox.restore();
    });

    describe('getTreeState.state', function() {
        it('Должен вернуть пустой массив', function() {
            expect(block.getTreeState().state).to.be.empty;
        });

        it('Должен вернуть +Россия', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '202'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '203'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '301'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '302'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '403'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '24'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": []
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '202'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '203'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '203'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                        { "id": 202, "name": "Северо-запад" }
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                        { "id": 202, "name": "Северо-запад" },
                        { "id": 203, "name": "Поволжье" }
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                        { "id": 202, "name": "Северо-запад" },
                        { "id": 203, "name": "Поволжье" },
                        { "id": 204, "name": "Юг" }
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Москва и область, -Белгородская область, -Северо-запад', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '203'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([{
                "id": 100,
                "name": "Россия",
                "excluded": [
                    { "id": 301, "name": "Москва и область" },
                    { "id": 302, "name": "Белгородская область" },
                    { "id": 202, "name": "Северо-запад" }
                ]
            }]);
        });

        it('Должен вернуть +Россия, -Москва и область, -Белгородская область, -Северо-запад, -Поволжье', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '204'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([{
                "id": 100,
                "name": "Россия",
                "excluded": [
                    { "id": 301, "name": "Москва и область" },
                    { "id": 302, "name": "Белгородская область" },
                    { "id": 202, "name": "Северо-запад" },
                    { "id": 203, "name": "Поволжье" }
                ]
            }]);
        });

        it('Должен вернуть +Россия, -Москва и область, -Белгородская область, -Северо-запад, -Поволжье, -Юг', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([{
                "id": 100,
                "name": "Россия",
                "excluded": [
                    { "id": 301, "name": "Москва и область" },
                    { "id": 302, "name": "Белгородская область" },
                    { "id": 202, "name": "Северо-запад" },
                    { "id": 203, "name": "Поволжье" },
                    { "id": 204, "name": "Юг" }
                ]
            }]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва ', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    { id: 401, name: "Москва", "excluded":[] }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва, +Болошиха ', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    { id: 401, name: "Москва", "excluded":[] },
                    { id: 402, name: "Болошиха", "excluded":[] }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва, +Болошиха, +Бронницы ', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '403'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    { id: 401, name: "Москва", "excluded":[] },
                    { id: 402, name: "Болошиха", "excluded":[] },
                    { id: 403, name: "Бронницы", "excluded":[] }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва и область', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '301'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '403'), 'checkbox').setMod('checked', 'yes');
            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    {
                        id: 301,
                        name: "Москва и область",
                        excluded:[]
                    }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва и область, -Бронницы', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '301'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '402'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                        { "id": 202, "name": "Северо-запад" },
                        { "id": 203, "name": "Поволжье" },
                        { "id": 204, "name": "Юг" }
                    ]
                },
                {
                    id: 301,
                    name: "Москва и область",
                    excluded:[
                        { id: 403, name: "Бронницы" }
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Москва и область, -Болошиха, -Бронницы', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '301'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '401'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                {
                    "id": 100,
                    "name": "Россия",
                    "excluded": [
                        { "id": 201, "name": "Центр" },
                        { "id": 202, "name": "Северо-запад" },
                        { "id": 203, "name": "Поволжье" },
                        { "id": 204, "name": "Юг" }
                    ]
                },
                {
                    id: 301,
                    name: "Москва и область",
                    excluded:[
                        { id: 402, name: "Болошиха" },
                        { id: 403, name: "Бронницы" }
                    ]
                }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, +Белгород', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '24'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    {
                        id: 24,
                        name: "Белгород",
                        excluded: []
                    }
            ]);
        });

        it('Должен вернуть +Россия, -Центр, -Северо-запад, -Поволжье, -Юг, -Москва и область, -Москва, -Болошиха, +Бронницы', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').setMod('checked', 'yes');
            block.findBlockOn(block.elem('region-checkbox', 'id', '301'), 'checkbox').setMod('checked', 'yes');

            sandbox.clock.tick(500);

            expect(block.getTreeState().state).to.deep.equal([
                    {
                        "id": 100,
                        "name": "Россия",
                        "excluded": [
                            { "id": 201, "name": "Центр" },
                            { "id": 202, "name": "Северо-запад" },
                            { "id": 203, "name": "Поволжье" },
                            { "id": 204, "name": "Юг" }
                        ]
                    },
                    {
                        id: 301,
                        name: "Москва и область",
                        excluded:[
                            { id: 401, name: "Москва" },
                            { id: 402, name: "Болошиха" },
                            { id: 403, name: "Бронницы" }
                        ]
                    }
            ]);
        });
    });

    describe('getTreeState.wasReset', function() {
        it('false, если регионы не сбрасывали', function() {
            expect(block.getTreeState().wasReset).to.be.false;
        });

        it('true, если регионы сбрасывали', function() {
            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

            block.reset();

            expect(block.getTreeState().wasReset).to.be.true;

            BEM.blocks['b-user-dialog'].confirm.restore();
        });
    });

    describe('getTreeState.changesInfo', function() {
        it('Если ничего не меняем changesInfo = {}', function() {
            expect(block.getTreeState().changesInfo).to.deep.equal({});
        });

        it('При изменении безконфликтного региона, информация о его состоянии добавляется в changesInfo', function() {
            block.findBlockOn(block.elem('region-checkbox', 'id', '202'), 'checkbox')
                .setMod('checked', 'yes')
                .trigger('click');

            sandbox.clock.tick(100);

            expect(block.getTreeState().changesInfo['202']).to.be.true;
        });

        it('При изменении конфликтного региона и не подтверждении своего согласия с текстом в показанном предупреждении, информация о его состоянии не добавляется в changesInfo', function() {
            var regionCheckbox = block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox');

            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

            regionCheckbox.trigger('click');
            sandbox.clock.tick(100);

            expect(block.getTreeState().changesInfo['201']).to.be.undefined;

            BEM.blocks['b-user-dialog'].confirm.restore();
        });

        it('При изменении конфликтного региона и подтверждении своего согласия с текстом в показанном предупреждении, информация о его состоянии добавляется в changesInfo', function() {
            var regionCheckbox = block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox');

            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function(options) {
                options.onConfirm.apply(options.callbackCtx);
            });

            regionCheckbox.setMod('checked', 'yes').trigger('click');
            sandbox.clock.tick(100);

            expect(block.getTreeState().changesInfo['201']).to.be.true;

            BEM.blocks['b-user-dialog'].confirm.restore();
        });
    });

    describe('getTreeState.resolvedRegions', function() {
        it('Если ничего не меняем resolvedRegions = {}', function() {
            expect(block.getTreeState().resolvedRegions).to.deep.equal({});
        });

        it('При изменении конфликтного региона и не подтверждении своего согласия с текстом в показанном предупреждении, информация о его состоянии не добавляется в resolvedRegions', function() {
            var regionCheckbox = block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox');

            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

            regionCheckbox.setMod('checked', '').trigger('click');
            sandbox.clock.tick(100);

            expect(block.getTreeState().resolvedRegions['201']).to.be.undefined;

            BEM.blocks['b-user-dialog'].confirm.restore();
        });

        it('При изменении конфликтного региона и подтверждении своего согласия с текстом в показанном предупреждении, информация о его состоянии добавляется в resolvedRegions', function() {
            var regionCheckbox = block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox');

            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function(options) {
                options.onConfirm.apply(options.callbackCtx);
            });

            regionCheckbox.setMod('checked', '').trigger('click');
            sandbox.clock.tick(100);

            expect(block.getTreeState().resolvedRegions['201']).to.be.true;

            BEM.blocks['b-user-dialog'].confirm.restore();
        });
    });

    describe('Регионы с различиями', function() {
        var regionWithDifferences;

        beforeEach(function() {
            regionWithDifferences = block.findBlockOn(block.elem('region-checkbox', 'id', '201'), 'checkbox');
        });

        it('Регион должен иметь иконку, обозначающую различия', function() {
            expect(block.getMod(block._getRegionByChildDomNode(block.elem('contrast-value-hint')), 'id')).to.equal('201');
        });

        it('При попытке изменить состояние чекбокса такого региона должно появляться предупреждающее сообщение', function() {
            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

            regionWithDifferences.setMod('checked', '').trigger('click');
            sandbox.clock.tick(100);

            expect(BEM.blocks['b-user-dialog'].confirm.called).to.be.equal(true);
            BEM.blocks['b-user-dialog'].confirm.restore();
        });

        it('При попытке изменить состояние чекбокса региона, изменение которого по цепочке вниз может затронуть регион, у которого есть различия, должно появляться предупреждающее сообщение', function() {
            sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

            block.findBlockOn(block.elem('region-checkbox', 'id', '100'), 'checkbox').trigger('click');
            sandbox.clock.tick(100);

            expect(BEM.blocks['b-user-dialog'].confirm.called).to.be.equal(true);

            BEM.blocks['b-user-dialog'].confirm.restore();
        });

        describe('Если пользователь не подтвердил свое согласие с текстом в предупреждении', function() {
            var initialCheckboxState;

            beforeEach(function() {
                initialCheckboxState = regionWithDifferences.isChecked();

                sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function() {});

                regionWithDifferences
                    .setMod('checked', regionWithDifferences.isChecked() ? '' : 'yes')
                    .trigger('click');
                sandbox.clock.tick(100);
            });

            afterEach(function() {
                BEM.blocks['b-user-dialog'].confirm.restore();
            });

            it('Выбор региона не меняется', function() {
                expect(regionWithDifferences.isChecked()).to.be.equal(initialCheckboxState);
            });

            it('Иконка различий рядом с регионом не удаляется', function() {
                expect(block.elem('contrast-value-hint').length).to.be.equal(1);
            });
        });

        describe('Если пользователь подтвердил свое согласие с текстом в предупреждении', function() {
            var initialCheckboxState;

            beforeEach(function() {
                initialCheckboxState = regionWithDifferences.isChecked();

                sandbox.stub(BEM.blocks['b-user-dialog'], 'confirm').callsFake(function(options) {
                    options.onConfirm.apply(options.callbackCtx);
                });

                regionWithDifferences
                    .setMod('checked', regionWithDifferences.isChecked() ? '' : 'yes')
                    .trigger('click');
                sandbox.clock.tick(100);
            });

            afterEach(function() {
                BEM.blocks['b-user-dialog'].confirm.restore();
            });

            it('Выбор региона меняется', function() {
                expect(regionWithDifferences.isChecked()).to.be.equal(!initialCheckboxState);
            });

            it('Иконка различий рядом с регионом удаляется', function() {
                expect(block.elem('contrast-value-hint').length).to.be.equal(0);
            });
        });
    });
});

