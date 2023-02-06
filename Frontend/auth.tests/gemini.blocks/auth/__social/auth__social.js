BEM.DOM.decl('auth', {}, {

    _getStubData: function() {
        return [
            {
                display_name: 'ВКонтакте',
                code: 'vk',
                enabled: true,
                primary: true
            },
            {
                display_name: 'Facebook',
                code: 'fb',
                enabled: true,
                primary: true
            },
            {
                display_name: 'Twitter',
                code: 'tw',
                enabled: true,
                primary: true
            },
            {
                display_name: 'Mail.ru',
                code: 'mr',
                enabled: true
            },
            {
                display_name: 'Google',
                code: 'gg',
                enabled: true
            },
            {
                display_name: 'Одноклассники',
                code: 'ok',
                enabled: true
            },
            {
                display_name: 'livejournal',
                code: 'lj',
                enabled: true
            },
            {
                display_name: 'lastfm',
                code: 'lf',
                enabled: true
            },
            {
                display_name: 'instagram',
                code: 'ig',
                enabled: true
            },
            {
                display_name: 'foursquare',
                code: 'fs',
                enabled: true
            }
        ];
    },

    /**
     * Создает BEM-дерево для элемента social.
     *
     * priv.js на клиенте.
     *
     * @protected
     * @param {Object} data — ключ providers, полученный из Паспорта
     * @param {String} id — уникальный идентификатор экземпляра блока
     * @returns {Object} BEM-дерево в формате BEMJSON
     */
    _buildSocial: function(data, id) {
        return this.__base.call(this, this._getStubData(), id);
    }
});
