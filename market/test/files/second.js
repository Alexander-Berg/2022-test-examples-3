module.exports = {
    module: 'second',
    commons: [
        require('./commons/comm1'),
        require('./commons/comm2'),
    ],
    vendors: [
        require('./vendors/vend1'),
        require('./vendors/vend2'),
    ],
    styles: [
        require('./second.css'),
    ],
};
