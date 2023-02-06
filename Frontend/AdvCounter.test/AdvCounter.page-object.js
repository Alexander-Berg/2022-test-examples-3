'use strict';

const { Entity, create } = require('../../../vendors/hermione/index');
const blocks = require('../../../../hermione/page-objects/common/blocks');

const PO = {};

PO.serpAdv = blocks.serpAdvItem.copy();
PO.serpAdvPremium1 = PO.serpAdv.nthType(1);
PO.serpAdvPremium1.serpAdvCounter = blocks.serpAdvCounter.copy();
PO.serpAdvPremium3 = PO.serpAdv.nthType(3);
PO.serpAdvPremium3.serpAdvCounter = blocks.serpAdvCounter.copy();
PO.serpAdvHalfpremium1 = blocks.serpItem.not(blocks.serpAdvItem).adjacentSibling(blocks.serpAdvItem);
PO.serpAdvHalfpremium1.serpAdvCounter = blocks.serpAdvCounter.copy();
PO.serpAdvHalfpremium2 = blocks.serpItem.not(blocks.serpAdvItem).adjacentSibling(blocks.serpAdvItem)
    .adjacentSibling(blocks.serpAdvItem);
PO.serpAdvHalfpremium2.serpAdvCounter = blocks.serpAdvCounter.copy();

PO.beforeHalfPremiumSnippet = blocks.serpItem.nthType(16);
PO.pager = new Entity({ block: 'pager' });
PO.header = blocks.header.copy();

module.exports = create(PO);
