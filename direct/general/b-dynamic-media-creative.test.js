describe('b-dynamic-media-creative', function() {
    var block,
        sandbox;

    function getCreativeStub(data) {
        return {
            business_type: 'test',
            creative_id: 11,
            name: 'name',
            creative_group_id: data.creative_group_id || '0',
            width: 100,
            height: 100,
            preview_scale: 1,
            altText: 'alt_text',
            href: 'data.href',
            geo_names: 'data.geo_names',
            status: 'data.status_moderate',
            preview_url: 'data.preview_url',
            rejectReasons: data.rejection_reason_ids || [],
            campaigns: data.campaigns || [],
            bs_template_name: 'data.bs_template_name'
        }
    }

    function createBlock(data) {
        data = data || {};

        return u.createBlock({
            block: 'b-dynamic-media-creative',
            notEditable: data.notEditable,
            creative: getCreativeStub(data)
        }, { inject: true });
    }

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });

        var constsStub = sandbox.stub(u, 'consts');

        constsStub.withArgs('SCRIPT_OBJECT').returns({
            host: 'direct.yandex.ru'
        });
    });

    afterEach(function() {
        sandbox.restore();
        block && block.destruct();
    });

    [true, false].forEach(function(bool) {
        it('Если в блок передан флаг notEditable = ' + bool + ' то блок с контроллами ' + (bool ? 'не ' : '') + 'отрисовывается', function() {
            block = createBlock({
                notEditable: bool
            });

            if (bool) {
                expect(block).not.to.haveElem('controls');
            } else {
                expect(block).to.haveElem('controls');
            }
        });
    });

    it('Если у креатива creative_group_id = 0 то блок должен иметь модификатор deprecated_yes', function() {
        block = createBlock({
            notEditable: false
        });

        expect(block).to.haveMod('deprecated', 'yes');
    });

    it('Если у креатива creative_group_id = 0 то у блока есть элемент deprecated-icon', function() {
        block = createBlock({
            notEditable: false
        });

        expect(block).to.haveElem('deprecated-icon');
    });

    it('Если у креатива creative_group_id != 0 то блок не должен иметь модификатора deprecated', function() {
        block = createBlock({
            creative_group_id: '100500'
        });

        expect(block).not.to.haveMod('deprecated');
    });

});
