module.exports = {
    create: Object.assign({
        title: 'test ticket',
        description: 'test description',
    }, getGeneral()),

    update: Object.assign({
        title: 'updated ticket',
        description: 'updated description',
        assignee: 'updatedUser',
    }, getGeneral()),
};

function getGeneral() {
    return {
        goldenset: [{
            engine: 'yandex-web',
            query: 'test',
            cgi: '',
            'w-exps-flags': '',
        }],
        'd-exps-flags': '',
        region: 'ru',
        device: 'tablet',
        options: [],
        tasksuiteComposition: {
            mode: 'custom',
            val: {
                goodTasksCount: '0',
                badTasksCount: '0',
                assignmentsAcceptedCount: '0',
            },
        },
        cross: 0,
        customCross: [],
    };
}
