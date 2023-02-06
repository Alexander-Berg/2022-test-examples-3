const { Entity } = require('../../../../../vendors/hermione');

module.exports = function() {
    const elems = {};

    elems.translate = new Entity({ block: 't-construct-adapter', elem: 'translate' });

    return elems;
};
