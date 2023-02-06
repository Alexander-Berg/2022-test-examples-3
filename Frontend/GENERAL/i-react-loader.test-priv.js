describeBlock('i-react-loader__configure-assets', function(block) {
    let glob;

    let assets;
    let clonedAssets;

    let resourceJs;
    let inlineJs;
    let inlineCss;

    beforeEach(function() {
        glob = stubGlobal('RequestCtx');
        resourceJs = { name: 'asset', js: { url: 'https://yandex.ru' } };
        inlineJs = { name: 'inline-js', js: 'console.log(false)' };
        inlineCss = { name: 'inline-js', css: 'body { color: red; }' };

        assets = [resourceJs, inlineJs, inlineCss];
        clonedAssets = _.cloneDeep(assets);
    });

    // Проверяем что изначальный объект не меняется
    afterEach(function() {
        assert.deepEqual(assets, clonedAssets);
        glob.restore();
    });

    it('should return not modified assets', function() {
        const simpleAssets = [
            { name: 'asset', js: { url: 'https://yandex.ru' } },
            { name: 'inline-js', js: 'console.log(false)' },
            { name: 'inline-js', css: 'body { color: red; }' }
        ];

        assert.deepEqual(block(simpleAssets), simpleAssets);
    });

    it('should return inline assets', function() {
        const inlineAssets = block([
            { name: 'asset', js: { content: 'console.log(true)' } },
            inlineJs,
            inlineCss
        ]);

        assert.deepEqual(
            inlineAssets,
            [
                { name: 'asset', js: 'console.log(true)' },
                inlineJs,
                inlineCss
            ]
        );
    });
});
