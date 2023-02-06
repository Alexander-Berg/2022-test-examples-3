//скопировано из ветки preview-tests
BEM.DOM.decl({ block: 'b-banner-preview2', modName: 'view', modVal: 'tests-only' }, {

    /**
     * Возвращает объект зависимостей элементов от входных данных
     * Слева название элемента, справа поля view-модели от которых зависит элемент
     * @returns {Object}
     * @private
     */
    _getElemsDepends: function() {
        return {
            additions: ['sitelinks', 'image', 'videoExtension'],
            body: ['body', 'phrase'],
            city: ['city', 'vcard'],
            domain: ['domain', 'vcard', 'isHrefHasParams'],
            favicon: ['url'],
            'flag-age': ['flagsSettings', 'flags', 'bid'],
            flags: ['flagsSettings', 'flags', 'bid'],
            'geo-names': ['geo'],
            image: ['image', 'creative'],
            metro: ['metro', 'vcard'],
            'partner-code': ['title', 'body', 'image', 'sitelinks', 'url', 'domain', 'vcard', 'flags', 'phrase'],
            phone: ['phone', 'country_code', 'country_code', 'ext', 'vcard'],
            rating: ['rating'],
            sitelinks: ['sitelinks'],
            title: ['title', 'url', 'phrase', 'vcard'],
            'v-card': ['vcard'],
            'warning-template': ['isTemplateBanner'],
            worktime: ['worktime', 'vcard'],

            //элементы, которых нет в preview-tests
            'mobile-content-icon': ['url', 'icon', 'showIcon'],
            'action-button': ['url', 'showPrice', 'price', 'currency', 'actionType'],
            'mobile-content-rating': [
                'url', 'showRating', 'rating', 'showRatingVotes', 'ratingVotes'
            ]
        };
    }
});
