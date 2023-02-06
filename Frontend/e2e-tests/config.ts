const port = process.env.TEST_STANDALONE ? 8088 : 8080;

export default {
    auth: {
        email: 'autotest@auto.test',
        password: 'autotest',
        getNewEmail: () => `autotest+${Date.now()}@auto.test`,
    },

    startURL: '/advertiser',
    origin: `http://0.0.0.0:${port}`,
    port,
};
