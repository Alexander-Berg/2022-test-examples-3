var NODE_PORT = process.env.NODE_PORT;

module.exports = {
    app: require.resolve('app'),

    workers: 1,

    control: {
        forkTimeout: 5000,
        stopTimeout: 10000
    },

    server: {
        port: NODE_PORT
    }
};
