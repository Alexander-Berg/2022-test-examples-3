describe('Daria.SaveToDiskMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('thread-sidebar-item-attachments-item', {'url-key': {}});
    });

    // TODO: переместить в тесты Daria.CountAttachmentsOpsMixin
    // describe('getCountLocationName', function() {
    //     it('В зависимости от id-ника возвращает строку для метрики', function() {
    //         this._v1 = ns.View.create('thread-sidebar-item-attachments-item', {'url-key': {}});
    //         expect(this._v1.getCountLocationName()).to.equal('в правой колонке');
    //
    //         this._v1 = ns.View.create('attachments-widget', {ids: '1111'});
    //         expect(this._v1.getCountLocationName()).to.equal('в списке писем');
    //
    //
    //         this._v1 = ns.View.create('attachments-widget-body', {ids: '1111'});
    //         expect(this._v1.getCountLocationName()).to.equal('в письме');
    //
    //         this._v1 = ns.View.create('attachments-widget-popup', {ids: '1111'});
    //         expect(this._v1.getCountLocationName()).to.equal('в попапе в списке писем');
    //     });
    //     it('Если нет id-ника в списке писем, то логируем это как ошибку', function() {
    //         this._v1 = ns.View.create('save-to-disk-mixin');
    //         this.sinon.stub(ns, 'assert');
    //         expect(this._v1.getCountLocationName()).to.equal(undefined);
    //         expect(ns.assert).to.have.callCount(1);
    //         expect(ns.assert).to.be.calledWith(
    //             undefined,
    //             'Daria.vAttachmentsWidget',
    //             'no name specified for %s to count in metrika',
    //             'save-to-disk-mixin'
    //         );
    //     });
    // });

    describe('countSaveToDisk', function() {
        it('Если вызываем метод вьюхи, выводим переопределенную метрику', function() {
            this.sinon.stub(Jane, 'c');
            this._v1 = ns.View.create('thread-sidebar-item-attachments-item', {'url-key': {}});
            this._v1.countSaveToDisk();

            expect(Jane.c).to.have.callCount(1);
            expect(Jane.c).to.be.calledWith([
                'Правая колонка',
                'Блоки правых колонок',
                'Показ Вложения',
                'Клик во вложение',
                'Сохранить на Диск'
            ]);
        });

    });
});
