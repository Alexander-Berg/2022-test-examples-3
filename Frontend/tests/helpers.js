const { assert } = require('chai');
const { Entity } = require('..');

describe('css helpers', function() {
    let PO;

    beforeEach(function() {
        PO = {};
    });

    it('should add specified pseudo-class', function() {
        let serpItem = new Entity('.serp-item');
        PO.serpItemHovered = serpItem.pseudo('hover');

        assert.equal(PO.serpItemHovered(), '.serp-item:hover');
    });

    it('should add "not" pseudo-class', function() {
        let block = new Entity({ block: 'block' });
        let block2 = new Entity({ block: 'block2' });

        PO.blockNotBlock2 = block.not(block2);

        assert.equal(PO.blockNotBlock2(), '.block:not(.block2)');
    });

    it('should add "nth-child" pseudo-class', function() {
        let serpItem = new Entity('.serp-item');

        PO.serpItem0 = serpItem.nthChild(0);

        assert.equal(PO.serpItem0(), '.serp-item:nth-child(0)');
    });

    it('should add "first-child" pseudo-class', function() {
        let serpItem = new Entity('.serp-item');

        PO.serpItemFirst = serpItem.firstChild();

        assert.equal(PO.serpItemFirst(), '.serp-item:first-child');
    });

    it('should add "last-child" pseudo-class', function() {
        let serpItem = new Entity('.serp-item');

        PO.serpItemLast = serpItem.lastChild();

        assert.equal(PO.serpItemLast(), '.serp-item:last-child');
    });

    it('should add "nth-of-type" pseudo-class', function() {
        PO.serpItem0 = new Entity('.serp-item').nthType(0);

        assert.equal(PO.serpItem0(), '.serp-item:nth-of-type(0)');
    });

    it('should add "first-of-type" pseudo-class', function() {
        let serpItem = new Entity('.serp-item');

        PO.serpItemFirstOfType = serpItem.firstOfType();

        assert.equal(PO.serpItemFirstOfType(), '.serp-item:first-of-type');
    });

    it('should add "last-of-type" pseudo-class', function() {
        let serpItem = new Entity('.serp-item');

        PO.serpItemLastOfType = serpItem.lastOfType();

        assert.equal(PO.serpItemLastOfType(), '.serp-item:last-of-type');
    });

    it('should add descendant combinator', function() {
        let link = new Entity('.link');
        PO.titleLink = new Entity('.title').descendant(link);

        assert.equal(PO.titleLink(), '.title .link');
    });

    it('should add adjacent sibling combinator', function() {
        let path = new Entity('.path');

        PO.titleSibling = new Entity('.title').adjacentSibling(path);

        assert.equal(PO.titleSibling(), '.title + .path');
    });

    it('should add general sibling combinator', function() {
        PO.titleGeneralSibling = new Entity('.title').generalSibling(new Entity('.price'));

        assert.equal(PO.titleGeneralSibling(), '.title ~ .price');
    });

    it('should add child combinator', function() {
        PO.titleChild = new Entity('.title').child(new Entity('.price'));

        assert.equal(PO.titleChild(), '.title > .price');
    });

    it('should append all selectors after combinator', function() {
        let complicatedPO = new Entity('.price');
        complicatedPO.currency = new Entity('.currency');
        complicatedPO.currency.sign = new Entity('.currency__sign');

        PO.title = new Entity('.title').combinator(' + ', complicatedPO);

        assert.equal(PO.title.currency.sign(), '.title + .price .currency .currency__sign');
    });

    it('should mix selectors', function() {
        let PO = {};

        PO.alert = new Entity('.alert');
        PO.popup = new Entity('.popup');
        PO.alertPopup = PO.alert.mix(PO.popup);

        assert.equal(PO.alertPopup(), '.alert.popup');
    });

    it('should keep own properties after mix if there are no such properties in mixing entity', function() {
        let PO = {};

        PO.alert = new Entity({ block: 'alert' });
        PO.alert.title = new Entity({ block: 'alert', elem: 'title' });
        PO.popup = new Entity({ block: 'popup' });
        PO.alertPopup = PO.alert.mix(PO.popup);

        assert.equal(PO.alertPopup.title(), '.alert.popup .alert__title');
    });

    it('should add properties from mixing entity', function() {
        let PO = {};

        PO.alert = new Entity({ block: 'alert' });
        PO.popup = new Entity({ block: 'popup' });
        PO.popup.title = new Entity({ block: 'popup', elem: 'title' });
        PO.alertPopup = PO.alert.mix(PO.popup);

        assert.equal(PO.alertPopup.title(), '.alert.popup .popup__title');
    });

    it('should fail if entities have common properties', function() {
        let PO = {};

        PO.alert = new Entity({ block: 'alert' });
        PO.alert.title = new Entity({ block: 'alert', elem: 'title' });
        PO.popup = new Entity({ block: 'popup' });
        PO.popup.title = new Entity({ block: 'popup', elem: 'title' });

        assert.throws(function() {
            PO.alert.mix(PO.popup);
        }, /Одинаковые элементы в сущностях: title/);
    });
});
