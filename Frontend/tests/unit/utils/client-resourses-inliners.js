const path = require('path');
const proxyquire = require('proxyquire');

const { getActiveRegionHintJSInlineCode, getSinglePageJSInlineCode } = require('../../../src/server/utils/client-resourses-inliners');

describe('utils/client-resourses-inliners', () => {
    const sandbox = sinon.createSandbox();

    afterEach(() => {
        sandbox.restore();
    });

    it('getJSInlineCode должен возвращать babel-processed содержимое js-файла, обернутое в тэг script', () => {
        const resourcePath = './resources/index.js';
        const resourceAbsolutePath = '/www/resources/index.js';
        const jsFileContent = 'console.log(true);';
        const minifiedAndTransplitedJsFileContent = 'console.log(!0);';
        const testedModuleDirname = path.resolve(__dirname, '../../../src/server/utils');

        const resolve = sandbox.stub()
            .withArgs(testedModuleDirname, resourcePath)
            .returns(resourceAbsolutePath);

        const readFile = sandbox.stub()
            .withArgs(resourceAbsolutePath, 'utf-8')
            .returns(Promise.resolve(jsFileContent));

        const transform = sandbox.stub()
            .withArgs(jsFileContent, {
                presets: ['env', 'minify'],
            })
            .returns({ code: minifiedAndTransplitedJsFileContent });

        const { getJSInlineCode } = proxyquire('../../../src/server/utils/client-resourses-inliners', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
            '@babel/core': {
                transform,
            },
        });

        return getJSInlineCode(resourcePath)
            .then((result) =>
                assert.equal(result, `<script type="text/javascript">${minifiedAndTransplitedJsFileContent}</script>`));
    });

    it('getCSSInlineCode должен возвращать содержимое css-файла, обернутое в тэг style', () => {
        const resourcePath = './resources/index.css';
        const resourceAbsolutePath = '/www/resources/index.css';
        const cssFileContent = '.body{color: red;}';
        const testedModuleDirname = path.resolve(__dirname, '../../../src/server/utils');

        const resolve = sandbox.stub()
            .withArgs(testedModuleDirname, resourcePath)
            .returns(resourceAbsolutePath);

        const readFile = sandbox.stub()
            .withArgs(resourceAbsolutePath, 'utf-8')
            .returns(Promise.resolve(cssFileContent));

        const { getCSSInlineCode } = proxyquire('../../../src/server/utils/client-resourses-inliners', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
        });

        return getCSSInlineCode(resourcePath)
            .then((result) =>
                assert.equal(result, `<style type="text/css">${cssFileContent}</style>`));
    });

    it('getMetrikaJSInlineCode должен возвращать babel-processed код вставки Метрики, обернутый в тэг script', () => {
        const resourcePath = '../html-templates/inline-metrika.js';
        const resourceAbsolutePath = '/www/html-templates/inline-metrika.js';
        const jsFileContent = 'console.log("inline-metrika" + "")';
        const minifiedAndTransplitedJsFileContent = 'console.log("inline-metrika")';
        const testedModuleDirname = path.resolve(__dirname, '../../../src/server/utils');

        const resolve = sandbox.stub()
            .withArgs(testedModuleDirname, resourcePath)
            .returns(resourceAbsolutePath);

        const readFile = sandbox.stub()
            .withArgs(resourceAbsolutePath, 'utf-8')
            .returns(Promise.resolve(jsFileContent));

        const transform = sandbox.stub()
            .withArgs(jsFileContent, {
                presets: ['env', 'minify'],
            })
            .returns({ code: minifiedAndTransplitedJsFileContent });

        const { getMetrikaJSInlineCode } = proxyquire('../../../src/server/utils/client-resourses-inliners', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
            '@babel/core': {
                transform,
            },
        });

        return getMetrikaJSInlineCode()
            .then((result) =>
                assert.equal(result, `<script type="text/javascript">${minifiedAndTransplitedJsFileContent}</script>`));
    });

    it('getTolokaLogJSInlineCode должен возвращать babel-processed код вставки toloka-log, обернутый в тэг script', () => {
        const resourcePath = '../html-templates/inline-toloka-log.js';
        const resourceAbsolutePath = '/www/html-templates/inline-toloka-log.js';
        const jsFileContent = 'console.log("inline-toloka-log" + "")';
        const minifiedAndTransplitedJsFileContent = 'console.log("inline-toloka-log")';
        const testedModuleDirname = path.resolve(__dirname, '../../../src/server/utils');

        const resolve = sandbox.stub()
            .withArgs(testedModuleDirname, resourcePath)
            .returns(resourceAbsolutePath);

        const readFile = sandbox.stub()
            .withArgs(resourceAbsolutePath, 'utf-8')
            .returns(Promise.resolve(jsFileContent));

        const transform = sandbox.stub()
            .withArgs(jsFileContent, {
                presets: ['env', 'minify'],
            })
            .returns({ code: minifiedAndTransplitedJsFileContent });

        const { getTolokaLogJSInlineCode } = proxyquire('../../../src/server/utils/client-resourses-inliners', {
            path: {
                resolve,
            },
            'fs-extra': {
                readFile,
            },
            '@babel/core': {
                transform,
            },
        });

        return getTolokaLogJSInlineCode()
            .then((result) =>
                assert.equal(result, `<script type="text/javascript">${minifiedAndTransplitedJsFileContent}</script>`));
    });

    it('getActiveRegionHintJSInlineCode должен возвращать js-код, который включает синие области в figma-прототипе', () => {
        const expectedResult = '<script type="text/javascript">window.ACTIVE_REGIONS_HINT = true;</script>';
        const result = getActiveRegionHintJSInlineCode(true);

        assert.equal(result, expectedResult);
    });

    it('getSinglePageJSInlineCode должен возвращать js-код, который показывает, что figma-прототип одностраничный', () => {
        const expectedResult = '<script type="text/javascript">window.SINGLE_PAGE = true;</script>';
        const result = getSinglePageJSInlineCode(true);

        assert.equal(result, expectedResult);
    });
});
