function cacheFilename({ cache }, next) {
    if (!cache.key || cache.filename) {
        next();

        return;
    }

    cache.filename = `${cache.key}.json.gz`;

    next();
}

module.exports = {
    'cache-filename': cacheFilename
};
