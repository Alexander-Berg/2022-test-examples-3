describe('Daria.router.', function() {
    describe('#addThreadedParam.', function() {
        beforeEach(function() {
            var mFolders = ns.Model.get('folders');
            setModelByMock(mFolders);
            this.mSettings = ns.Model.get('settings');
            this.sinon.stub(this.mSettings, 'isThreaded').returns(true);
        });

        // папки без тредного режима
        [ 'draft', 'spam', 'template', 'trash' ].forEach(function(folderSymbol) {
            it('не должен добавлять флаг тредности для папки "' + folderSymbol + '"', function() {
                var mFolders = ns.Model.get('folders');
                var fid = mFolders.getFidBySymbol(folderSymbol);
                var params = {
                    threaded: 'yes',
                    current_folder: fid
                };

                Daria.router.addThreadedParam(params);
                expect(params).to.be.eql({
                    current_folder: fid
                });
            });
        });

        it('не должен добавлять флаг тредности для запроса с пейджером', function() {
            var params = {
                current_folder: '1',
                datePager: 'yes',
                threaded: 'yes'
            };

            Daria.router.addThreadedParam(params);
            expect(params).to.be.eql({
                current_folder: '1',
                datePager: 'yes'
            });
        });

        it('не должен добавлять флаг тредности для запроса с фильтром "extra_cond"', function() {
            var params = {
                current_folder: '1',
                extra_cond: 'only_atta',
                threaded: 'yes'
            };

            Daria.router.addThreadedParam(params);
            expect(params).to.be.eql({
                current_folder: '1',
                extra_cond: 'only_atta'
            });
        });

        describe('не должен добавлять флаг тредности, если выключен тредный режим', function() {
            beforeEach(function() {
                this.mSettings.isThreaded.restore();
                this.sinon.stub(this.mSettings, 'isThreaded').returns(false);
            });

            it('для обычной папки', function() {
                var params = {
                    threaded: 'yes',
                    current_folder: '1'
                };

                Daria.router.addThreadedParam(params);
                expect(params).to.be.eql({
                    current_folder: '1'
                });
            });

            it('для папки Архив', function() {
                var mFolders = ns.Model.get('folders');
                var fid = mFolders.getFidBySymbol('archive');
                var params = {
                    threaded: 'yes',
                    current_folder: fid
                };

                Daria.router.addThreadedParam(params);
                expect(params).to.be.eql({
                    current_folder: fid
                });
            });
        });

        it('должен добавлять флаг тредности для обычной папки, если включен тредный режим', function() {
            var params = {
                current_folder: '1'
            };

            Daria.router.addThreadedParam(params);
            expect(params).to.be.eql({
                current_folder: '1',
                threaded: 'yes'
            });
        });

        it('должен добавлять флаг тредности для папки Архив, если включен тредный режим', function() {
            var mFolders = ns.Model.get('folders');
            var fid = mFolders.getFidBySymbol('archive');
            var params = {
                current_folder: fid
            };

            Daria.router.addThreadedParam(params);
            expect(params).to.be.eql({
                current_folder: fid,
                threaded: 'yes'
            });
        });
    });
});
