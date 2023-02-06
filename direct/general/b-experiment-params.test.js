describe('b-experiment-params', function() {
    var camp1 = { name: 'Компания 1', cid: '111' },
        camp2 = { name: 'Компания 2', cid: '222' },
        camp3Disabled = { name: 'Компания 3', cid: '333', experiment_id: '100' },
        block,
        createBlock = function(campaigns) {
            block = u.createBlock({
                block: 'b-experiment-params',
                campaigns: campaigns
            });
        },
        sandbox;

     beforeEach(function() {
         sandbox = sinon.sandbox.create({ useFakeServer: true, useFakeTimers: true });
     });

    afterEach(function() {
        block.model.destruct();
        block.destruct();
        sandbox.restore();
    });

    describe('Обновление списка кампаний при изменении модели', function() {

        it('При изменении primaryCid обновляются элементы селекта кампании B', function() {
            var campaigns = [camp1, camp2],
                select;

            createBlock(campaigns);

            select = block.findBlocksInside('campaign-select', 'b-campaigns-select')[1];

            sandbox.spy(select, 'setCampaigns');
            sandbox.stub(u['b-experiment-params'], 'getDisabledCampaigns')
                .withArgs(campaigns, camp2.cid)
                .returns('moo');

            block.model.set('primaryCid', camp2.cid);

            expect(select.setCampaigns.calledOnce).to.be.equal(true);
            expect(select.setCampaigns.getCall(0).args[0]).to.be.eql(campaigns);
            expect(select.setCampaigns.getCall(0).args[1]).to.be.eql({ disabledIds: 'moo', addEmpty: true });
        });

        it('При изменении secondaryCid обновляются элементы селекта кампании A', function() {
            var campaigns = [camp1, camp2],
                select;

            createBlock(campaigns);

            select = block.findBlocksInside('campaign-select', 'b-campaigns-select')[0];

            sandbox.spy(select, 'setCampaigns');
            sandbox.stub(u['b-experiment-params'], 'getDisabledCampaigns')
                .withArgs(campaigns, camp2.cid)
                .returns('moo');

            block.model.set('secondaryCid', camp2.cid);

            expect(select.setCampaigns.calledOnce).to.be.equal(true);
            expect(select.setCampaigns.getCall(0).args[0]).to.be.eql(campaigns);
            expect(select.setCampaigns.getCall(0).args[1]).to.be.eql({ disabledIds: 'moo', addEmpty: true });
        });
    });

    describe('Формирование списка заблокированных cid', function() {

        it('Блокируется кампания с переданным cid', function() {
            expect(u['b-experiment-params'].getDisabledCampaigns([camp1], camp1.cid)).to.be.eql([camp1.cid])
        });

        it('Блокируется кампания с заполненным id эксперимента', function() {
            expect(u['b-experiment-params'].getDisabledCampaigns([camp3Disabled])).to.be.eql([camp3Disabled.cid])
        });
    });

    describe('Расчет процентной доли кампаний', function() {

        it('при изменении доли первой кампании пересчитывается доля второй кампании', function() {
            createBlock([]);

            block.model.set('primaryPercent', 12);

            expect(block.model.get('secondaryPercent')).to.be.equal(88);
        });

        it('при изменении доли второй кампании пересчитывается доля первой кампании', function() {
            createBlock([]);

            block.model.set('secondaryPercent', 33);

            expect(block.model.get('primaryPercent')).to.be.equal(67);
        });
    });

    describe('Отправка запроса на создание нового эксперимента', function() {

        it('Передаются корректные данные', function() {

            createBlock([camp1, camp2]);

            block.model.update({ primaryCid: camp2.cid, secondaryCid: camp1.cid, primaryPercent: 1, secondaryPercent: 99 });

            sandbox.stub(block.findBlockOn('calendar', 'b-date-range-picker'), 'getRange')
                .returns({ start: 'xxx', finish: 'zzz' });

            block.findBlockOn('submit', 'button').trigger('click');

            sandbox.clock.tick(1);

            expect(sandbox.server.requests[0].requestBody).to.be.equal('cmd=ajaxCreateExperiment&primary_cid=222&secondary_cid=111&percent=1&date_from=xxx&date_to=zzz');
        });
    });

});
