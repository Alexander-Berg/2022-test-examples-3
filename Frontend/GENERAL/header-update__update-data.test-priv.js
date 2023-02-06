describeBlock('header__update-data-js', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('cgi', 'experiments', 'counters');
        data.isPumpkin = false;
    });

    stubBlocks([
        'RequestCtx',
        'search2__input-js',
        'header__search2-context'
    ]);

    it('should not escape &', function() {
        blocks['search2__input-js'].returns({ text: 'm&ms' });

        expect(block(data)).to.match(/m&ms/);
    });

    it('should escape </script> and <!--', function() {
        blocks['search2__input-js'].returns({ text: '</script><!--' });

        expect(block(data)).to.not.match(/(<\/script)|(<--)/);
    });
});
