const { ROBOT_ID } = require('./constant');
const { wrapToSpoiler, unwrapFromSpoiler, findCostResultComments, parseVersions, buildCommentData, getCommentData } = require('./startrek');

const makeComment = pkgName => {
    let text = '';
    if (pkgName) {
        text = buildCommentData(
            'ISL-1111',
            42,
            JSON.stringify({ name: pkgName, some: 'data', 'components-cost-development': {} }),
        );
    } else {
        text = 'some random text bla bla { "json": true }';
    }

    return { createdBy: { id: pkgName ? ROBOT_ID : 123 }, text };
};

describe('startrek', () => {
    it('cut and uncut text', () => {
        const text = 'some long text \n \n { "json": true }';

        expect(unwrapFromSpoiler(wrapToSpoiler(text))).toEqual(text);
    });

    it('build comment data', () => {
        const data = JSON.stringify({ data: true });

        expect(buildCommentData('ISL-1111', 42, data)).toMatchSnapshot();
    });

    it('comment data format parse', () => {
        const data = JSON.stringify({ data: true });

        expect(getCommentData(buildCommentData('ISL-1111', 42, data))).toEqual(data);
    });

    it('find cost result comment for package', () => {
        const comments = [
            makeComment('@yandex-lego/components'),
            makeComment(),
            makeComment('@yandex-int/draft-components'),
            makeComment(),
        ];

        const found = findCostResultComments(comments, '@yandex-lego/components');
        expect(found).toEqual([makeComment('@yandex-lego/components')]);
    });

    it('find all cost result comments', () => {
        const comments = [
            makeComment('@yandex-lego/components'),
            makeComment(),
            makeComment('@yandex-int/draft-components'),
            makeComment(),
        ];

        const found = findCostResultComments(comments);
        expect(found).toEqual([makeComment('@yandex-lego/components'), makeComment('@yandex-int/draft-components')]);
    });

    it('parse versions', () => {
        const fixVersions = [
            {
                self: 'https://st-api.yandex-team.ru/v2/versions/183717',
                id: '183717',
                display: 'frontend-ci@1.30.2',
            },
            {
                self: 'https://st-api.yandex-team.ru/v2/versions/183716',
                id: '183716',
                display: '@yandex-int/iver-next@0.11.2',
            },
            {
                self: 'https://st-api.yandex-team.ru/v2/versions/183786',
                id: '183786',
                display: '@yandex-lego/components@1.30.3',
            },
        ];

        expect(parseVersions(fixVersions)).toEqual([
            { name: 'frontend-ci', value: '1.30.2' },
            { name: '@yandex-int/iver-next', value: '0.11.2' },
            { name: '@yandex-lego/components', value: '1.30.3' },
        ]);
    });
});
