describe('b-modal-popup-decorator', function() {
    describe('Установка опциональных модификаторов блока', function() {
        describe('Модификатор has-close', function() {
            var block;

            afterEach(function() {
                block.destruct();
            });

            it('По умолчанию модификатор ставится в занчение yes', function() {
                block = BEM.DOM.blocks['b-modal-popup-decorator'].create();

                expect(block.getPopup()).to.haveMod('has-close', 'yes');
            });

            it('При параметре hasClose: true модификатор ставится в занчение yes', function() {
                block = BEM.DOM.blocks['b-modal-popup-decorator'].create(undefined, { hasClose: true });

                expect(block.getPopup()).to.haveMod('has-close', 'yes');
            });

            it('При параметре hasClose: false модификатор не ставится', function() {
                block = BEM.DOM.blocks['b-modal-popup-decorator'].create(undefined, { hasClose: false });

                expect(block.getPopup()).not.to.haveMod('has-close');
            })
        });
    });
});
