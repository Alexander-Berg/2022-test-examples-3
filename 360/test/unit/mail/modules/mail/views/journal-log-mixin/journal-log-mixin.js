describe('Daria.vJournalLogMixin', function() {
    beforeEach(function() {
        this.view = ns.View.create('journal-log-mixin');

        this.sinon.stub(this.view, 'getUserBehaviorId').returns('dsfsd12d');

        this.sinon.stub(ns.Model, 'get').returns(undefined);
        this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);
        this.sinon.stub(Daria, 'isPriorityTab').returns(false);
        this.sinon.stub(Daria, 'isAttachmentsTab').returns(false);
    });

    describe('#getUserBehaviorParams', function() {
        it('Логгируются основные параметры', function() {
            expect(this.view.getUserBehaviorParams()).to.be.eql({
                target: 'message',
                isSearch: false,
                readingId: 'dsfsd12d'
            });
        });

        it('Если есть mid, то логгирует его наряду с основными параметрами', function() {
            this.sinon.stub(this.view, 'params').value({ ids: '12345' });

            const res = this.view.getUserBehaviorParams();

            expect(res).to.be.eql({
                target: 'message',
                isSearch: false,
                readingId: 'dsfsd12d',
                mid: '12345'
            });
        });

        it('Если есть tid, то логгирует его наряду с основными параметрами', function() {
            this.sinon.stub(this.view, 'params').value({ tid: 't12345' });

            const res = this.view.getUserBehaviorParams();

            expect(res).to.be.eql({
                target: 'thread',
                isSearch: false,
                readingId: 'dsfsd12d',
                tid: 't12345'
            });
        });

        it('Если есть модели message и folder, логирует fid и folder_type', function() {
            this.view.params = { ids: '12345' };
            ns.Model.get
                .withArgs('message', { ids: '12345' }).returns({
                    getFolderId: function() {
                        return 'test_fid';
                    }
                })
                .withArgs('folders').returns({
                    getFolderById: function(id) {
                        expect(id).to.be.eql('test_fid');
                        return { fid: id, symbol: 'test' };
                    }
                });

            const res = this.view.getUserBehaviorParams();

            expect(res).to.be.eql({
                target: 'message',
                isSearch: false,
                readingId: 'dsfsd12d',
                mid: '12345',
                container_kind: 'folder',
                container_id: 'test_fid',
                container_type: 'test'
            });
        });

        it('Если папка не системная, логирует folder_type, как user', function() {
            this.view.params = { ids: '12345' };
            ns.Model.get
                .withArgs('message', { ids: '12345' }).returns({
                    getFolderId: function() {
                        return 'test_fid';
                    }
                })
                .withArgs('folders').returns({
                    getFolderById: function(id) {
                        expect(id).to.be.eql('test_fid');
                        return { fid: id };
                    }
                });

            const res = this.view.getUserBehaviorParams();

            expect(res).to.be.eql({
                target: 'message',
                isSearch: false,
                readingId: 'dsfsd12d',
                mid: '12345',
                container_kind: 'folder',
                container_id: 'test_fid',
                container_type: 'user'
            });
        });
    });
});
