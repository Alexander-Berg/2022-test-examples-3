const assert = require('assert');
const { Entity } = require('..');

class ReactEntity extends Entity {
    static preset() {
        return 'react';
    }
}

describe('React naming support', function() {
    let PO;
    let reusedPO = {};

    before(function() {
        reusedPO.arrow = new ReactEntity({ block: 'arrow' });
        reusedPO.arrow.search = new ReactEntity({ block: 'arrow', elem: 'search' });

        reusedPO.page = new ReactEntity({ block: 'page' });
        reusedPO.page.header = new ReactEntity({ block: 'header' });
        reusedPO.page.header.arrow = reusedPO.arrow;
    });

    beforeEach(function() {
        PO = {};
    });

    it('Selector with bem-naming', function() {
        PO.page = new ReactEntity({ block: 'page', modName: 'js', modVal: 'inited' });

        assert.equal(PO.page(), '.page_js_inited');
    });

    it('Re-usage of nested blocks builds correct selector', function() {
        assert.equal(reusedPO.page.header.arrow(), '.page .header .arrow');
        assert.equal(reusedPO.page.header.arrow.search(), '.page .header .arrow .arrow-search');
    });

    it('Several modifiers with boolean value', function() {
        PO.serpList = new ReactEntity({ block: 'serp-list' });
        PO.serpList.serpItem = new ReactEntity({ block: 'serp-item' });
        PO.serpList.serpItem0 = PO.serpList.serpItem.mods({ pos: 0, inited: '' });

        assert.equal(PO.serpList.serpItem0(), '.serp-list .serp-item.serp-item_pos_0.serp-item_inited');
    });

    it('Considering nested elements when creating modifier', function() {
        PO.serpList = new ReactEntity('.serp-list');
        PO.serpList.serpItem = new ReactEntity({ block: 'serp-item' });
        PO.serpList.serpItem.snippet = new ReactEntity({ block: 'serp-item', elem: 'snippet' });
        PO.serpList.serpItem.itemLink = new ReactEntity({ block: 'serp-item', elem: 'link' });
        PO.serpList.serpItem0 = PO.serpList.serpItem.mods({ pos: 0 });

        assert.equal(PO.serpList.serpItem0.itemLink(), '.serp-list .serp-item.serp-item_pos_0 .serp-item-link');
    });
});
