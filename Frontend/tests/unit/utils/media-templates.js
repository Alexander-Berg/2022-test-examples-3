const path = require('path');
const proxyquire = require('proxyquire');

describe('utils/media-templates', function() {
    const sandbox = sinon.createSandbox();

    afterEach(function() {
        sandbox.restore();
    });

    it('возвращает html-разметку для аудио-файлов', function() {
        const template = '<html><audio><source src="{SOURCE}" type="{TYPE}"></audio></html>';
        const expected = '<html><script>console.log("metrika");console.log("tolokaLog")<script><audio><source src="test.mp3" type="audio/mp3"></audio></html>';
        const fileName = 'test.mp3';
        const fileMIMEType = 'audio/mp3';

        const dirnameForTestedModule = path.resolve(__dirname, '../../../src/server/utils');
        const resolve = sandbox.stub()
            .withArgs(dirnameForTestedModule, '../html-templates/audio.html')
            .returns('/Users/temp-user/temp-path');
        const readFile = sandbox.stub()
            .withArgs('/Users/temp-user/temp-path', 'utf8')
            .returns(Promise.resolve(template));
        const inlineMetrikaAndTolokaLogInHtmlString = sandbox.stub()
            .withArgs('<html><audio><source src="test.mp3" type="audio/mp3"></audio></html>')
            .returns(Promise.resolve(expected));

        const { getHTMLString } = proxyquire('../../../src/server/utils/media-templates', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
            './client-resourses-inliners': {
                inlineMetrikaAndTolokaLogInHtmlString,
            },
        });

        return getHTMLString(fileName, fileMIMEType)
            .then((result) => {
                assert.equal(result, expected);
            });
    });

    it('возвращает html-разметку для видео-файлов', function() {
        const template = '<html><video><source src="{SOURCE}" type="{TYPE}"></video></html>';
        const expected = '<html><script>console.log("metrika");console.log("tolokaLog")<script><video><source src="test.mp4" type="video/mp4"></video></html>';
        const fileName = 'test.mp4';
        const fileMIMEType = 'video/mp4';

        const dirnameForTestedModule = path.resolve(__dirname, '../../../src/server/utils');
        const resolve = sandbox.stub()
            .withArgs(dirnameForTestedModule, '../html-templates/video.html')
            .returns('/Users/temp-user/temp-path');
        const readFile = sandbox.stub()
            .withArgs('/Users/temp-user/temp-path', 'utf8')
            .returns(Promise.resolve(template));
        const inlineMetrikaAndTolokaLogInHtmlString = sandbox.stub()
            .withArgs('<html><audio><video src="test.mp4" type="video/mp4"></video></html>')
            .returns(Promise.resolve(expected));

        const { getHTMLString } = proxyquire('../../../src/server/utils/media-templates', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
            './client-resourses-inliners': {
                inlineMetrikaAndTolokaLogInHtmlString,
            },
        });

        return getHTMLString(fileName, fileMIMEType)
            .then((result) =>
                assert.equal(result, expected));
    });

    it('генерирует ошибку для не медиа файла', function() {
        const { getHTMLString } = require('../../../src/server/utils/media-templates');
        return getHTMLString('test.txt', 'text/plain')
            .catch((error) => assert.equal(error.message, 'Unknown media type'));
    });
});
