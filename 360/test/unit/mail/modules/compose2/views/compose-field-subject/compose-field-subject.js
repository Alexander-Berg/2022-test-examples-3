describe('Daria.vComposeFieldSubject', function() {
    beforeEach(function() {
        this.view = ns.View.create('compose-field-subject');
    });

    describe('FIELD_NAME', function() {
        it('Должен быть `subj`', function() {
            expect(Object.getPrototypeOf(this.view).FIELD_NAME).to.be.equal('subj');
        });

        it('Должен зависеть от Daria.mComposeState для работы с фокусом', function() {
            expect(this.view.getModel('compose-state')).to.be.ok;
        });
    });
});
