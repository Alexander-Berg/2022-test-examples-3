describe('dm-mobile-content-banner', function() {
    var dataModel;

    describe('Проверка API', function() {
        before(function(){
            dataModel = BEM.MODEL.create('dm-mobile-content-banner');
        });
        after(function(){
            dataModel.destruct();
        });
        [
            'getGroup', 'provideData', 'setHashFlagsWithDependencies'
        ].forEach(function(name) {
            it('В API должна присутствовать функция ' + name, function() {
                expect(dataModel[name]).to.be.an.instanceof(Function);
            });
        });
    });

    describe('Проверка полей на наличие', function() {
        before(function(){
            dataModel = BEM.MODEL.create('dm-mobile-content-banner');
        });

        after(function(){
            dataModel.destruct();
        });

        [
            'modelId', 'bid', 'BannerID', 'isNewBanner', 'banner_type', 'statusShow', 'statusModerate', 'archive',
            'can_delete_banner', 'can_archive_banner',
            'title', 'body', 'reflected_attrs', 'url_protocol', 'href', 'is_template_banner',
            'flags', 'newBannerIndex', 'hash_flags', 'ageInstalled', 'ad_type', 'creative', 'image_ad', 'image_model'
        ].forEach(function(name) {
            it('Поле ' + name + ' должно содержаться в модели', function() {
                expect(dataModel.hasField(name)).to.be.true;
            });
        });
    });

    describe('Поле image_model должно корректно заполняться', function() {
        var bannerImageFieldNames = [
                'modelId',
                'image',
                'image_name',
                'image_width',
                'image_height',
                'source_image',
                'image_source_url',
                'mds_group_id'
            ],
            bannerImageFields = {},
            result;

        beforeEach(function() {
            bannerImageFieldNames.forEach(function(fieldName) {
                bannerImageFields[fieldName] = 'test';
            });

            result = u['dm-mobile-content-banner'].transformData({ banner: bannerImageFields, group: {} });
        });

        bannerImageFieldNames.forEach(function(fieldName) {
            it('Поле ' + fieldName + ' должно быть заполнено', function() {
                expect(result[fieldName]).to.not.undefined;
            });
        });
    });
});
