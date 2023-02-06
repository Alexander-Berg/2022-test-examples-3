describe('Daria.vFolder', function() {

    beforeEach(function() {
        /** @type Daria.mFolders */
        this.mFolders = ns.Model.get('folders');
        setModelByMock(this.mFolders);

        /** @type Daria.vFolder */
        this.vFolder = ns.View.create('folder', {fid: '1'});
        return this.vFolder.update();
    });

    describe('#onFolderModelChanged', function() {

        beforeEach(function() {
            this.sinon.stub(this.vFolder, 'forceUpdate');
            this.sinon.stub(this.vFolder.$node, 'toggleClass');
        });

        describe('event === "ns-model-changed.folded" ->', function() {

            beforeEach(function() {
                this.vFolder.onFolderModelChanged('ns-model-changed.folded', '.folded');
            });

            it('должен поменять класс', function() {
                expect(this.vFolder.$node.toggleClass).to.have.callCount(1);
            });

            it('не должен вызвать обновление', function() {
                expect(this.vFolder.forceUpdate).to.have.callCount(0);
            });

        });

        describe('event === "ns-model-changed", jpath === ".folded" ->', function() {

            beforeEach(function() {
                this.vFolder.onFolderModelChanged('ns-model-changed', '.folded');
            });

            it('не должен поменять класс', function() {
                expect(this.vFolder.$node.toggleClass).to.have.callCount(0);
            });

            it('не должен вызвать обновление', function() {
                expect(this.vFolder.forceUpdate).to.have.callCount(0);
            });

        });

        describe('event === "ns-model-changed", jpath === "" ->', function() {

            beforeEach(function() {
                this.vFolder.onFolderModelChanged('ns-model-changed', '');
            });

            it('не должен поменять класс', function() {
                expect(this.vFolder.$node.toggleClass).to.have.callCount(0);
            });

            it('должен вызвать обновление', function() {
                expect(this.vFolder.forceUpdate).to.have.callCount(1);
            });

        });

    });

    describe('#onClickMarkAllRead', function() {
        beforeEach(function() {
            this.clickEvent = {
                preventDefault: this.sinon.stub(),
                stopPropagation: this.sinon.stub()
            };
            this.sinon.stub(Daria.Folder, 'markRead').withArgs(this.vFolder.params.fid);
            this.sinon.stub(this.vFolder, '_showMarkAllReadPopup');
            this.sinon.stub(Daria.Dialog, 'close');
            this.sinon.stub(nb, 'trigger').withArgs('popup-close');

            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'isSet').withArgs('no_popup_mark_read');
        });


        it('При клике на прыщ прочитанности должен предотвратить дефолтную обработку и не передавать событие клика дальше', function() {
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(this.clickEvent.preventDefault).to.have.callCount(1);
            expect(this.clickEvent.stopPropagation).to.have.callCount(1);
        });

        it('При клике на прыщ прочитанности должен закрыть все попапы', function() {
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(nb.trigger).to.have.callCount(1);
            expect(Daria.Dialog.close).to.have.callCount(1);
        });

        it('Если выставлена настройка "Не показывать попап больше", должен пометить все письма в текущей папке прочитанными', function() {
            this.mSettings.isSet.returns(true);
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(Daria.Folder.markRead).to.have.callCount(1);
        });

        it('Если выставлена настройка "Не показывать попап больше", не должен показывать попап"', function() {
            this.mSettings.isSet.returns(true);
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(this.vFolder._showMarkAllReadPopup).to.have.callCount(0);
        });

        it('Если не выставлена настройка "Не показывать попап больше", должен показать попап', function() {
            this.mSettings.isSet.returns(false);
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(this.vFolder._showMarkAllReadPopup).to.have.callCount(1);
        });

        it('Если не выставлена настройка "Не показывать попап больше", не должен помечать письма прочитанными', function() {
            this.mSettings.isSet.returns(false);
            this.vFolder.onClickMarkAllRead(this.clickEvent);
            expect(Daria.Folder.markRead).to.have.callCount(0);
        });
    });

});
