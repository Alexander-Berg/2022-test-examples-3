module.exports = {
    service: 'sitesearch-serp-test',
    '@title': 'Sitesearch Serp Test',
    '@iconUrl': 'https://github.yandex-team.ru/images/icons/emoji/unicode/1f4f1.png',
    pipeLinks: [
        {
            label: 'PR-beta',
            url: 'https://pr-{{github_payload.pull_request.number}}.sst.pr.yandex.ru/hamster.yandex.ru/search/site/htmlcss.html',
            qr: true,
        },
    ],
};
