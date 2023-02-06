describe("b-edit-group-mobile-content-href", function() {
    // DM - data model
    function createBannerDM(modelParams) {
        modelParams = modelParams || {};
        return BEM.MODEL.create({
            name: 'dm-mobile-content-group'
        },modelParams);
    }

    function createBannerBlock(blockParams) {

        blockParams = blockParams || {};

        var theMods = blockParams.mods ?
                blockParams.mods :
                { 'read-only': 'no' };
        return u.createBlock({
            block: "b-edit-group-mobile-content-href",
            mods: theMods,
            modelName: 'dm-mobile-content-group',
            modelId: '1',
            value: '',
            apps: blockParams.apps || []
        });
    }

    function createAppList() {
        return [
            {
                id: 78542,
                minOsVersion: "4.0",
                mobileContent: {
                    is_available: true,
                    is_show_icon: true,
                    min_os_version: "4.0",
                    os_type: "Android",
                    age_label: "12+"
                },
                mobileContentId: 78542,
                name: "ViNTERA TV",
                primaryAction: "DOWNLOAD",
                store: "GOOGLE_PLAY",
                storeHref: "https://play.google.com/store/apps/details?id=tv.vintera.smarttv.v2&#38;hl=ru"
            }
        ]
    }

    var sandbox, bannerBlock, bannerDataModel;

    describe('Проверка DOM структуры', function() {
        beforeEach(function(done) {
            sandbox = sinon.sandbox.create();
            u.stubCurrencies2(sandbox);
            bannerDataModel = createBannerDM();
            bannerBlock = createBannerBlock();
            done();
        });

        afterEach(function(done) {
            bannerDataModel.destruct();
            bannerBlock.destruct();
            sandbox.restore();
            done();
        });

        ['input', 'spin2','popup2'].forEach(function(block) {
            it('Проверка на наличие блока ' + block, function() {
                expect(bannerBlock).to.haveBlock(block);
            })
        });

        ['numerator'].forEach(function(elem) {
            it('Проверка на наличие элемента ' + elem, function() {
                expect(bannerBlock).to.haveElem(elem);
            });
        });
    });

    describe('Проверка поведения пустого списка приложений при фокусе на input контроле ', function() {

        before(function(done) {
            sandbox = sinon.sandbox.create();
            u.stubCurrencies2(sandbox);
            bannerDataModel = createBannerDM();
            bannerBlock = createBannerBlock();
            done();
        });

        after(function(done) {
            sandbox.restore();
            bannerDataModel.destruct();
            bannerBlock.destruct();
            done();
        });

        it('Список приложений должен быть скрыт', function() {
            bannerBlock.findBlockOn('url', 'input').trigger('focus');
            expect(bannerBlock.findBlockInside('popup2')).to.not.haveMod('visible', 'yes');
        });

    });

    describe('Проверка поведения заполненного списка приложений при фокусе на input контроле ', function() {

        before(function(done) {
            sandbox = sinon.sandbox.create();
            u.stubCurrencies2(sandbox);
            bannerDataModel = createBannerDM();
            bannerBlock = createBannerBlock({'apps': createAppList()});
            done();
        });

        after(function(done) {
            sandbox.restore();
            bannerDataModel.destruct();
            bannerBlock.destruct();
            done();
        });

        it('Список приложений должен быть показан', function() {
            bannerBlock.findBlockOn('url', 'input').trigger('focus');
            expect(bannerBlock.blockInside('popup2')).to.haveMod('visible', 'yes');
        });

    });
});

