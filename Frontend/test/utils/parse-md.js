const { parseWikiMd } = require('../..');

const parseMdKeepPosition = text => parseWikiMd(text, {
    remark: {
        markdown: {
            commonmark: true,
        },
        woofmd: {
            tracker: {
                url: 'https://st.yandex-team.ru',
                aliases: ['https://jira.yandex-team.ru'],
            },
            actions: [
                { name: 'a' },
                { name: 'anchor' },
                { name: 'grid' },
                { name: 'linkstree' },
                { name: 'iframe' },
            ],
        },
    },
});

exports.parseMdKeepPosition = parseMdKeepPosition;
