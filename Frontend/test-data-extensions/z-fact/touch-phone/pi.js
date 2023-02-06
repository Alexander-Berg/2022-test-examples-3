module.exports = {
    type: 'wizard',
    request_text: 'число пи',
    data_stub: [
        {
            num: '0',
            snippets: {
                main: {
                    template: 'generic',
                    type: 'generic'
                }
            },
            server_descr: 'SUGGESTFACTS2',
            construct: {
                type: 'suggest_fact',
                baobab: {
                    path: '/snippet/suggest_fact'
                },
                question: 'Число Пи',
                text: '3,1415926535…'
            },
            markers: {
                WizardPos: '0'
            }
        }
    ]
};
