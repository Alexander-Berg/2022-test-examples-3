module.exports = {
    module: 'third',
    commons: [
        require('./commons/comm1'),
        require('./commons/comm2'),
        require('./commons/comm3'),
    ],
    vendors: [
        require('./vendors/vend1'),
        require('./vendors/vend2'),
        require('./vendors/vend3'),
    ],
    styles: [
        require('./third.css'),
    ],
};
