const { Entity } = require('../../../../vendors/hermione');
const blocks = require('../../../../../hermione/page-objects/touch-phone/blocks');

const PO = {};

PO.route = blocks.serpItem.mix(
    new Entity({ block: 't-construct-adapter', elem: 'route' }),
);

PO.route.organic = blocks.organic.copy();

PO.route.goMt = new Entity('.button2[data-type="mt"]');
PO.route.goAuto = new Entity('.button2[data-type="auto"]');

module.exports = PO;
