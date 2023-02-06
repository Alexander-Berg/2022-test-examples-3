describeBlock('misspell__messages', function(block) {
    var data,
        result;

    stubBlocks(
        'misspell_type_anti-mirror',
        'misspell_type_error',
        'misspell_type_oblivion',
        'misspell_type_region__show'
    );

    beforeEach(function() {
        data = stubData('experiments');
        data.wizplaces = [];
        data.searchdata = {};
        data.query = {};
        data.localization = {};
    });

    function checkMapping(from, to, addict = {}) {
        describe('if "' + from + '" wizard found ', function() {
            beforeEach(function() {
                sinon.stub(blocks, 'misspell_type_' + to).returns({ content: to });

                if (from === 'region') {
                    blocks['misspell_type_region__show'].returns(true);
                }
            });

            afterEach(function() {
                blocks['misspell_type_' + to].restore();

                if (from === 'region') {
                    blocks['misspell_type_region__show'].restore();
                }
            });

            it('should add "' + to + '" message', function() {
                data.wizplaces.upper = [
                    { type: from, ...addict }
                ];

                result = block(data);

                assert.deepInclude(result, { type: from, content: to });
            });
        });
    }

    checkMapping('web_misspell', 'misspell');
    checkMapping('misspell_source', 'misspell');
    checkMapping('old_syntax', 'misspell');
    checkMapping('tasix', 'tasix');
    checkMapping('request_filter', 'longcat');
    checkMapping('minuswords', 'minusword');
    checkMapping('minuswords_obsolete', 'minusword');
    checkMapping('reask', 'reask', { show_message: 1 });
    checkMapping('region', 'region');

    describe('should be in strict order', function() {
        beforeEach(function() {
            // добавляем все возможные опечаточники
            blocks['misspell_type_error'].returns({ content: 'error' });
            blocks['misspell_type_oblivion'].returns({ content: 'oblivion' });

            sinon.stub(blocks, 'misspell_type_misspell').returns({ content: 'misspell' });
            sinon.stub(blocks, 'misspell_type_region').returns({ content: 'region' });
            sinon.stub(blocks, 'misspell_type_reask').returns({ content: 'reask' });
            blocks['misspell_type_region__show'].returns(true);

            data.wizplaces.upper = [
                { type: 'web_misspell' },
                { type: 'service_redirect' },
                { type: 'oblivion' },
                { type: 'reask', show_message: 1 },
                { type: 'region' }
            ];

            data.tld = 'ru';
            data.query.text = true;
            data.searchdata.err_text = false;
            data.localization.top = true;
        });

        afterEach(function() {
            blocks['misspell_type_misspell'].restore();
            blocks['misspell_type_region__show'].restore();
        });

        it('list', function() {
            assert.deepEqual(block(data), [
                { type: 'error', content: 'error' },
                { type: 'web_misspell', content: 'misspell' },
                { type: 'reask', content: 'reask' },
                { type: 'region', content: 'region' },
                { type: 'oblivion', content: 'oblivion' }
            ]);
        });
    });

    it('should be return object for top and bottom messages', function() {
        blocks['misspell_type_anti-mirror'].returns({ content: 'anti-mirror' });
        blocks['misspell_type_oblivion'].returns({ content: 'oblivion' });

        data.wizplaces.upper = [
            { type: 'oblivion' }
        ];
        // данные для anti-mirror
        data.misc_data = { antispam: [{ type: 'rurkn' }] };

        assert.deepEqual(
            block(data),
            {
                top: [
                    { type: 'oblivion', content: 'oblivion' }
                ],
                bottom: [
                    { type: 'antispam', content: 'anti-mirror' }
                ]
            }
        );
    });
});
