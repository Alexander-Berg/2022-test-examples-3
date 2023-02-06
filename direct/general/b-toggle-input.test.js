describe('b-toggle-input', function() {
    var tree,
        toggleInput,
        input,
        link,
        innerLink,
        clock;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
        tree = u.getDOMTree({
            block: 'b-toggle-input',
            mods: { mode: 'link' },
            value: 100,
            name: 'name'
        });

        $('body').append(tree);

        toggleInput = BEM.DOM.init(tree).bem('b-toggle-input');
        input = toggleInput.input;
        link = toggleInput.link;
        innerLink = link.elem('inner');
    });

    afterEach(function() {
        clock.tick(0);
        BEM.DOM.destruct(tree);
        clock.restore();
    });

    it('must be inited with link, not field', function(){
        expect(input.domElem.is(':visible')).to.be.equal(false);
        expect(link.domElem.is(':visible')).to.be.equal(true);
    });

    it('must switch to input on click', function(){
        link.domElem.click();

        clock.tick(0);

        expect(input.domElem.is(':visible')).to.be.equal(true);
        expect(link.domElem.is(':visible')).to.be.equal(false);
    });

    it('must correctly get and set value', function(){
        expect(input.val()).to.be.eql('100');
        expect(innerLink.text()).to.be.eql('100');
        expect(toggleInput.val()).to.be.eql('100');

        toggleInput.val('test');

        expect(input.val()).to.be.eql('test');
        expect(innerLink.text()).to.be.eql('test');
        expect(toggleInput.val()).to.be.eql('test');
    });

    it('must correctly respond to toggle()', function(){
        //toggle to input
        toggleInput.toggle(true);

        clock.tick(0);
        expect(input.domElem.is(':visible')).to.be.equal(true);
        expect(input.hasMod('focused')).to.be.equal(true);
        expect(link.domElem.is(':visible')).to.be.equal(false);

        //toggle to link
        toggleInput.toggle(false);

        expect(input.domElem.is(':visible')).to.be.equal(false);
        expect(link.domElem.is(':visible')).to.be.equal(true);

        //toggle to input (without args)
        toggleInput.toggle();
        expect(input.domElem.is(':visible')).to.be.equal(true);
        expect(input.hasMod('focused')).to.be.equal(true);
        expect(link.domElem.is(':visible')).to.be.equal(false);
    })

});
