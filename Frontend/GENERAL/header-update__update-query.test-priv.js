describeBlock('header__update-query-js', function(block) {
    var data;

    beforeEach(function() {
        data = { query: { text: '' } };
        data.isPumpkin = false;
    });

    it('should not escape &', function() {
        data.query.text = 'm&ms';

        expect(block(data)).to.match(/m&ms/);
    });

    it('should escape </script> and <!--', function() {
        data.query.text = '</script><!--';

        expect(block(data)).to.not.match(/(<\/script)|(<--)/);
    });
});
