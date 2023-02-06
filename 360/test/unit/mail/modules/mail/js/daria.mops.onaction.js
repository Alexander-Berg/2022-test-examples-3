describe('Daria.MOPS. Реакция на действия.', function() {

    describe('Daria.MOPS.onMove.', function() {

        beforeEach(function() {
            this.sinon.stub(ns.action, 'run');

            this.mFolders = ns.Model.get('folders');
            this.sinon.stub(this.mFolders, 'isFolder').returns(false);

            this.mFilters = ns.Model.get('filters');
            this.sinon.stub(this.mFilters, 'getForFolder').returns([]);

            this.validParams = {
                count: 1,
                current_folder: '1',
                ids: {
                    ids: ['1']
                }
            };
        });

        it('не должен вызвать action, если переносим больше, чем 1 письмо', function() {
            ns.events.trigger('daria:MOPS:move', _.extend({}, this.validParams, {
                count: 2
            }));

            expect(ns.action.run).to.have.callCount(0);
        });

        it('не должен вызвать action, если переносим письмо в Удаленные', function() {
            this.mFolders.isFolder.returns(true);
            ns.events.trigger('daria:MOPS:move', _.extend({}, this.validParams));

            expect(ns.action.run).to.have.callCount(0);
        });

        it('не должен вызвать action, если переносим письмо в папку, куда настроены фильтры', function() {
            this.mFilters.getForFolder.returns([1]);
            ns.events.trigger('daria:MOPS:move', _.extend({}, this.validParams));

            expect(ns.action.run).to.have.callCount(0);
        });

        it('не должен вызвать action, если архивируем письмо', function() {
            ns.events.trigger('daria:MOPS:move', _.extend({}, this.validParams, {
                originalAction: 'archive'
            }));

            expect(ns.action.run).to.have.callCount(0);
        });

        it('не должен вызвать action, если переносим письмо ПК', function() {
            ns.events.trigger('daria:MOPS:move', _.extend({}, this.validParams, {
                originalAction: 'infolder'
            }));

            expect(ns.action.run).to.have.callCount(0);
        });

        it('должен вызвать action, если перемещаем письмо', function() {
            ns.events.trigger('daria:MOPS:move', this.validParams);

            expect(ns.action.run).to.have.callCount(1);
        });

        it('должен вызвать action с правильными параметерами, если перемещаем письмо', function() {
            ns.events.trigger('daria:MOPS:move', this.validParams);

            expect(ns.action.run).to.be.calledWith('message.filter-move.show', {
                fid: '1',
                mid: '1'
            });
        });

    });

});
