const resourcesInliners = require('../../../src/server/utils/client-resourses-inliners');
const { generateSimpleHtml, generateHtmlWithMetrikaAndTolokaLogLib } = require('../../../src/server/utils/generate-figma-template');

describe('utils/generate-figma-template', () => {
    const sandbox = sinon.createSandbox();

    afterEach(function() {
        sandbox.restore();
    });

    it('generateSimpleHtml должен возвращать HTML-шаблон для Figma без кода вставки Метрики и toloka-log', () => {
        const jsTemplate = '<script type="text/javascript">console.log(true);</script>';
        const cssTemplate = '<style type="text/css">body{margin: 0}</style>';
        const expected = `<!DOCTYPE html>
            <html lang="ru">
                <head>
                    <title>..</title>
                    <meta charset="UTF-8">
                    ${jsTemplate}${cssTemplate}
                </head>
                    {{DYNAMIC_CONTENT}}
            </html>`;

        sandbox.spy(resourcesInliners, 'inlineMetrikaInHtmlString');
        sandbox.spy(resourcesInliners, 'inlineTolokaLogInHtmlString');

        sandbox.stub(resourcesInliners, 'getJSInlineCode')
            .withArgs('../html-templates/figma-resource/index.js')
            .returns(jsTemplate);

        sandbox.stub(resourcesInliners, 'getCSSInlineCode')
            .withArgs('../html-templates/figma-resource/index.css')
            .returns(cssTemplate);

        return generateSimpleHtml()
            .then((result) => {
                assert.notCalled(resourcesInliners.inlineMetrikaInHtmlString);
                assert.notCalled(resourcesInliners.inlineTolokaLogInHtmlString);
                assert.equal(result, expected);
            });
    });

    it('generateHtmlWithMetrikaAndTolokaLogLib должен возвращать HTML-шаблон для Figma с кодом вставки Метрики и toloka-log', () => {
        const jsTemplate = '<script type="text/javascript">console.log(true);</script>';
        const cssTemplate = '<style type="text/css">body{margin: 0}</style>';
        const metrikaInlineCode = '<script type="text/javascript">console.log("inline-metrika");</script>';
        const tolokaLogInlineCode = '<script type="text/javascript">console.log("inline-toloka-log");</script>';
        const expected = `<!DOCTYPE html>
            <html lang="ru">
                <head>${tolokaLogInlineCode}${metrikaInlineCode}
                    <title>..</title>
                    <meta charset="UTF-8">
                    ${jsTemplate}${cssTemplate}
                </head>
                    {{DYNAMIC_CONTENT}}
            </html>`;

        sandbox.stub(resourcesInliners, 'inlineMetrikaInHtmlString').callsFake((html) => html.replace('<head>', '<head>' + metrikaInlineCode));
        sandbox.stub(resourcesInliners, 'inlineTolokaLogInHtmlString').callsFake((html) => html.replace('<head>', '<head>' + tolokaLogInlineCode));

        sandbox.stub(resourcesInliners, 'getJSInlineCode')
            .withArgs('../html-templates/figma-resource/index.js')
            .returns(jsTemplate);

        sandbox.stub(resourcesInliners, 'getCSSInlineCode')
            .withArgs('../html-templates/figma-resource/index.css')
            .returns(cssTemplate);

        return generateHtmlWithMetrikaAndTolokaLogLib()
            .then((result) => assert.equal(result, expected));
    });
});
