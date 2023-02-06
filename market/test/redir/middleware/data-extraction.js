'use strict';

const url = require('url');

const PATH_RE = /^\/.*/;

/**
 * @typedef {Object} ExtractedData
 * @property {Undefined|String} url
 * @property {Undefined|String} parsedURL
 * @property {Undefined|String} hostname
 * @property {Undefined|String} target
 * @property {Undefined|Number} affId
 * @property {Undefined|Number} clid
 */

/**
 * Data extraction middleware.
 *
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function dataExtractionMiddleware(req, res, next) {
    let parsedUrl;
    if (req.query && typeof req.query.url === 'string') {
        parsedUrl = url.parse(req.query.url, true);
        if (parsedUrl && parsedUrl.path && !PATH_RE.test(parsedUrl.path)) {
            res.end();
            throw new Error(`${req.query.url} has unknown path`);
        }
    }

    const hostname = parsedUrl && parsedUrl.hostname;
    const target = req.query && req.query.target;
    const affId = req.query && req.query.aff_id;
    const clid = req.query && req.query.clid;

    /**
     * @type {ExtractedData}
     */
    req.extractedData = {
        url: req.query && req.query.url,
        parsedUrl,
        hostname,
        target,
        affId,
        clid
    };

    next();
}

/**
 * @type {dataExtractionMiddleware}
 */
module.exports = dataExtractionMiddleware;
