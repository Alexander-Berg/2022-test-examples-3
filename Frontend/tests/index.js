const assert = require('assert');
const _ = require('lodash');
const { Entity } = require('..');

describe('common functionality', function() {
    let PO;
    let reusedPO = {};

    before(function() {
        reusedPO.arrow = new Entity({ block: 'arrow' });
        reusedPO.arrow.search = new Entity({ block: 'arrow', elem: 'search' });

        reusedPO.page = new Entity({ block: 'b-page' });
        reusedPO.page.header = new Entity({ block: 'header' });
        reusedPO.page.header.arrow = reusedPO.arrow.copy();
    });

    beforeEach(function() {
        PO = {};
    });

    it('Selector for unique element', function() {
        PO.page = new Entity({ block: 'b-page' });

        assert.equal(PO.page(), '.b-page');
    });

    it('Selector with bem-naming', function() {
        PO.page = new Entity({ block: 'b-page', modName: 'js', modVal: 'inited' });

        assert.equal(PO.page(), '.b-page_js_inited');
    });

    it('Should consider the nesting', function() {
        PO.arrow = new Entity({ block: 'arrow' });
        PO.arrow.search = new Entity({ block: 'arrow', elem: 'search' });

        assert.equal(PO.arrow(), '.arrow');
        assert.equal(PO.arrow.search(), '.arrow .arrow__search');
    });

    it('Re-usage of nested blocks builds correct selector', function() {
        assert.equal(reusedPO.page.header.arrow(), '.b-page .header .arrow');
        assert.equal(reusedPO.page.header.arrow.search(), '.b-page .header .arrow .arrow__search');
    });

    it('Re-usage of nested blocks not changes origin entities', function() {
        assert.equal(reusedPO.arrow(), '.arrow');
        assert.equal(reusedPO.arrow.search(), '.arrow .arrow__search');
        assert.equal(reusedPO.page(), '.b-page');
        assert.equal(reusedPO.page.header(), '.b-page .header');
    });

    it('Common modifiers', function() {
        PO.serpList = new Entity('.serp-list');
        PO.serpList.serpItem = new Entity('.serp-item');
        PO.serpList.serpItem0 = PO.serpList.serpItem.mods({ pos: 0 });

        assert.equal(PO.serpList.serpItem0(), '.serp-list .serp-item.serp-item_pos_0');
    });

    it('Several modifiers with boolean value', function() {
        PO.serpList = new Entity('.serp-list');
        PO.serpList.serpItem = new Entity('.serp-item');
        PO.serpList.serpItem0 = PO.serpList.serpItem.mods({ pos: 0, inited: '' });

        assert.equal(PO.serpList.serpItem0(), '.serp-list .serp-item.serp-item_pos_0.serp-item_inited');
    });

    it('Considering nested elements when creating modifier', function() {
        PO.serpList = new Entity('.serp-list');
        PO.serpList.serpItem = new Entity('.serp-item');
        PO.serpList.serpItem.snippet = new Entity('.serp-item__snippet');
        PO.serpList.serpItem.itemLink = new Entity('.serp-item__link');
        PO.serpList.serpItem0 = PO.serpList.serpItem.mods({ pos: 0 });

        assert.equal(PO.serpList.serpItem0.itemLink(), '.serp-list .serp-item.serp-item_pos_0 .serp-item__link');
    });

    it('Should copy with methods', function() {
        let block = new Entity({ block: 'button' });

        assert(_.isFunction(block.copy().copy));
    });

    it('Should deep copy with methods', function() {
        const serpList = new Entity('.serp-list');
        serpList.serpItem = new Entity('.serp-item');

        assert(_.isFunction(serpList.copy().serpItem.copy));
    });
});
