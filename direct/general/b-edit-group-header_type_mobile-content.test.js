describe('b-edit-group-header_type_mobile-content', function() {
    var block,
        ctx = {
            block: 'b-edit-group-header',
            mods: { type: 'mobile-content' },
            group_name: 'Тестовая группа',
            groupNumber: '12345',
            errorPath: '',
            text: 'Тестовый текст'
        },
        js = {
            modelParams: {
                id: 1644061276,
                name: "dm-mobile-content-group"
            },
            banners_quantity: 3
        },
        modelData = {
            adgroup_id: 1644061276,
            banners_quantity: js.banners_quantity,
            group_name: 'Другое имя группы',
            adgroup_type: 'mobile_content',
            banners: [
                {
                    bid: 1,
                    title: '1',
                    isNewBanner: true
                }, {
                    bid: 2,
                    title: '2'
                }
            ],
            store_content_href: 'http://appstore.com/'
        },
        sandbox,
        model;

    beforeEach(function() {
        u.stubCurrencies();
    });

    afterEach(function() {
        u.restoreCurrencies();
    });

    describe('DOM', function() {
        before(function () {
            block = u.createBlock(ctx);
        });

        after(function() {
            block.destruct();
        });

        it('Есть блок со ссылкой на стор', function() {
            expect(block).to.haveElem('store-href-wrap');
        });

        it('По умолчанию блок со ссылкой на стор пустой', function() {
            expect(block.elem('store-href-wrap').html()).to.equal('');
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

    describe('Метод setGroupModelParams', function() {
        before(function() {
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

        after(function() {
            block.destruct();
            model.destruct();
        });

        it('В блоке со ссылкой на стор ставится ссылка из модели', function() {
            expect(block.findBlockInside('b-edit-group-mobile-content-href').domElem.text())
                .to.equal('http://appstore.com/');
        });

        it('В блоке со ссылкой на стор - рид-онли', function() {
            expect(block.findBlockInside('b-edit-group-mobile-content-href')).to.haveMod('read-only', 'yes');
        });
    });
});
