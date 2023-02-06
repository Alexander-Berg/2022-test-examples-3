describeBlock('misspell_type_minusword__formatter', function(block) {
    var words;

    it('should join elements with comma', function() {
        words = ['one', 'two'];
        assert.equal(block(words), '<span class="misspell__error misspell__error_type_bold">one, two</span>');
    });
});
