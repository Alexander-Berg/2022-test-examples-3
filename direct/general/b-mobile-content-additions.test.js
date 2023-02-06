describe('b-mobile-content-additions', function() {
    var groupDM, bannerDM, model, time,
        mobileContentStub = {
            name: 'myApp',
            rating: 4.5,
            icon_url: 'ico',
            rating_votes: 1000,
            prices: {
                download: {
                    price: 10,
                    price_currency: 'RUB'
                }
            }
        };

    beforeEach(function() {
        u.stubCurrencies();

        groupDM = BEM.MODEL.create({ name: 'dm-mobile-content-group', id: 'gr1' }, {
            modelId: 'gr1',
            mobile_content: {},
            banners: [{ modelId: 'bnr1', title: 'test' }]
        });

        groupDM.init();

        bannerDM = groupDM.get('banners').getByIndex(0);

        model = BEM.MODEL.create({ name: 'b-mobile-content-additions', id: 'bnr1' }, {
            ratingDisabled: true,
            priceDisabled: true,
            iconDisabled: true,
            ratingVotesDisabled: true,
            ratingVotesItemDisabled: true,
            isCorruptedData: false,
            attrs: []
        });

        time = sinon.useFakeTimers();

        model.init();
    });

    afterEach(function() {
        bannerDM.destruct();
        model.destruct();
        groupDM.destruct();

        time.restore();

        u.restoreCurrencies();
    });

    describe('состояние чекбоксов при измении mobile_content в DM группы', function() {
        beforeEach(function() {
            groupDM.get('mobile_content').update(mobileContentStub);

            time.tick(10);
        });

        afterEach(function() {
            groupDM.get('mobile_content').clear();
        });

        it('должен активировать рейтинг', function() {
            expect(model.get('ratingDisabled')).to.be.equal(false);
        });

        it('должен активировать цену', function() {
            expect(model.get('priceDisabled')).to.be.equal(false);
        });

        it('должен активировать иконку', function() {
            expect(model.get('iconDisabled')).to.be.equal(false);
        });

        it('должен активировать количество оценок, если рейтинг активен', function() {
            expect(model.get('ratingVotesItemDisabled')).to.be.equal(false);
        });

        it('не должен активировать количество оценок, если рейтинг неактивен', function() {
            model.set('attrs', ['price', 'rating_votes', 'icon']);
            expect(model.get('ratingVotesItemDisabled')).to.be.equal(true);
        });
    });

    describe('значение reflected_attrs в DM', function() {
        it('должно выставляться из для всех параметров', function() {
            groupDM.get('mobile_content').update(mobileContentStub);
            time.tick(10);

            model.set('attrs', ['rating', 'price', 'icon', 'rating_votes']);

            time.tick(10);
            //slice нужен, чтобы привести массив из модели к нормальному массиву: тогда работает сравнение по eql
            expect(bannerDM.get('reflected_attrs').slice()).to.be.eql(['rating', 'price', 'icon', 'rating_votes']);
        });
    });

    describe('состояние чекбоксов при изменении reflected_attrs в DM баннера', function() {
        beforeEach(function() {
            groupDM.get('mobile_content').update(mobileContentStub);
            time.tick(100);

            bannerDM.set('reflected_attrs', ['rating', 'price', 'icon', 'rating_votes']);
            time.tick(10);
        });

        it('должно выставлять рейтинг', function() {
            expect(model.get('attrs')).to.include('rating');
        });

        it('должно выставлять иконку', function() {
            expect(model.get('attrs')).to.include('icon');
        });

        it('должно выставлять цену', function() {
            expect(model.get('attrs')).to.include('price');
        });

        it('должно выставлять количество отзывов', function() {
            expect(model.get('attrs')).to.include('rating_votes');
        });

        it('не должен выставлять количество отзывов, если ретинг неактивен', function() {
            time.tick(10);
            bannerDM.set('reflected_attrs', ['rating_votes']);
            time.tick(10);

            expect(model.get('attrs')).to.not.include('rating_votes');
        });
    });

    describe('в интерфейсе', function() {
        var block,
            menu,
            items;

        beforeEach(function() {
            var blockTree = u.getDOMTree($.extend({
                block: 'b-mobile-content-additions',
                banner: u._.pick(bannerDM.toJSON(), ['modelId', 'image', 'image_name']),
                enabledAttrs: [],
                showAttrs: []
            }));

            block = BEM.DOM.init(blockTree.appendTo('body')).bem('b-mobile-content-additions');
            menu = block.findBlockInside('select2').menu;
            items = menu.getItems('disabled', 'yes').map(function(item) {
                return menu.elemParams(item).val;
            });
        });

        afterEach(function() {
            block && block.destruct();
        });

        describe('в дефолтном состоянии', function() {
            it('должен быть неактивный рейтинг', function() {
                expect(items).to.include('rating');
            });

            it('должена быть неактивна цена', function() {
                expect(items).to.include('price');
            });

            it('должена быть неактивна иконка', function() {
                expect(items).to.include('icon');
            });

            it('должено быть неактивно количество оценок', function() {
                expect(items).to.include('rating_votes');
            });
        });

        describe('при наличии приложения', function() {
            beforeEach(function() {
                groupDM.set('mobile_content', mobileContentStub);

                time.tick(10);

                items = menu.getItems('disabled', 'yes').map(function(item) {
                    return menu.elemParams(item).val;
                });

                time.tick(10);
            });

            it('должен активировать рейтинг', function() {
                expect(items).to.not.include('rating');
            });

            it('должен отображать рейтинг', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('rating');
            });

            it('должен активировать цену', function() {
                expect(items).to.not.include('price');
            });

            it('должен активировать иконку', function() {
                expect(items).to.not.include('icon');
            });

            it('должен отображать иконку', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('icon');
            });
        });

        describe('при изменении своей модели', function() {
            beforeEach(function() {
                groupDM.set('mobile_content', mobileContentStub);

                time.tick(10);

                block.model.update({
                    attrs: ['rating', 'price', 'icon', 'rating_votes']
                });
            });

            it('должен отображать рейтинг', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('rating');
            });

            it('должен отображать цену', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('price');
            });

            it('должен отображать иконку', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('icon');

            });

            it('должен отображать количество оценок', function() {
                expect(block.findBlockInside('select2').getVal()).to.include('rating_votes');
            });
        });

        describe('при неполных данных по приложению', function() {
            beforeEach(function() {
                groupDM.set('mobile_content', $.extend(mobileContentStub, { rating: 0 }));

                time.tick(10);
            });

            it('должен отображать предупреждение', function() {
                expect(block.hasMod(block.findElem('corrupted-data-warning'), 'visible', 'yes')).to.be.equal(true);
            });
        });

    });

});
