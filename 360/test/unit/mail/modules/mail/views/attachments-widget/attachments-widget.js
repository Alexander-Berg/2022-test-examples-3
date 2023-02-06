describe('Daria.vAttachmentsWidget', function() {
    beforeEach(function() {
        this.view = ns.View.create('attachments-widget', { ids: '123' });
        this.emlView = ns.View.create('attachments-widget', { ids: '123', hid: '456' });

        let $target = $('<div>').data('params', 'mid=12345&hid=6789');
        this.clickEvent = {
            currentTarget: $target[0],
            preventDefault: this.sinon.stub()
        };
    });

    describe('#previewImage', function() {
        beforeEach(function() {
            this.sinon.stub(Daria.ImageViewer, 'open');
        });

        it('Должен предотвращать действие по умолчанию', function() {
            this.view.previewImage(this.clickEvent);
            expect(this.clickEvent.preventDefault).to.have.callCount(1);
        });

        it('Должен открыть просмотрщик изображений', function() {
            this.view.previewImage(this.clickEvent);
            expect(Daria.ImageViewer.open).to.have.callCount(1);
        });

        it('Должен открыть просмотрщик изображений с mid и hid для обычного письма', function() {
            this.view.previewImage(this.clickEvent);
            expect(Daria.ImageViewer.open).to.be.calledWith('', '12345', '6789');
        });

        it('Для вложенного письма должен открыть просмотрщик изображений с mid и hid аттача и message-hid родительского письма', function() {
            this.emlView.previewImage(this.clickEvent);
            expect(Daria.ImageViewer.open).to.be.calledWith('', '12345', '6789', '456');
        });
    });

    describe('#onAttachmentActionsItemClick', function() {
        beforeEach(function() {
            this.sinon.stub(this.view, 'logClickMessageFromSearch');
            this.sinon.stub(this.view, 'writeAttachOpenToUserJournal');
            this.sinon.stub(this.view, 'getAttachSource');
        });

        it('должен вызвать логгер поисковых кликов', function() {
            this.view.onAttachmentActionsItemClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.have.callCount(1);
        });

        it('логгер поисковых кликов должен быть вызван с параметром attach', function() {
            this.view.onAttachmentActionsItemClick(this.clickEvent);

            expect(this.view.logClickMessageFromSearch).to.be.calledWith('attach');
        });

        it('должен вызвать getAttachSource', function() {
            this.view.onAttachmentActionsItemClick(this.clickEvent);

            expect(this.view.getAttachSource).to.have.callCount(1);
        });

        it('должен вызвать writeAttachOpenToUserJournal', function() {
            this.view.onAttachmentActionsItemClick(this.clickEvent);

            expect(this.view.writeAttachOpenToUserJournal).to.have.callCount(1);
        });
    });
});
