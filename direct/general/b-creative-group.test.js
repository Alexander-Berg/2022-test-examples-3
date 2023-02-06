describe('b-creative-group', function() {
    var block,
        sandbox,
        observable,
        selectedData = [{ id: 222, buisness_type: 'MMM' }],
        creativesData = [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
        groupStub = {
            group_id: '111',
            name: 'Тестовая группа',
            // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
            creatives_data: creativesData,
            //данные по первому креативу, нужные для отрисовки картинки
            width: 240,
            height: 400,
            alt_text: 'Bla',
            preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
            preview_scale: 0.5
        },
        getGroupStub = function(creativesData, selectedData) {
            return {
                group_id: '111',
                name: 'Тестовая группа',
                // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
                creatives_data: creativesData,
                selected_data: selectedData,
                //данные по первому креативу, нужные для отрисовки картинки
                width: 240,
                height: 400,
                alt_text: 'Bla',
                preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
                preview_scale: 0.5
            }
        };

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Проверка модификатора _state_', function() {
        afterEach(function() {
            block.destruct();
        });

        it('Если в данных переданных группе selected_data совпадает по длине с creatives_data, то группа отрисуется с _state_checked', function() {
            block = u.createBlock({
                block: 'b-creative-group',
                group: getGroupStub(creativesData, creativesData)
            }, { inject: true });

            expect(block).to.haveMod('state', 'checked');
        });

        it('Если в данных переданных группе selected_data меньше по длине чем creatives_data, то группа отрисуется с _state_indeterminate', function() {
            block = u.createBlock({
                block: 'b-creative-group',
                group: getGroupStub(creativesData, selectedData)
            }, { inject: true });

            expect(block).to.haveMod('state', 'indeterminate');
        });

        it('Если в данных переданных группе selected_data пустое, то группа отрисуется без state', function() {
            block = u.createBlock({
                block: 'b-creative-group',
                group: getGroupStub(creativesData, [])
            }, { inject: true });

            expect(block).not.to.haveMod('state');
        });
    });

    describe('Выбор креативов через попап креативов в группе', function() {
        beforeEach(function() {
            block = u.createBlock({
                block: 'b-creative-group',
                group: groupStub
            }, { inject: true });

            observable = new $.observable();

            sandbox.stub(BEM.DOM.blocks['b-modal-popup-decorator'], 'create2').callsFake(function() {
                return new ($.inherit($.observable, {
                    setPopupContent: function() {
                        return observable;
                    },
                    show: function() {},
                    hide: function() {}
                }))()
            })
        });

        afterEach(function() {
            block && block.destruct();
        });

        it('Если через попап не выбрали креативов - блок не должен иметь модификатора state', function() {
            block.findBlockInside('button2').domElem.click();

            observable.trigger('save', { selectedCreatives: [] });

            expect(block).to.not.haveMod('state');
        });
    });
});
