describe('Daria.vDiskWidgetSavePopup', function() {

    beforeEach(function() {
        this.view = ns.View.create('disk-widget-save-popup');
    });

    describe('#_getMailAttachmentSrc', function() {

        it('строит ключ для переданных mid и hid', function() {
            expect(this.view._getMailAttachmentSrc('160440736725076013', '1.2')).to.be.eql('/mail/file:160440736725076013:1.2');
        });

        it('не строит ключ если не передан mid', function() {
            expect(this.view._getMailAttachmentSrc(null, '1.2')).to.be.equal(null);
        });

        it('не строит ключ если не передан hid', function() {
            expect(this.view._getMailAttachmentSrc('160440736725076013')).to.be.equal(null);
        });

        it('не строит ключ если не передан ни mid ни hid', function() {
            expect(this.view._getMailAttachmentSrc()).to.be.equal(null);
        });

    });

    describe('#_getSaveToDiskWidgetParams', function() {

        beforeEach(function() {
            this.uid = '112233';
            this.diskSecretKey = '322';
            this.fakeWidgetNode = {};

            this.sinon.stub(ns.Model.get('account-information'), 'get').withArgs('.uid').returns(this.uid);
            this.sinon.stub(Jane.ErrorLog, 'send');
        });

        describe('Почтовые аттачи', function() {

            it('если sourceId вычислилось - объект с параметрами возвращается', function() {
                var mailAttachSourceId = '/mail/file:44:1.1';
                var openOptions = { mid: '44', hid: '1.1' };

                this.sinon.stub(this.view, '_getMailAttachmentSrc').returns(mailAttachSourceId);

                var result = this.view._getSaveToDiskWidgetParams(openOptions, this.diskSecretKey, this.fakeWidgetNode);
                var goodResult = {
                    source: 'copy-widget-yamail',
                    type: 'mail',
                    sourceId: mailAttachSourceId,
                    uid: this.uid,
                    sk: this.diskSecretKey,
                    container: this.fakeWidgetNode
                };

                expect(result).to.be.eql(goodResult);
            });

            it('если sourceId не вычислилось - логируем ошибку и возвращаем null', function() {
                var mailAttachSourceId = null;
                var openOptions = { mid: '44', hid: '1.1' };
                var diskSecretKey = '322';
                var fakeWidgetNode = {};

                this.sinon.stub(this.view, '_getMailAttachmentSrc').returns(mailAttachSourceId);

                var result = this.view._getSaveToDiskWidgetParams(openOptions, diskSecretKey, fakeWidgetNode);

                expect(result).to.be.eql(null);

                expect(Jane.ErrorLog.send).to.have.callCount(1);
                expect(Jane.ErrorLog.send).calledWith({
                    errorType: 'disk-widget-save-popup_could-not-generate-source-id',
                    mid: openOptions.mid,
                    hid: openOptions.hid,
                    diskDownloadUrl: undefined
                });
            });

        });

        describe('Дисковые аттачи', function() {

            it('передан diskDownloadUrl => передаём в виджет, что сохраняется дисковый аттач', function() {
                var diskDownloadUrl = 'https://yadi.sk/mail/?hash=ALFH7q01KV%2B8';
                var openOptions = { diskDownloadUrl: diskDownloadUrl };

                var result = this.view._getSaveToDiskWidgetParams(openOptions, this.diskSecretKey, this.fakeWidgetNode);
                var goodResult = {
                    source: 'copy-widget-yamail',
                    type: 'disk-public',
                    sourceId: diskDownloadUrl,
                    uid: this.uid,
                    sk: this.diskSecretKey,
                    container: this.fakeWidgetNode
                };

                expect(result).to.be.eql(goodResult);
            });

        });

    });

});
