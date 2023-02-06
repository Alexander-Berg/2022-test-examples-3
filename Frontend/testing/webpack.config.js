module.exports = [].concat(
    require('./webpack.client.config'),
    require('./webpack.server.config'),
    require('./webpack.overlay.config'),
    require('./webpack.externals.config'),
);
