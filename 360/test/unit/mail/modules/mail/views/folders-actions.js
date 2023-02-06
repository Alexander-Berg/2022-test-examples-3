xdescribe('Daria.vFoldersActions', function() {

    describe('#getFolderName', function() {
        beforeEach(function() {
            this.foldersActions = ns.View.create('folders-actions');
        });

        it('если $searchInput равен null, то должен возвращать пустую строку', function() {
            this.foldersActions.$searchInput = null;

            expect(this.foldersActions.getFolderName()).to.be.equal('');
        });

        it('если $searchInput.length равен 0, то должен возвращать пустую строку', function() {
            this.foldersActions.$searchInput = $();

            expect(this.foldersActions.getFolderName()).to.be.equal('');
        });

        it('если есть $searchInput, то нужно вернуть его значение', function() {
            var result = 'ololo';
            this.foldersActions.$searchInput = $('<input value="' + result + '"/>');

            expect(this.foldersActions.getFolderName()).to.be.equal(result);
        });
    });

    describe('#createNewFolder', function() {

        beforeEach(function() {
            this.foldersActions = ns.View.create('folders-actions');
            this.sinon.stub(ns.action, 'run');
            this.sinon.stub(this.foldersActions, 'getFolderName');
        });

        it('если есть Jane.$B("folders-actions").$searchInput, то должен вызваться экшен "folders.add" с правильными параметрами', function() {
            this.foldersActions.getFolderName.returns('ololo');

            this.foldersActions.createNewFolder();

            expect(ns.action.run).to.be.calledWithMatch('folders.add', {
                name: 'ololo',
                toolbar: true
            });
        });

    });
});
