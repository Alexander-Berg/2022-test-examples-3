const path = require('path');

const webpack = require('webpack');
const Memoryfs = require('memory-fs');

module.exports = (fixture, options) => {
    const compiler = webpack({
        context: __dirname,
        entry: `./fixtures/${fixture}`,
        output: {
            path: path.resolve(__dirname),
            filename: 'bundle.js',
        },
        module: {
            rules: [{
                test: /(\\|\/)fixtures(\\|\/).+\.(js|ts|tsx)/,
                use: {
                    loader: path.resolve(__dirname, '..'),
                    options: {
                        relativeTo: path.resolve(__dirname, 'fixtures'),
                        handler: 'someHandler',
                        ...options,
                    },
                },
            }],
        },
    });

    compiler.outputFileSystem = new Memoryfs();

    return new Promise((resolve, reject) => {
        compiler.run((err, stats) => {
            if (err) {
                reject(err);
            }

            try {
                resolve(stats.toJson().modules[0].source);
            } catch (ex) {
                reject(ex);
            }
        });
    });
};
