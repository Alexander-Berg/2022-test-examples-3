describe('b-crypta-interests-collection', function() {

    var block,
        sandbox,
        interestsStub = [
            {
                id: 2499000201,
                parent_id: 0,
                name: 'Транспорт',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000202,
                parent_id: 2499000201,
                name: 'Авто',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000203,
                parent_id: 2499000201,
                name: 'Самолеты',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000404,
                parent_id: 0,
                name: 'Техника',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000209,
                parent_id: 2499000404,
                name: 'Мобильные устройства',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000210,
                parent_id: 2499000209,
                name: 'Apple',
                description: '',
                type: 'interests'
            },
            {
                id: 2499001210,
                parent_id: 2499000209,
                name: 'Samsung',
                description: '',
                type: 'interests'
            },
            {
                id: 2499002210,
                parent_id: 2499000209,
                name: 'Остальные',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000250,
                parent_id: 2499000202,
                name: 'Легковые',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000251,
                parent_id: 2499000202,
                name: 'Внедорожники',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000252,
                parent_id: 2499000202,
                name: 'Пикапы',
                description: '',
                type: 'interests'
            },
            {
                id: 2499000253,
                parent_id: 2499000202,
                name: 'Кабриолеты',
                description: '',
                type: 'interests'
            },
        ];

    function createBlock(options) {
        options || (options = {});

        block = u.getInitedBlock({
            block: 'b-crypta-interests-collection',
            interests: interestsStub,
            items: options.items || []
        })
    }

    function destructBlock() {
        block.destruct && block.destruct();
    }

    before(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        sandbox.stub(u, 'consts')
            .withArgs('cryptaInterestsLimit').returns(10)
            .withArgs('cryptaInterestsSetLimit').returns(3);
    });

    after(function() {
        sandbox.restore();
    });

    describe('Инициализация пустой формы', function() {

        before(function() {
            createBlock();
        });

        after(function() {
            destructBlock();
        });

        it('Внутри одна форма', function() {
            expect(block.elem('item').length).to.be.eq(1);
        });

        it('Кнопка добавить набор видна', function() {
            expect(block).to.not.haveMod(block.elem('add-item'), 'hidden', 'yes');
        });

    });

    describe('Инициализация не пустой формы', function() {

        var sets = [
            {
                type: 'all',
                ids: [2499000210]
            },
            {
                type: 'all',
                ids: [2499000211]
            },
            {
                type: 'all',
                ids: [2499000212]
            }

        ];

        [1,3].forEach(function(countSets) {

            describe('Кол-во сетов ' + countSets, function() {
                before(function() {
                    createBlock({
                        items: sets.slice(0, countSets)
                    });
                });

                after(function() {
                    destructBlock();
                });

                it('Внутри есть ' + countSets +' формы(а)', function() {
                    expect(block.elem('item').length).to.be.eq(countSets);
                });

                it('Кнопка добавить набор видна', function() {
                    expect(block.hasMod(block.elem('add-item'), 'hidden', 'yes' )).to.be.eq(
                        countSets === 3
                    );
                });
            })
        });

    });

    describe('Поведение', function() {

        describe('При нажатии на кнопку добавляется набор', function() {

            before(function() {
                createBlock();
            });

            after(function() {
                destructBlock();
            });

            it('Сначала набор один', function() {
                expect(block.findElem('item').length).to.be.eq(1);
            })

            it('При нажатии добавляется еще один', function() {
                block.findBlockInside('add-item', 'button2').trigger('click');

                expect(block.findElem('item').length).to.be.eq(2);
            })
        });

        describe('При добавлении последнего набора, кнопка пропадает', function() {

            before(function() {
                createBlock({
                    items: [
                        {
                            type: 'all',
                            ids: [2499000210]
                        },
                        {
                            type: 'all',
                            ids: [2499000211]
                        }
                    ]
                });
            });

            after(function() {
                destructBlock();
            });

            it('Сначала кнопка видима', function() {
                expect(block).to.not.haveMod(block.elem('add-item'), 'hidden', 'yes');
            })

            it('После добавления набора - не видна', function() {
                block.findBlockInside('add-item', 'button2').trigger('click');

                expect(block).to.haveMod(block.elem('add-item'), 'hidden', 'yes');
            })
        });

    });

});
