const sizeOf = require('image-size');

async function paintImagesOver(ctx, next) {
    const { cacheMode } = ctx.env;
    if (cacheMode !== 'create' && cacheMode !== 'write') {
        return next();
    }

    ctx.sourceReq.hooks = {
        beforeRequest: [
            options => {
                options.encoding = null;
                options.responseType = 'buffer';
            },
        ],
        afterResponse: [
            response => {
                try {
                    const { width, height } = sizeOf(response.body);
                    response.headers['content-type'] = 'image/svg+xml';
                    response.body = `<?xml version="1.0"?><svg viewBox="0 0 ${width} ${height}" xmlns="http://www.w3.org/2000/svg"><rect x="0" y="0" width="100%" height="100%" style="fill: #33ff33;" /></svg>`;
                } catch {
                    response.body = Buffer.from(response.body).toString();
                }
                return response;
            },
        ],
    };

    next();
}

module.exports = {
    'data-fetch': paintImagesOver,
};
