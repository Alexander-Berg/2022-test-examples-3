(function() {
    if (!Ya.StaticPrefetcher) return;

    var prefetchedServices = [];

    // В тестах не нужно загружать prefetch.txt поэтому очищаем
    Ya.StaticPrefetcher._resetPrefetcher();

    Ya.StaticPrefetcher.add = function(listUrl) {
        if (!listUrl) return;

        if (!Ya.StaticPrefetcher.isServiceStaticPrefetched(listUrl)) {
            prefetchedServices.push(listUrl);
        }
    };

    var baseIsServiceStaticPrefetched = Ya.StaticPrefetcher.isServiceStaticPrefetched;

    Ya.StaticPrefetcher.isServiceStaticPrefetched = function(servicePrefetchUrl) {
        return baseIsServiceStaticPrefetched(servicePrefetchUrl) ||
            prefetchedServices.indexOf(servicePrefetchUrl) !== -1;
    };
})();
