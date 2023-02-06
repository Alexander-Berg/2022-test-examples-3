describe('b-href-control_inline_yes', function() {
    it('У блока нет элемента hint', function() {
        var block = u.createBlock({
            block: 'b-href-control',
            mods: { inline: 'yes' },
            protocol: 'https://',
            href: 'vk.com'
        });

        expect(block).not.to.haveElem('hint');
        block.destruct();
    });
});
