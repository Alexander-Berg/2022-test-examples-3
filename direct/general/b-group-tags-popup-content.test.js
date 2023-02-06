describe('b-group-tags-popup-content', function() {
    var campaignModel,
        bannerGroupModel,
        popup,
        createModels = function(campTags, groupTags) {
            bannerGroupModel = BEM.MODEL.create({ name: 'm-group', id: 'banner' }, { tags: groupTags, cid: 'campaign' });
            campaignModel = BEM.MODEL.create(
                { name: 'm-campaign', id: 'campaign' },
                { tags: campTags, adgroupsIds: ['banner'], mediaType: 'text' });
        },
        getPopup = function(params) {
            return $(BEMHTML.apply($.extend({
                block: 'b-group-tags-popup-content',
                modelParams: {
                    name: 'm-group',
                    id: 'banner'
                }
            }, params))).appendTo($('body')).bem('b-group-tags-popup-content');
        },
        makeTags = function(count) {
            var result = [];

            for (var i = 0; i < count; i++) {
                result.push({
                    id: i + '',
                    value: i + ''
                });
            }

            return result;
        };

    before(function() {
        u.stubDMParams();
    });

    after(function() {
        u.restoreConsts();
    });

    afterEach(function() {
        campaignModel.destruct();
        bannerGroupModel.destruct();
        popup.destruct();
    });

    //DIRECT-28490
    it('должен давать добавлять объявлению существующую в кампании метку, когда меток в кампании - 200', function() {
        var tags = makeTags(200);
        createModels(tags, []);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }
        });

        popup.findBlockInside('new-tags', 'input').val('0');

        expect(popup.elem('errors').text()).to.be.equal('');
    });

    it('не должен давать добавлять объявлению не существующую в кампании метку, когда меток в кампании - 200', function() {
        var tags = makeTags(200);
        createModels(tags, []);
        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }
        });

        popup.findBlockInside('new-tags', 'input').val('200');

        expect(popup.elem('errors').text()).to.have.string('Нельзя создавать больше 200 меток');
    });

    //DIRECT-28422
    it('не должен давать задавать объявлению больше 30 меток', function() {
        var tags = makeTags(30);

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        popup.findBlockInside('new-tags', 'input').val('30');

        expect(popup.elem('errors').text()).to.have.string('Нельзя устанавливать на баннер больше 30 меток');
    });

    //DIRECT-28479
    it('по нажатию enter в поле ввода меток не должен отправлять запрос на сохранение при наличии ошибок валидации', function() {
        var tags = makeTags(30);

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        popup.outboard = { //todo: не обращаться в попапе к outboard, тестировать через stub jquery.ajax
            accept: sinon.spy()
        };

        var input = popup.findBlockInside('new-tags', 'input'),
            e = $.Event('keypress');

        e.which = 13; // Enter
        input.val('30');

        input.elem('control').trigger(e);

        expect(popup.outboard.accept.called).to.be.equal(false);
    });

    it('по нажатию enter в поле ввода меток должен отправлять запрос на сохранение', function() {
        var tags = makeTags(10);

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        popup.outboard = { //todo: не обращаться в попапе к outboard, тестировать через stub jquery.ajax
            accept: sinon.spy()
        };

        var input = popup.findBlockInside('new-tags', 'input').elem('control'),
            e = $.Event('keypress');

        e.which = 13; // Enter
        input.trigger(e);

        expect(popup.outboard.accept.called).to.be.equal(true);
    });

    it('должен отображать метки в алфавитном порядке', function() {
        var tags = ['N', '№', 'O'].map(function(value) { return { id: value, value: value }; });

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        popup.prepareToShow(['banner']);

        expect(popup.elem('checkbox-label').map(function() { return $(this).text(); }).toArray()).to.be.eql(['N', 'O', '№']);
    });

    it.skip('не должен учитывать регистр меток', function() {
        var tags = [];

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        sinon.stub($, 'ajax').returns($.Deferred());

        var input = popup.findBlockInside('new-tags', 'input');

        input.val('метка,Метка');
        popup.provideData();

        expect($.ajax.calledWithMatch({
            new_tags: 'метка'
        })).to.be.equal(true);
    });

    //DIRECT-25699
    it('не должен добавлять метку, если введенная в поле ввода метка уже существует', function() {
        var tags = [{ id: 'id', value: '123' }];

        createModels(tags, tags);

        popup = getPopup({
            adgroupIds: ['banner'],
            cid: 'campaign',
            campaignTags: JSON.stringify(tags),
            ulogin: 'svarka74-dextra',
            modelParams: {
                id: 'campaign',
                name: 'm-campaign'
            }

        });

        sinon.stub($, 'ajax').returns($.Deferred());

        var input = popup.findBlockInside('new-tags', 'input');

        input.val('123');
        popup.provideData();

        var callData = $.ajax.firstCall.args[0].data;

        expect(callData).to.have.property('tags_ids', 'id');
        expect(callData.new_tags).to.be.undefined;
    });
});
