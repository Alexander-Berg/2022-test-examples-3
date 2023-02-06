const {teamcity} = require('../../api');

module.exports = async ({build}) => {
    const data = await teamcity.build(build);
    const {buildTypeId, id, testOccurrences, webUrl} = data;
    const {passed, failed, ignored} = testOccurrences;

    return [
        'Результаты тестов:',
        passed && `!!(green)✓ прошло ${passed}!!`,
        failed && `!!(red)✗ упало ${failed}!!`,
        ignored && `!!(grey)✗ в игноре ${ignored}!!`,
        ' ',
        `((${webUrl} Teamcity build))`,
        `((https://teamcity.yandex-team.ru/repository/download/${buildTypeId}/${id}:id/html-report/index.html html report))`,
    ].filter(Boolean).join('\n');
};
