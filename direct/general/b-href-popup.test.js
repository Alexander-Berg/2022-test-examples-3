describe('b-href-popup', function() {
    var ctx = {
        block: 'b-href-popup'
    },
        clock,
        block,
        modal;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
    });

    afterEach(function() {
        block && block.destruct();
        clock.restore();
    });

    function createBlock(cb, ctx, options) {
        block = BEM.DOM.blocks['b-href-popup'].create(cb, ctx, options)
    }

    function getInnerBlock() {
        return block.getPopup().findBlockByInterfaceInside('i-modal-popup-inner-block-interface');
    }

    describe('Создание попапа: ', function() {
        it('Попап создается без модификатора при отсутствии опций', function() {
            createBlock();
            modal = getInnerBlock();

            expect(modal).not.to.haveMod('type');
        });

        it('Попап создается с модификатором mobile при isMobileCampaign в опциях', function() {
            createBlock(undefined, undefined, { isMobileCampaign: true });
            modal = getInnerBlock();

            expect(modal).to.haveMod('type', 'mobile');
        });

        it('Попап создается без модификатора mobile при isMobileCampaign: false в опциях', function() {
            createBlock(undefined, undefined, { isMobileCampaign: false });
            modal = getInnerBlock();

            expect(modal).not.to.haveMod('type');
        });

        it('Правильно устанавливается текст для кнопки Оk', function() {
            createBlock(undefined, undefined, { btnOkText: 'test-ok' });
            modal = getInnerBlock();

            expect(modal._btnSave.getText()).to.be.equal('test-ok');
        });

        it('Правильно устанавливается текст для кнопки Отмена', function() {
            createBlock(undefined, undefined, { btnCancelText: 'test-cancel' });
            modal = getInnerBlock();

            expect(modal._btnCancel.getText()).to.be.equal('test-cancel');
        });
    });

    describe('Работа попапа', function() {
        beforeEach(function() {
            createBlock(function() { console.log('I am triggered'); });
            modal = getInnerBlock();
        });

        it('Изначально кнопка активна', function() {
            expect(modal._btnSave).not.to.haveMod('disabled')
        });

        describe('При обновлении ссылки, ', function() {
            it('если ссылка не валидная - кнопка не активна', function() {
                modal._href.findBlockOn('href', 'input').val('lalala');

                clock.tick(100);
                modal._href.trigger('state:changed', { isReady: false });

                expect(modal._btnSave).to.haveMod('disabled', 'yes');
            });

            it('если ссылка валидная - кнопка активна', function() {
                modal._href.findBlockOn('href', 'input').val('www.yandex.ru');

                clock.tick(100);
                modal._href.trigger('state:changed', { isReady: true });

                expect(modal._btnSave).not.to.haveMod('disabled');
            })
        });

        describe('При нажатии на кнопку save, ', function() {
            it ('триггерится событие save', function() {
                sinon.spy(modal, 'trigger');

                modal._btnSave.trigger('click');

                expect(modal.trigger.calledWith('save')).to.be.true;
            });

            it ('модальное окно прячется', function() {
                sinon.spy(block, 'hide');

                modal._btnSave.trigger('click');

                expect(block.hide.called).to.be.true;
            });
        });

        describe('При нажатии на кнопку cancel, ', function() {
            it ('триггерится событие cancel', function() {
                sinon.spy(modal, 'trigger');

                modal._btnCancel.trigger('click');

                expect(modal.trigger.calledWith('cancel')).to.be.true;
            });

            it ('модальное окно прячется', function() {
                sinon.spy(block, 'hide');

                modal._btnCancel.trigger('click');

                expect(block.hide.called).to.be.true;
            });
        })
    })
});
