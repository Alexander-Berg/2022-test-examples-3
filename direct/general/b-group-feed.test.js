describe('b-group-feed', function() {
    var ctx = {
            block: 'b-group-feed',
            groupId: '1',
            selectedFeedId: '0',
            feeds: [
                {
                    'url': 'http://chaiport.ru/yandexmarket/472aef68-4be9-4d0e-b268-5837f570d6f6.xml',
                    'categories': [
                        {
                            'name': 'Чай',
                            'category_id': '2',
                            'path': [],
                            'is_deleted': 0,
                            'parent_category_id': '0'
                        },
                        {
                            'path': [3],
                            'category_id': '18',
                            'name': 'Заварочные чайники',
                            'parent_category_id': '15',
                            'is_deleted': 0
                        }
                    ],
                    'feed_type': 'YandexMarket',
                    'feed_id': 3238,
                    'source': 'url',
                    'name': 'Первый фид'
                },
                {
                    'url': 'http://chaiport.ru/yandexmarket/472aef68-4be9-4d0e-b268-5837f570d6f6.xml',
                    'categories': [
                        {
                            'name': 'Чай',
                            'category_id': '2',
                            'path': [],
                            'is_deleted': 0,
                            'parent_category_id': '0'
                        },
                        {
                            'path': [3],
                            'category_id': '18',
                            'name': 'Заварочные чайники',
                            'parent_category_id': '15',
                            'is_deleted': 0
                        }
                    ],
                    'feed_type': 'YandexMarket',
                    'feed_id': 3239,
                    'source': 'url',
                    'name': 'Второй фид'
                }
            ],
            disabled: false,
            ulogin: ''
        },
        block,
        chooser,
        sandbox;

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
        sandbox.stub(u, 'consts').withArgs('SCRIPT_OBJECT').returns(u.getScriptObjectForStub());
        block = u.createBlock(ctx);
        chooser = block.findBlockOn('chooser', 'b-chooser');
    });

    afterEach(function() {
        sandbox.restore();
        block.destruct();
    });

    describe('События на чузере', function() {
        it('При изменении на чузере триггерится событие change на блоке', function() {
            sandbox.spy(block, 'trigger');
            chooser.check('3239');

            expect(block.trigger.calledWith('change', {
                prev_feed_id: '',
                feed_id: '3239'
            })).to.equal(true);
        });

        it('Метод select изменяет состояние чузера', function() {
            block.select('3239');

            expect(chooser.getSelected().name).to.equal('3239');
        });
    });

    describe('Состояние стрелочки - поле модели arrowDirection', function() {
        [
            {
                action: 'show',
                state: 'открыт',
                modelState: 'up'
            },
            {
                action: 'hide',
                state: 'закрыт',
                modelState: 'down'
            }
        ].forEach(function(state) {
            it('Если попап ' + state.state + ', то состояние модели - ' + state.modelState, function() {
                block._dropdown.trigger(state.action);

                sandbox.clock.tick(100);

                expect(block.model.get('arrowDirection')).to.equal(state.modelState);
            });
        });
    });

    describe('HTML', function() {
        describe('Если в блоке есть фиды', function() {
            it('В блоке есть dropdown', function() {
                expect(block).to.haveElem('dropdown');
            });

            it('В блоке есть b-chooser', function() {
                expect(block).to.haveElem('chooser');
            });

            it('В блоке b-chooser есть 2 элемента с правильными name', function() {
                var length = chooser.elem('item').length;

                expect(length).to.equal(2);
                expect(chooser.elem('item', 'name', '3238').length).not.to.equal(0);
                expect(chooser.elem('item', 'name', '3239').length).not.to.equal(0);
            })
        });

        it('Если в блоке нет фидов, то нет блока dropdown', function() {
            block = u.createBlock({
                block: 'b-group-feed',
                groupId: '1',
                selectedFeedId: '0',
                feeds:[],
                disabled: false
            });

            expect(block).not.to.haveElem('dropdown');
        });

        describe('Если блок не задизейблен', function() {
            it('Кнопка дропдауна не задизейблена', function() {
                expect(block._dropdown.findBlockInside('button')).not.to.haveMod('disabled');
            });

            it('Кнопка ДОБАВИТЬ cуществует', function() {
                expect(block).to.haveElem('add-feed');
            })
        });

        describe('Если блок задизейблен', function() {
            beforeEach(function() {
                block = u.createBlock(u._.assign(ctx, { disabled: true }));
            });

            it('Дропдаун задизейблен', function() {
                expect(block._dropdown.findBlockInside('button')).to.haveMod('disabled', 'yes');
            });

            it('Кнопка ДОБАВИТЬ не существует', function() {
                expect(block).not.to.haveElem('add-feed');
            });
        });
    });

    describe('Работа поиска интересов', function() {
        it('При задании текста элементы без такой подстроки получают модификатор visibility_hidden', function() {
            var elemsNoLetter = [
                chooser.elem('item', 'name', '3239')  //Второй фид
            ];
            chooser.search('П');

            sandbox.clock.tick(100);

            var allInvisible = elemsNoLetter.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == 'hidden';
            });

            expect(allInvisible).to.be.true;
        });

        it('При очищении строки поиска все элементы списка становятся видимы', function() {
            var elems = [
                chooser.elem('item', 'name', '3238'),
                chooser.elem('item', 'name', '3239')
            ];

            chooser.search('');

            sandbox.clock.tick(100);

            var allVisible = elems.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == '';
            });

            expect(allVisible).to.be.true;
        });

        it('Если заданной подстроки нет ни в 1 элементе, то все элементы становятся невидимы', function() {
            var elems = [
                chooser.elem('item', 'name', '3238'),  //Первый фид
                chooser.elem('item', 'name', '3239') //Второй фид
            ];

            chooser.search('У');

            sandbox.clock.tick(100);

            var allInvisible = elems.every(function(elem) {
                return chooser.getMod(elem, 'visibility') == 'hidden';
            });

            expect(allInvisible).to.be.true;
        });
    });

});
