const { Entity } = require('../../../../vendors/hermione');
const blocks = require('../../../../../hermione/page-objects/common/blocks');

module.exports = () => {
    const elems = {};

    elems.traffic = new Entity({ block: 't-construct-adapter', elem: 'traffic' });
    elems.traffic.semaphore = new Entity({ block: 'traffic-summary', elem: 'semaphore' });
    elems.traffic.status = new Entity({ block: 'traffic-summary', elem: 'status' });
    elems.traffic.map = blocks.map2.copy();

    return elems;
};
