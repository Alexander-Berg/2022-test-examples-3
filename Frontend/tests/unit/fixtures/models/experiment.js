const Readable = require('stream').Readable;

module.exports = {
    _data: [
        {
            id: 1,
            ticket: 'SIDEBYSIDE-1',
            login: 'eroshinev',
            params: {
                poolTitle: 'sbs_ranking',
                assessmentGroup: 'tolokers',
                notificationMode: {
                    preset: 'workflowOnly',
                    workflowNotificationChannels: ['email'],
                },
                owners: [],
                device: 'touch',
            },
            cdate: Date.now(),
            workflows: [
                {
                    id: 'a57e954e-0d4b-48e8-b86c-397d9d75c3bb',
                    type: 'main',
                },
                {
                    id: '36027914-4baa-11e7-89a6-0025909427cc',
                    type: 'main',
                },
                {
                    id: 'bccf77dc-3568-11e7-89a6-0025909427cc',
                    type: 'main',
                },
            ],
            'runtime-info': {
                pools: {
                    production: {
                        'project-id': 123,
                        'pool-id': 456,
                    },
                    sandbox: {
                        'project-id': 576,
                        'pool-id': 89,
                    },
                },
            },
            type: 'serp',
            planStats: [
                {
                    'plan-report-link': 'https://sbs.s3.yandex.net/0681d29e3610579d7e3b823f198a4a6e6421cc58eda45500d63c3310aeae4ab3/sbs-101174-plan-report.html',
                    'workflow-id': 'bccf77dc-3568-11e7-89a6-0025909427cc',
                    'workflow-type': 'main',
                },
            ],
        },
        {
            id: 2,
            ticket: 'SIDEBYSIDE-2',
            login: 'gwer',
            params: {
                notificationMode: {
                    preset: 'workflowOnly',
                    workflowNotificationChannels: ['email'],
                },
            },
            cdate: Date.now(),
            workflows: [],
        },
        {
            id: 3,
            ticket: 'SIDEBYSIDE-2',
            login: 'ensuetina',
            params: {
                notificationMode: {
                    preset: 'workflowOnly',
                    workflowNotificationChannels: ['email'],
                },
            },
            cdate: Date.now(),
            workflows: ['6a21ec42-cc3f-43a4-8bdd-6daa4f0427d8'],
        },
    ],

    count() {
        return Promise.resolve(this._data.length);
    },

    createExp() {
        return Promise.resolve({ id: 1 });
    },

    updateExp(id) {
        return Promise.resolve({ id });
    },

    getSystems() {
        return Promise.resolve([]);
    },

    setWorkflowId() {
        return Promise.resolve();
    },

    setStatus() {
        return Promise.resolve();
    },

    setPools() {
        return Promise.resolve();
    },

    getByLogin(login) {
        return Promise.resolve(this._data.find((exp) => exp.login === login));
    },

    /**
     * @return {Stream.Readable}
     */
    abExport() {
        const docs = new Readable;
        docs.on = () => docs;

        this._data.forEach((doc) => docs.push(JSON.stringify(doc)));
        docs.push(null);

        return docs;
    },

    /**
     * @return {Promise}
     */
    abExportPromise() {
        return Promise.resolve(this._data);
    },

    getByQuery() {
        return Promise.resolve(this._data[1]);
    },

    countByQuery() {
        return Promise.resolve(1);
    },

    countByLogin() {
        return Promise.resolve(1);
    },

    getById({ id }) {
        return Promise.resolve(this._data.find((exp) => exp.id === id));
    },

    getQueriesAnalysis() {
        return Promise.resolve();
    },

    getCurrentWorkflowId() {
        return Promise.resolve(null);
    },

    lock(id) {
        return Promise.resolve(this._data.find((exp) => exp.id === id));
    },

    unlock(id) {
        return Promise.resolve(this._data.find((exp) => exp.id === id));
    },

    getByWorkflow(workflowId) {
        return Promise.resolve(this._data.find((exp) => exp.workflows.find((w) => w.id === workflowId)));
    },

    setCommentsAndVotesFiles() {
        return Promise.resolve();
    },

    setExpStats() {
        return Promise.resolve();
    },

    setInternalStatus() {
        return Promise.resolve();
    },
};
