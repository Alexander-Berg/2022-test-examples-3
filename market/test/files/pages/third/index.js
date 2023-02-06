module.exports = {
    module: 'third/page',
    // eslint-disable-next-line global-require
    widget: require('./widget'),
    styles: [
        require('./index.css'),
    ],
};
