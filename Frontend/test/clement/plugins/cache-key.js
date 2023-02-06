const { join: pathJoin } = require('path');
const crypto = require('crypto');

function escape(s) {
    return s
        .replace(/[:=;]/g, $0 => `(${$0.charCodeAt(0)})`)
        .replace(/\//g, ';');
}

function createTypeKey({ req: { method } }) {
    return escape(method).toLowerCase();
}

function createNameKey({ sourceReq: { host } }) {
    return escape(host).toLowerCase();
}

function createPortKey({ req: { parsedURL: { port } } }) {
    return String(port);
}

function createPathKey({ req: { parsedURL: { pathname } } }) {
    return pathname;
}

function createArgsKey({ req: { parsedURL: { query } } }) {
    return Object.entries(Object(query))
        .sort(([a], [b]) => a.length - b.length)
        .reduce((acc0, [key, v]) => {
            let vals = v;

            if (!Array.isArray(v)) {
                vals = [vals];
            }

            return vals.reduce((acc1, val) => {
                if (typeof val !== 'string') {
                    return acc1;
                }

                return acc1.concat(`${escape(key)}=${escape(val)}`);
            }, acc0);
        }, [])
        .join(';');
}

function createHashKey({ req: { cookies: { __hash__ } } }) {
    return escape(__hash__ || '');
}

function createReqKey(ctx) {
    const path = pathJoin(`${createNameKey(ctx)}_${createPortKey(ctx)}`, createPathKey(ctx));

    const key = [
        `args:${createArgsKey(ctx)}`,
        `hash:${createHashKey(ctx)}`,
    ].join(';');

    const method = createTypeKey(ctx);
    const hash = crypto.createHash('sha256').update(key).digest('hex');

    const filename = `${method}_${hash}`;

    return pathJoin(path, filename);
}

function cacheKey(ctx, next) {
    if (ctx.cache.key) {
        next();
        return;
    }

    ctx.cache.key = createReqKey(ctx);

    next();
}

module.exports = {
    'cache-key': cacheKey,
};
