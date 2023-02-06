module.exports = {
    module: 'first/page',
    // eslint-disable-next-line global-require
    widget: require('./widget'),
    commons: [
        require('../../commons/comm1'),
    ],
    vendors: [
        require('../../vendors/vend1'),
    ],
    styles: [
        require('./index.css'),
    ],
};
