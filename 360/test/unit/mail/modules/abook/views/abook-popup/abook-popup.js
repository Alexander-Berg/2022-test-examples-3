describe('Daria.vAbookPopup', function() {
    beforeEach(function() {
        this.view = ns.View.create('abook-popup');
    });

    describe('#setSelectedEmails', function() {

        it('устанавливает this.selectedEmails даже если параметр не задан', function() {
            this.view.setSelectedEmails();
            expect(this.view.selectedEmails).to.be.eql([]);
        });

        it('устанавливает this.selectedEmails даже если передан пустой объект', function() {
            this.view.setSelectedEmails({});
            expect(this.view.selectedEmails).to.be.eql([]);
        });

        it('у преобразованных имейлов проставляется свойство field', function() {
            this.view.setSelectedEmails({
                to: [
                    { email: 'test1@ya.ru', name: 'Test 1' }
                ]
            });
            expect(this.view.selectedEmails[0].field).to.be.equal('to');
        });

        it('преобразуем объект с имейлами в массив', function() {
            this.view.setSelectedEmails({
                to: [
                    { email: 'test1@ya.ru', name: 'Test 1' }
                ],
                cc: [
                    { email: 'test2@ya.ru', name: 'Test 2' },
                    { email: 'test3@ya.ru', name: 'Test 3' }
                ]
            });
            expect(this.view.selectedEmails).to.be.eql([
                { email: 'test1@ya.ru', name: 'Test 1', field: 'to' },
                { email: 'test2@ya.ru', name: 'Test 2', field: 'cc' },
                { email: 'test3@ya.ru', name: 'Test 3', field: 'cc' }
            ]);
        });

    });

});
