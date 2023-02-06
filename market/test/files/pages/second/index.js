module.exports = {
    module: 'second/page',
    // eslint-disable-next-line global-require
    widget: require('./widget'),
    styles: [
        require('./index.css'),
    ],
};
