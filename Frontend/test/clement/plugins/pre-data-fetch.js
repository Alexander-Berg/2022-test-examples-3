const qs = require('qs');

function stringifyQuery(ctx, next) {
    if (ctx.sourceReq.query) {
        const query = qs.stringify(
            ctx.sourceReq.query,
            { arrayFormat: 'repeat' },
        );
        ctx.sourceReq.query = null;
        ctx.sourceReq.path += `?${query}`;
    }

    next();
}

module.exports = {
    'pre-data-fetch': stringifyQuery,
};
