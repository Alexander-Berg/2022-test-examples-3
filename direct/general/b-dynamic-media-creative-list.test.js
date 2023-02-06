describe('b-dynamic-media-creative-list', function() {
    var block,
        sandbox,
        getIdsStub = function(startId, endId) {
            var i,
                stub = [];

            for (i = startId; i <= endId; i++) {
                stub.push({
                    id: i,
                    business_type: 'test',
                    creative_group_id: 1
                })
            }

            return stub;
        },
        getItemsStub = function(startId, endId) {
            var i,
                stub = [];

            for (i = startId; i <= endId; i++) {
                stub.push({
                    business_type: 'test',
                    creative_id: i,
                    creative_group_id: 1,
                    name: 'name',
                    width: 100,
                    height: 100,
                    preview_scale: 1,
                    alt_text: 'alt_text',
                    href: 'data.href',
                    geo_names: 'data.geo_names',
                    status_moderate: 'data.status_moderate',
                    preview_url: 'data.preview_url',
                    rejection_reason_ids: [],
                    campaigns: [],
                    bs_template_name: 'data.bs_template_name'
                })
            }

            return stub;
        };

    describe('b-dynamic-media-creative-list_type_choose-multiple', function() {
        function createBlock(data) {
            return u.createBlock({
                block: 'b-dynamic-media-creative-list',
                mods: { type: 'choose-multiple' },
                mix: { block: 'b-creative-wrapper', elem: 'items' },
                otherIds: data.otherIds,
                selectedItemsIds: data.selectedItemsIds,
                controlsType: data.controlsType,
                items: data.items
            }, { inject: true });
        }
        beforeEach(function() {
            sandbox = sinon.sandbox.create({
                useFakeTimers: true
            });

            var constsStub = sandbox.stub(u, 'consts');

            constsStub.withArgs('SCRIPT_OBJECT').returns({
                host: 'direct.yandex.ru'
            });
        });

        afterEach(function() {
            sandbox.restore();
            block && block.destruct();
        });

        describe('Проверяем прокидывание параметров при первоначальной отрисовке блока', function() {
            [{ name: 'controlsType', value: 'add' }].forEach(function(param) {
                it('Если блоку были переданы параметр ' + param.name + ' то он должнен передаться в items-wrapper при первоначальное отрисовке блока', function() {
                    var applySpy = sandbox.spy(BEMHTML, 'apply');

                    block = createBlock({
                        otherIds: getIdsStub(2, 5),
                        selectedItemsIds: getIdsStub(1, 5),
                        items: getItemsStub(1, 1),
                        controlsType: 'add'
                    });
                    //первый apply - отрисовка блока без контента, второй - отрисовка items-wrapper'а
                    expect(applySpy.args[1][0][param.name]).to.be.eql(param.value);
                });
            });
        });

        it('Метод getSelectedIds должен возвращать все id, переданные в selectedItemsIds при инициализации', function() {
            var selectedItemsIds = getIdsStub(1, 5);

            block = createBlock({
                otherIds: getIdsStub(3, 5),
                selectedItemsIds: selectedItemsIds,
                items: getItemsStub(1, 2)
            });

            expect(block.getSelectedIds()).to.be.eql(selectedItemsIds);
        });

        it('Отрисованные элементы, id которых были переданы в selectedItemsIds должны отрисоваться выбранными', function() {
            block = createBlock({
                otherIds: getIdsStub(3, 5),
                selectedItemsIds: getIdsStub(1, 5),
                items: getItemsStub(1, 2)
            });

            block.findElem('item-selector').map(function(i, elem) {
                expect(block.findBlockOn($(elem), 'checkbox')).to.haveMod('checked', 'yes');
            });
        });

        it('Отрисованные элементы, id которых не были переданы в selectedItemsIds должны отрисоваться невыбранными', function() {
            block = createBlock({
                otherIds: getIdsStub(3, 5),
                selectedItemsIds: [],
                items: getItemsStub(1, 2)
            });

            block.findElem('item-selector').map(function(i, elem) {
                expect(block.findBlockOn($(elem), 'checkbox')).not.to.haveMod('checked');
            });
        });
    })
});
