describeBlock('serp-item__move-snippets-to-construct', function(block) {
    it('should move specified snippet types to construct', function() {
        const context = {};
        const doc = {
            snippets: {
                pre: [{ type: 'market' }, { type: 'ignored1' }],
                main: [{ type: 'ignored2' }, { type: 'recipe' }],
                inline_pre: [{ type: 'special_dates' }, { type: 'ignored3' }],
                full: [{ type: 'ignored4' }, { type: 'entity_search' }],
                post: [{ type: 'bno' }, { type: 'ignored5' }]
            }
        };

        block(context, doc);

        assert.deepEqual(
            doc.construct,
            [
                { type: 'market' },
                { type: 'recipe' },
                { type: 'special_dates' },
                { type: 'entity-card', subtype: 'entity_search' },
                { type: 'bno' }
            ]
        );
    });
});

describeBlock('serp-item__log-empty-result', function(block) {
    it('should log error with a message containing the document type', function() {
        const context = {
            expFlags: {},
            reportData: {}
        };
        const pair = {
            context: context,
            dataset: {
                type: 'special_dates',
                _multipleAdaptersData: [
                    { context, dataset: { type: 'special_dates' } },
                    { context, dataset: { type: 'entity-card', subtype: 'entity_search' } }
                ]
            }
        };

        RequestCtx.Logger.reportError.reset();

        block(context, pair);

        assert.calledOnce(RequestCtx.Logger.reportError);

        const errArg = RequestCtx.Logger.reportError.firstCall.args[0];
        assert.instanceOf(errArg, Error);
        assert.include(errArg.message, 'special-dates');
        assert.include(errArg.message, 'entity-card/entity_search');
    });
});
