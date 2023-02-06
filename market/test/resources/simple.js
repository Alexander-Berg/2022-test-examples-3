const Resource = require('./setup');

const ResourceMain = Resource.create();

ResourceMain.cfg = {
    path: '/test',
};

module.exports = ResourceMain;
