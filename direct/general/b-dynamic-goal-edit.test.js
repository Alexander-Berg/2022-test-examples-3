describe('b-dynamic-goal-edit', function() {
    var block,
        blockTree,
        goal = {
            kind:  'exact',
            value:  [{ value: 'url' }],
            type:  'URL'
        },
        lim = 100,
        createBlock = function() {
            blockTree = u.getDOMTree($.extend({
                block: 'b-dynamic-goal-edit'
            }, {
                parentId: 19742,
                goal: BEM.MODEL.create({ name: 'vm-dynamic-goal' }, goal)
            }));

            block = BEM.DOM.init(blockTree.appendTo('body')).bem('b-dynamic-goal-edit');
        },
        getSelectOptions = function(type) {
            var select = block.findBlockOn(type, 'select'),
                options = select.findElem('option');

            return Array.prototype.slice.apply(options).map(function(elem) {
                return $(elem).val();
            }, this);
        },
        cleanUp = function() {
            block && block.destruct();
        },
        selectData = [
            {
                type: 'URL',
                title: 'Ссылка на товар',
                optList:['exact', 'not_exact']
            },
            {
                type: 'URL_prodlist',
                title: 'URL списка предложений',
                optList: ['equals', 'not_equals']
            },
            {
                type: 'title',
                title: 'Заголовок страницы',
                optList: ['exact', 'not_exact']
            },
            {
                type: 'content',
                title: 'Контент страницы',
                optList: ['exact', 'not_exact']
            },
            {
                type: 'domain',
                title: 'Домен',
                optList: ['exact', 'not_exact']
            }
        ];

    describe('Валидация: для модели с дублирующем набором правил ', function() {
        it('вернет ошибку о дублирующем условии', function() {
            var checkResult = {};
            createBlock();
            block.model.set('value',[
                { value: 'title' },
                { value: 'title' }
            ]);
            block.model.get('value').forEach(function(model) {
                checkResult = u._.extend(checkResult, model.validate('isUnique'));
            });
            expect(checkResult.errorFields.indexOf('isUnique') >= 0).to.be.equal(true);
        });
    });


    describe('Селект типа (type)', function() {
        beforeEach(function() {
            createBlock();
            this.typeOptions = getSelectOptions('type');
        });

        it('состоит из 5 опции', function() {
            expect(this.typeOptions.length).to.be.equal(5);
        });

        ['URL', 'URL_prodlist', 'title', 'content', 'domain'].forEach(function(opt) {
            it('содержит опцию ' + opt, function() {
                expect((this.typeOptions.indexOf(opt) >= 0)).to.be.equal(true);
            });
        });

        afterEach(cleanUp);
    });

    describe('Селект вида (kind)', function() {
        beforeEach(function() {
            createBlock();
            this.typeSelect = block.findBlockOn('type', 'select');
            this.kindSelect = block.findBlockOn('kind', 'select');
        });

        describe('связан с селектом типа', function() {
            beforeEach(function() {
                this.spy = sinon.spy(this.kindSelect, 'setOptions');
                this.typeSelect.val('URL_prodlist');
            });

            it('по изменению одного, обновляется другой', function() {
                expect(this.spy.called).to.be.equal(true);
            });
        });

        selectData.forEach(function(rule) {
            describe('при типе нацеливания "'+ rule.title +'"', function() {
                var otpLength = rule.optList.length;

                beforeEach(function() {
                    this.typeSelect.val(rule.type);
                    this.kindsOptions = getSelectOptions('kind');
                });

                it('состоит из '+ otpLength +' опции', function() {
                    expect(this.kindsOptions.length).to.be.equal(otpLength);
                });

                rule.optList.forEach(function(opt) {
                    it('содержит опцию ' + opt, function() {
                        expect((this.kindsOptions.indexOf(opt) >= 0)).to.be.equal(true);
                    });
                });
            });
        });

        afterEach(cleanUp);
    });

});
