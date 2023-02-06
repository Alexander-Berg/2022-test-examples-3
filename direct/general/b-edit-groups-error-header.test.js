describe('b-edit-groups-error-header', function() {
    var bemBlock,
        blockTree,
        groupsModels = [];

    function createBlock(groups) {
        blockTree = u.getDOMTree({
            block: 'b-edit-groups-error-header',
            groups: groups || []
        });

        $('body').append(blockTree);
        groups.forEach(function(group) {
            groupsModels.push(BEM.MODEL.create({ name: 'm-group', id: group.modelId }, group));
        })

        bemBlock = BEM.DOM.init(blockTree).bem('b-edit-groups-error-header');
    }

    afterEach(function() {
        bemBlock.destruct();
        groupsModels.forEach(function(model) {
            model.destruct();
        });
    });

    describe('1 группа adgroup_id с ошибкой в заголовке', function() {
        beforeEach(function() {
            createBlock([
                {
                    modelId: 'group-id',
                    adgroup_id: 'group-id',
                    banners: [],
                    errors: {
                        group_name: 1
                    }
                }
            ])
        });

        afterEach(function() {
            bemBlock.destruct();
        });

        it('Должен отрисоваться общий текст об ошибке', function() {
            expect(bemBlock.elem('message').text()).to.be.equal('Пожалуйста, заполните правильно необходимые поля');
        });

        it('Должен отрисоваться 1 элемент с текстом "№ adgroup_id"', function() {
            expect(bemBlock.elem('group-name').length).to.be.equal(1);
            expect(bemBlock.elem('group-name').text()).to.be.equal('№ group-id');
        });

        it('Ссылка с элемента должна вести на #Group-group-id', function() {
            expect(bemBlock.elem('group-link', 'type', 'title').length).to.be.equal(1);
            expect(bemBlock.elem('group-link', 'type', 'title').attr('href')).to.be.equal('#Group-group-id');
        });
    });

    describe('1 группа adgroup_id с ошибкой в свойствах группы', function() {
        beforeEach(function() {
            createBlock([
                {
                    modelId: 'group-id',
                    adgroup_id: 'group-id',
                    banners: [],
                    errors: {
                        phrases: 1
                    }
                }
            ])
        });

        it('Должен отрисоваться общий текст об ошибке', function() {
            expect(bemBlock.elem('message').text()).to.be.equal('Пожалуйста, заполните правильно необходимые поля');
        });

        it('Должен отрисоваться 1 элемент с текстом "№ adgroup_id"', function() {
            expect(bemBlock.elem('group-name').length).to.be.equal(1);
            expect(bemBlock.elem('group-name').text()).to.be.equal('№ group-id');
        });

        it('Ссылка с элемента должна вести на #Group-properties-group-id', function() {
            expect(bemBlock.elem('group-link', 'type', 'properties').length).to.be.equal(1);
            expect(bemBlock.elem('group-link', 'type', 'properties').attr('href')).to.be.equal('#Group-properties-group-id');
        });
    });

    describe('1 группа с двумя ошибками в баннерах', function() {
        beforeEach(function() {
            createBlock([
                {
                    modelId: 'group-id',
                    adgroup_id: 'group-id',
                    banners: [
                        { bid: 'banner-id-1', modelId: 'banner-id-1', errors: { 'title': 1 } },
                        { bid: 'banner-id-2', modelId: 'banner-id-2', errors: { 'title': 1 } }
                    ],
                    errors: {
                        banners: true
                    }
                }
            ])
        });


        it('Должен отрисоваться 2 элемент с текстом "№ modelId"', function() {
            expect(bemBlock.elem('banner-name').length).to.be.equal(2);
            expect($(bemBlock.elem('banner-name').get(0)).text()).to.be.equal('№ M-banner-id-1');
            expect($(bemBlock.elem('banner-name').get(1)).text()).to.be.equal('№ M-banner-id-2');
        });

        it('Ссылки с этих двух элементов должны вести на #Banner-group-id-banner-id-1 и #Banner-group-id-banner-id-2', function() {
            expect(bemBlock.elem('banner-link').length).to.be.equal(2);
            expect($(bemBlock.elem('banner-link').get(0)).attr('href')).to.be.equal('#Banner-group-id-banner-id-1');
            expect($(bemBlock.elem('banner-link').get(1)).attr('href')).to.be.equal('#Banner-group-id-banner-id-2');
        });
    });

    describe('Блок должен реагировать на событие invalid канала multiedit-errors', function() {
        beforeEach(function() {
            createBlock([
                {
                    modelId: 'group-id',
                    adgroup_id: 'group-id',
                    banners: [
                        { bid: 'banner-id-1', modelId: 'banner-id-1' },
                        { bid: 'banner-id-2', modelId: 'banner-id-2' }
                    ],
                    errors: {}
                }
            ]);

        });


        it('Если пришло bannersErrors - должен отрисовать ошибки баннеров', function() {
            bemBlock.channel('multiedit-errors').trigger('invalid', {
                groupsErrors: [],
                titleErrors: [],
                bannersErrors: [
                    { banner: { modelId: 'banner-id-1' }, group:  { modelId: 'group-id-1' } },
                    { banner: { modelId: 'banner-id-2' }, group:  { modelId: 'group-id-2' } }
                ]
            });

            var names = bemBlock.elem('banner-name');

            names && expect(names.length).to.be.equal(2) &&
                expect($(names.get(0)).text()).to.be.equal('№ M-banner-id-1') &&
                expect($(names.get(1)).text()).to.be.equal('№ M-banner-id-2');
        });

        it('Если пришло titleErrors - блок должен отрисовать ошибки заголовков групп', function() {
            bemBlock.channel('multiedit-errors').trigger('invalid', {
                groupsErrors: [],
                titleErrors: [1],
                bannersErrors: []
            });

            expect(bemBlock.elem('group-link', 'type', 'title').length).to.be.equal(1);
        });

        it('Если пришло groupsErrors - блок должен отрисовать ошибки свойств группы', function() {
            bemBlock.channel('multiedit-errors').trigger('invalid', {
                groupsErrors: [1],
                titleErrors: [],
                bannersErrors: []
            });

            expect(bemBlock.elem('group-link', 'type', 'properties').length).to.be.equal(1);
        });
    })
});
