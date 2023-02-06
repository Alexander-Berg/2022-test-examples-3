describe('b-api-management-cert-moderation', function() {
    var block;

    beforeEach(function() {
        var node = u.getDOMTree({
            block: 'b-api-management-cert-moderation'
        });

        node.appendTo($('body'));

        block = node.bem('b-api-management-cert-moderation');
    });

    afterEach(function() {
        block.destruct();
    });

    it ('должен предоставлять application_id, ulogin, action в том виде, в котором они были переданы в параметры', function() {
        block.prepareToShow({
            application_id: 1,
            ulogin: 2,
            action: 3
        });

        var data = block.provideData();

        expect(data.application_id).to.be.equal(1);
        expect(data.ulogin).to.be.equal(2);
        expect(data.action).to.be.equal(3);
    });

    it ('должен предоставлять comment как содержимое введенного комментария', function() {
        block.prepareToShow({});
        block._comment.val('comment');
        expect(block.provideData().comment).to.be.equal('comment');
    });
});
