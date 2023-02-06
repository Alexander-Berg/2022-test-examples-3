module.exports = {
    env: 'test',
    hostname: 'test.sbs.yandex-team.ru',
    pluginsHost: 'https://test.sbs.yandex-team.ru',
    fdbHost: 'https://fdb-test.common.yandex.net',
    fdbPublicHost: 'https://test.fdb.sbs.yandex-team.ru',
    yandexInternalRootCAPath: '/usr/share/yandex-internal-root-ca/YandexInternalRootCA.crt',
    oAuthAppId: '96fdba7c16684cb1a4af93806fc8e0f5',
    nirvanaPrefix: 'test',
    mongoDB: {
        host: [
            'man-vfuj03vw5yoak2cj.db.yandex.net:27018',
            'sas-mmdr38zczv8v6132.db.yandex.net:27018',
            'vla-vldrtj858sqtg0mn.db.yandex.net:27018',
        ],
        db: 'samadhi_test',
        replicaSetName: 'rs01',
        options: { user: 'samadhi_admin' },
        secret: {
            id: 'sec-01dnpsrrne4gvjfwkt4rdfbdst',
            options: [
                {
                    src: 'samadhi-mongo-test-pass',
                    dest: 'pass',
                },
            ],
        },
    },
    hosts: {
        argentum: 'https://test.argentum.yandex-team.ru',
    },
};
