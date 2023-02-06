describeBlock('cloaker', function(block) {
    var jsParams = { counter: {}, ajaxKey: 'distr-smartbanner' },
        assets;

    function getSmartbanner() {
        return {
            block: 'distr-smartbanner',
            mods: { type: 'atom', type2: 'mota' },
            mix: [
                { block: 'grid', mods: { 'pad-x': 'yes' } },
                { block: 'flex-none' }
            ],
            js: jsParams,
            content: [
                { block: 'distr-smartbanner', elem: 'icon' },
                {
                    block: 'distr-smartbanner',
                    elem: 'content',
                    content: [
                        { block: 'distr-smartbanner', elem: 'button' },
                        { block: 'link' }
                    ]
                }
            ]
        };
    }

    beforeEach(function() {
        assets = {
            css: '.distr-smartbanner {} .distr-smartbanner_type2_mota {} .distr-smartbanner_type_atom {} ' +
            '.distr-smartbanner__button {}',
            js: 'BEM.DOM.decl({ block: \'distr-smartbanner\', modName: \'type\', modVal: \'atom\' }, ' +
            '{ init: function() { var smartInfo = \'distr-smartbanner\' }};'
        };

        RequestCtx.GlobalContext.getHash = function(str) {
            /*jslint bitwise: true */
            let hashValue = 0x811c9dc5;
            let index;

            for (index = 0; index < str.length; ++index) {
                hashValue ^= str.charCodeAt(index);
                hashValue +=
                    (hashValue << 1) + (hashValue << 4) + (hashValue << 7) + (hashValue << 8) + (hashValue << 24);
            }

            return hashValue >>> 0;
            /*jslint bitwise: false */
        };
    });

    describe('should correctly replace block name and mods', function() {
        var smartBanner;

        // на входном bemjson blocks['cloaker__get-random-string'] позовётся 5 раз
        beforeEach(function() {
            var randomStringPattern = 'random',
                numberOfRandomCalls = 4;

            smartBanner = getSmartbanner();

            sinon.stub(blocks, 'cloaker__get-random-string');

            for (var call = 0; call <= numberOfRandomCalls; call++) {
                blocks['cloaker__get-random-string'].onCall(call).returns(randomStringPattern + call);
            }

            sinon.stub(blocks, 'cloaker__get-salt').returns('salt');
        });

        afterEach(function() {
            blocks['cloaker__get-random-string'].restore();
            blocks['cloaker__get-salt'].restore();
        });

        it('in BEMJSON', function() {
            var result = block(smartBanner, assets);

            assert.deepEqual(result, {
                block: 'random0',
                mods: { type: 'atom', type2: 'mota' },
                salt: 'salt',
                mix: [
                    { block: 'grid', mods: { 'pad-x': 'yes' } },
                    { block: 'flex-none' }
                ],
                js: true,
                content: [
                    { block: 'random0', elem: 'icon' },
                    {
                        block: 'random0',
                        elem: 'content',
                        content: [
                            { block: 'random0', elem: 'button' },
                            { block: 'link' }
                        ]
                    }
                ]
            });
        });

        it('in CSS and JS', function() {
            block(smartBanner, assets);

            assert.deepEqual(assets, {
                css: '.random0 {} .random0_type2_mota {} .random0_type_atom {} .random0__button {}',
                js: 'BEM.DOM.decl({ block: \'random0\', modName: \'type\', modVal: \'atom\' }, ' +
                '{ init: function() { var smartInfo = \'random0\' }}; ' +
                'BEM.DOM.decl(\'random0\', {}, { getParams: function() { return ' +
                JSON.stringify(_.extend(jsParams, { salt: 'salt' })) + '; } });'
            });
        });
    });

    it('should return always different block name', function() {
        var result = block(getSmartbanner()),
            result2 = block(getSmartbanner());

        assert.isTrue(result.block !== result2.block);
    });

    it('should correctly restore class name from salt', function() {
        var salt = 'randomSalt',
            result,
            result2;

        sinon.stub(blocks, 'cloaker__get-salt').returns(salt);

        result = block(getSmartbanner());

        blocks['cloaker__get-salt'].restore();

        result2 = block(getSmartbanner(), undefined, salt);

        assert.deepEqual(result, result2);
    });

    it('should generate valid css classes with letters at the beginning', function() {
        var result;

        // такая соль в сочетании с 'distr-smartbanner' вернёт нам в итоге строку начинающуся с цифты – 3d3gul
        sinon.stub(blocks, 'cloaker__get-salt').returns(123123);

        result = block(getSmartbanner());

        assert.isTrue(new RegExp('[a-z]').test(result.block[0]));

        blocks['cloaker__get-salt'].restore();
    });
});
