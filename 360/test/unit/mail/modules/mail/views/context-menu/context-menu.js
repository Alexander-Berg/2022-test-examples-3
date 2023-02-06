describe('Daria.ContextMenu', function() {

    it('Класс меню присутствует и вызывается', function() {
        new Daria.ContextMenu({
            items: [{}]
        });

        expect(true).ok;
    });

    describe('Кидает ошибку при некорректных параметрах ->', function() {

        it('если передать пустой массив элементов', function() {
            expect(function() {
                new Daria.ContextMenu({
                    items: []
                });
            }).to.throw('Parameter "items" must contain at least 1 element');
        });

    });

    describe('Открывает/Закрывает меню ->', function() {

        beforeEach(function() {

            this._menu = new Daria.ContextMenu({
                items: [
                    {
                        text: 'Hello',
                        index: 1,
                        addClass: 'context-menu-item-1'
                    }
                ],
                onCancel: this._onCancelStub = sinon.stub(),
                onClose: this._onCloseStub = sinon.stub()
            });

            if (!window.onCancel) {
                window.onCancel = this._onCancelStub;
            } else {
                window.onCancel2 = this._onCancelStub;
            }

        });

        it('isOpened() соответствует состоянию', function() {
            this._menu.show({
                x: 0,
                y: 0
            });

            expect(Daria.ContextMenu.isOpened()).to.be.equal(true);
        });

        it('Появляется в DOMе при показе', function() {
            this._menu.show({
                x: 0,
                y: 0
            });

            expect(this._menu._$menu.closest('BODY').length).to.be.ok;
        });

        describe('Закрытие ->', function() {

            beforeEach(function() {
                this._menu.show({
                    x: 0,
                    y: 0
                });
            });

            it('Вызывается "onCancel" и "onClose" при закрытии без совершеного действия', function() {
                Daria.ContextMenu.close();

                expect(this._onCancelStub.calledOnce).to.be.ok;
                expect(this._onCloseStub.calledOnce).to.be.ok;
            });

            it('Закрывается кликом по элементу', function() {
                $('.context-menu-item-1').click();

                expect(Daria.ContextMenu.isOpened()).to.be.equal(false);
            });

            it('При клики на пункт меню вызывается только "onClose"', function() {
                $('.context-menu-item-1').click();

                expect(this._onCancelStub.notCalled).to.be.ok;
                expect(this._onCloseStub.calledOnce).to.be.ok;
            });
        });

    });

    describe('Построение меню ->', function() {

        beforeEach(function() {

            this._menu = new Daria.ContextMenu({
                items: [
                    {
                        text: 'Hello2',
                        index: 2,
                        addClass: 'element2',
                        metrika: 'text-of-metrika'
                    },
                    {
                        text: 'Hello1',
                        index: 1,
                        addClass: 'element1',
                        customIcon: '<span class="custom-icon-for-item-1"></span>',
                        alt: 'MY_ALT'
                    },
                    {
                        text: 'This item is disabled',
                        isActive: false,
                        index: 3,
                        addClass: 'disabled-element'
                    }
                ],
                metrika: 'Menu-Name'
            });

            this._menu.show({
                x: 0,
                y: 0
            });

        });

        xdescribe('Работа параметров элементов меню ->', function() {

            it('"addClass"', function() {
                expect($('.element1').length).to.be.equal(1);
            });

            xit('сортировка согласно "index"', function() {
                expect($('.js-context-menu-items .element1 + .element2').length).to.be.equal(1);
            });

            it('"isActive"', function() {
                expect($('.disabled-element').hasClass('is-inactive')).to.be.ok;
            });

            it('"customIcon"', function() {
                expect($('.element1 .custom-icon-for-item-1').length).to.be.equal(1);
            });

            it('"metrika"', function() {
                this.sinon.stub(Jane, 'c');

                $('.element2').click();

                expect(Jane.c.calledOnce).to.be.ok;
                expect(Jane.c.calledWith('Меню по правому клику', 'Menu-Name', 'text-of-metrika')).to.be.ok;
            });

            it('в случае отсутствия "metrika"', function() {
                this.sinon.stub(Jane, 'c');

                $('.element1').click();

                expect(Jane.c.calledOnce).to.be.ok;
                expect(Jane.c.calledWith('Меню по правому клику', 'Menu-Name', 'Неизвестная строка')).to.be.ok;
            });

            it('"alt"', function() {
                expect($('.element1').attr('title')).to.be.equal('MY_ALT');
            });
        });
    });

    describe('Работа с меню ->', function() {

        beforeEach(function() {

            this._menu = new Daria.ContextMenu({
                items: [
                    {
                        text: 'Hello1',
                        index: 1,
                        addClass: 'cm-element1',
                        onClick: this._stub1 = sinon.stub()
                    },
                    {
                        text: 'Hello2',
                        index: 2,
                        addClass: 'cm-element2',
                        onClick: this._stub2 = sinon.stub()
                    }
                ]
            });

            this._menu.show({
                x: 0,
                y: 0
            });

        });

        it('срабатывает "onClick" при клике', function() {
            $('.cm-element2').click();

            expect(this._stub2.calledOnce).to.be.ok;
        });

        it('не срабатывает "onClick" на других элементах', function() {
            $('.cm-element2').click();

            expect(this._stub1.notCalled).to.be.ok;
        });

    });

});
