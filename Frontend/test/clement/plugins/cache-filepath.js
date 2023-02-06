const { resolve: pathResolve } = require('path');

function cacheFilePath(ctx, next) {
    if (!ctx.cache.key || ctx.cache.filepath) {
        next();

        return;
    }

    const { req: { cookies: { dumps_test_path: dumpsTestPath } } } = ctx;

    let { cfg: { cachePath } } = ctx;

    if (dumpsTestPath) {
        cachePath = dumpsTestPath;
    }

    ctx.cache.filepath = pathResolve(cachePath);

    next();
}

module.exports = {
    'cache-filepath': cacheFilePath
};
