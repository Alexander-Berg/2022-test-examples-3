module.exports = {
    module: 'comm1',
    styles: [
        require('./comm1.css'),
        require('./images.css'),
    ],
    modules: [
        require('../modules/module1'),
    ],
    flow: [
        require('../flow/flow1'),
    ],
    resolvers: [
        require('../resolvers/sync'),
        require('../resolvers/async'),
        require('../resolvers/remote'),
    ],
    externals: [
        require('react'),
    ],
};
