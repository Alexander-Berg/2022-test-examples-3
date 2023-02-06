describe('b-segments-group_type_social-demo', function() {

    var block,
        sandbox,
        defaultResponse = {
            success: 1,
            result: [
                {
                    id: 2499000001,
                    parent_id: 0,
                    name: 'Мужчины',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000002,
                    parent_id: 0,
                    name: 'Женщины',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000003,
                    parent_id: 0,
                    name: 'до 18',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000004,
                    parent_id: 0,
                    name: '18–24',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000009,
                    parent_id: 0,
                    name: 'Низкий',
                    description: '',
                    type: 'social_demo'
                },
                {
                    id: 2499000010,
                    parent_id: 0,
                    name: 'Средний',
                    description: '',
                    type: 'social_demo'
                }
            ]
        },
        stubCryptaRequest = function(action, response) {
            sandbox.stub(BEM.blocks['i-web-api-request'].crypta, action)
                   .callsFake(function() {
                       return Promise.resolve(response);
                   });
        };

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-segments-group',
            mods: Object.assign({}, options.mods, {
                type: 'social-demo'
            }),
            groups: options.groups
        });
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    describe('js', function() {

        before(function() {
            sandbox = sinon.sandbox.create({ useFakeTimers: true });

            stubCryptaRequest('getCryptaSegments', defaultResponse);

            window.CONSTS.cryptaSocDemMapping = {
                gender: [2499000001, 2499000002],
                age: [2499000003, 2499000004, 2499000005, 2499000006, 2499000007, 2499000008],
                finance: [2499000009, 2499000010, 2499000011, 2499000012]
            };
        });

        after(function() {
            sandbox.restore();
        });

        describe('Без open_yes и без данных', function() {

            beforeEach(function() {
                createBlock();
                return new Promise(function(resolve, reject) {
                    block.on('ready', function() { resolve(); });
                });
            });

            afterEach(function() {
                destructBlock();
            });

            it('Есть заголовок', function() {
                expect(block.findBlockInside('b-expander').elem('title').text()).to.be.eq('Социально-демографический профиль');
            });

            it('Нет формы', function() {
                expect(block).to.not.haveElem('form');
            });

            it('После нажатия на заголовок появляется форма', function() {
                block.findBlockInside('b-expander').elem('title').click();
                expect(block).to.haveElem('form');
            });

            it('Свертка пустая', function() {
                expect(block.findBlockInside('b-expander').elem('subtitle').text()).to.be.eq('');
            });

        });

        describe.skip('С open_yes', function() {

            beforeEach(function() {
                createBlock({
                    mods: {
                        open: 'yes'
                    }
                });
            });

            afterEach(function() {
                destructBlock();
            });

            it('Есть заголовок', function() {
                expect(block.findBlockInside('b-expander').elem('title').text()).to.be.eq('Соцдем');
            });

            it('Есть форма выбора сегментов', function() {
                expect(block).to.haveElem('form');
            });

        });

    });

});
