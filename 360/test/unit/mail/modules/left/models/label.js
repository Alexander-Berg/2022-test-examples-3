describe('Daria.mLabel', function() {

    beforeEach(function() {
        /** @type Daria.mLabel */
        this.mLabel = ns.Model.get('label', {lid: '1'}).setData({});
    });

    it('Должны убрать признак unused, если количество писем с меткой больше 1', function() {
        this.mLabel.set('.unused', true);
        this.mLabel.set('.count', 1);
        expect(this.mLabel.get('.unused')).to.be.equal(false);
    });

    it('Должны установить признак unused, если количество писем с меткой равно 0', function() {
        this.mLabel.set('.unused', false);
        this.mLabel.set('.count', 0);
        expect(this.mLabel.get('.unused')).to.be.equal(true);
    });
});

