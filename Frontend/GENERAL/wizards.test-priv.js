describeBlock('wizards', function(block) {
    var data, wizArray, pos;
    stubBlocks(['wizards__move-snippets-to-construct', 'wizards__item']);

    beforeEach(function() {
        data = [];
        pos = '';
    });

    it('should call "move-snippet-to-construct" if "wzrd.construct" doesn`t exist', function() {
        wizArray = [{}];
        blocks['wizards__move-snippets-to-construct'].returns({});
        block(data, wizArray, pos);

        assert.called(blocks['wizards__move-snippets-to-construct']);
    });

    describe('return', function() {
        it('should return undefined if wizArray is undefined', function() {
            wizArray = undefined;
            assert.isUndefined(block(data, wizArray, pos));
        });

        it('should return undefined if wizArray is not array', function() {
            wizArray = { foo: 'bar' };
            assert.isUndefined(block(data, wizArray, pos));
        });

        it('should return undefined if wizArray is empty array', function() {
            wizArray = [];
            assert.isUndefined(block(data, wizArray, pos));
        });

        it('should return undefined if wizArray is array of nonsense', function() {
            wizArray = [{ foo: 'bar' }];
            assert.isUndefined(block(data, wizArray, pos));
        });

        it('should return array if wizArray is not empty array', function() {
            blocks['wizards__item'].returns({
                block: 'serp-item',
                content: {}
            });

            wizArray = [{
                counter_prefix: '/wiz/pseudo_fast/yandex_browser_mobile/',
                data: {},
                serp_info: {
                    subtype: 'yandex_browser_mobile',
                    template: 'browser',
                    format: 'json',
                    type: 'pseudo_fast'
                },
                subtype: 'yandex_browser_mobile',
                template: 'browser',
                type: 'pseudo_fast',
                types: { kind: 'wizard', all: ['snippets', 'pseudo_fast'], main: 'pseudo_fast' },
                wizkey: 'browsers__browsers_mobile__ru',
                wizplace: 'important'
            }];

            assert.isArray(block(data, wizArray, pos));
        });
    });
});

describeBlock('wizards__move-snippets-to-construct', function(block) {
    var wzrd, pos;

    beforeEach(function() {
        wzrd = { type: '' };
        pos = '';
    });

    it('should not push wizards data to construct if "wzrd.type" doesn`t exist in whiteList[pos]', function() {
        wzrd.type = 'some-wiz-type';
        pos = 'important';
        block(wzrd, pos);

        assert.isUndefined(wzrd.construct);
    });

    it('should not push wizards data to construct if "wzrd.type" is undefined', function() {
        wzrd.type = undefined;
        pos = 'important';
        block(wzrd, pos);

        assert.isUndefined(wzrd.construct);
    });

    it('should push wizards data to construct if "wzrd.type" exists in whiteList[pos]', function() {
        wzrd.type = 'companies';
        pos = 'important';
        block(wzrd, pos);

        assert.deepEqual(wzrd.construct, wzrd);
    });

    it('should push wizards data to construct if "wzrd.type" exists in whiteList[no_pos]', function() {
        wzrd.type = 'math';
        block(wzrd, pos);

        assert.deepEqual(wzrd.construct, wzrd);
    });
});

describeBlock('wizards__item', function(block) {
    var context, wzrd;

    stubBlocks([
        'construct__context_for_wizard',
        'construct__dataset',
        'construct',
        'related',
        'serp-item-counters'
    ]);

    beforeEach(function() {
        context = { reportData: {}, expFlags: {} };
        blocks['construct__dataset'].returns({
            context: {},
            dataset: {}
        });
        blocks['construct'].returns({
            block: 'serp-item',
            content: {}
        });
    });

    // формирование bemjson и вызов счетчика
    it('should call blocks["construct"] and counter if construct exists in wzrd', function() {
        wzrd = {
            construct: [{
                type: 'entity-fact'
            }],
            pos: 'important',
            type: 'entity-fact'
        };
        block(context, wzrd);

        assert.calledOnce(blocks['construct']);
        assert.calledOnce(blocks['serp-item-counters']);
    });
});
