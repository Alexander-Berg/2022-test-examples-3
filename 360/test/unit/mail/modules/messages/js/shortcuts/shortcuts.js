describe('Daria.Shortcuts', function() {

    describe('.getShortcutLabelFor', function() {

        it('Должен вернуть хоткей в скобках и с отступом если передан true', function() {
            var key = Daria.Shortcuts.getShortcutLabelFor('Forward', 'messages', true);

            expect(key).to.be.equal(' (Shift + f)');
        });

        it('Должен вернуть только хоткей если передан false', function() {
            var key = Daria.Shortcuts.getShortcutLabelFor('Forward', 'messages', false);

            expect(key).to.be.equal('Shift + f');
        });
    });
});
