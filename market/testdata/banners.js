'use strict';

const EMPTY_BANNERS_RESULT = {
    data: [],
    meta: {},
    errors: [],
    jsonapi: {
        version: '1.0',
    },
};

const wrapBannerResult = banner => ({
    id: banner.ad_place,
    attributes: {
        origin: 'adfox',
        http_status_code: '200',
        content: banner,
    },
});

module.exports = {
    EMPTY_BANNERS_RESULT,
    wrapBannerResult,
};
