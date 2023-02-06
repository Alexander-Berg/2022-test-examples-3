window.mockCollectionsAPI = function(params) {
    BEM.decl('favorites-controller', {}, {
        _loadCollectionsList: function() {
            var results = [];
            var dfd = $.Deferred();

            results.push({
                title: 'Избранное',
                is_default: true,
                is_private: true,
                owner: {}
            });

            dfd.resolve({
                count: 0,
                results: results
            });

            return dfd;
        },

        _favoritesRequest: function(method, data, cardId) {
            var dfd = $.Deferred();
            setTimeout(() => dfd.resolve({
                id: '0e50591a-700e-469e-b89c-38e37e8630e9',
                state: 'QUEUED',
                board: {
                    id: '5b842c83f0d00a0091cc6847',
                    title: 'TEST',
                    url: 'https://yandex.ru/collections/favorites/'
                }
            }),
            100);
            return dfd;
        }
    });
};
