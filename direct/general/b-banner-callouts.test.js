describe('b-banner-callouts', function() {
    var block,
        callouts = [
            {
                "callout_text": "Наличие",
                "status_moderate": "Yes",
                "additions_item_id": "81405"
            },
            {
                "additions_item_id": "81407",
                "status_moderate": "Yes",
                "callout_text": "Фото"
            },
            {
                "callout_text": "Цена",
                "additions_item_id": "81408",
                "status_moderate": "Yes"
            },
            {
                "status_moderate": "Yes",
                "additions_item_id": "81409",
                "callout_text": "Описание"
            },
            {
                "status_moderate": "Yes",
                "additions_item_id": "81415",
                "callout_text": "Доставка"
            }
        ],
        ctx = {
            block: 'b-banner-callouts',
            modelParams: {
                id: "2107846689",
                name: "dm-dynamic-banner",
                parentId: 1481767116,
                parentName: "dm-dynamic-group"
            },
            banner: {
                "title": "{Динамический заголовок}",
                "bid": 2107846689,
                "callouts": callouts
            }
        },
        sandbox,
        bannerModel;

    beforeEach(function() {
        block = u.createBlock(ctx);
        bannerModel = BEM.MODEL.create(ctx.modelParams, { callouts: callouts });
    });

    afterEach(function() {
        block.destruct();
    });

    describe('Смена модели уточнений', function() {
        var newCallout = {
            "status_moderate": "Yes",
            "additions_item_id": "76667",
            "callout_text": "Учкудук-три колодца"
        };

        beforeEach(function() {
            var nc = callouts.concat([newCallout]);
            bannerModel.set('callouts', nc);
        });

        it('Если есть уточнения - текст кнопки "Изменить"', function() {
            expect(block._getSwitcher().getText()).to.equal('Изменить');
        });

        it('Если нет уточнений - текст кнопки "Добавить"', function() {
            bannerModel.set('callouts', []);
            expect(block._getSwitcher().getText()).to.equal('Добавить');
        });

        it('При изменении списка уточнений перерисовывается DOM', function() {
            expect(block.elem('outcome').text().indexOf('Учкудук')).not.to.equal(-1);
        })
    })
});
