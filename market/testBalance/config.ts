/* istanbul ignore file */

import {pipe} from 'ramda';
import xamel2json from 'app/stout/deprecated/xamel2json';

export default {
    method: 'POST',
    cfg: {
        maxRetries: 0,
        timeout: 5000,
        dataType: 'xml',
    },
    bodyEncoding: 'string',
    headers: {
        'Content-Type': 'application/xml',
        'cache-control': 'no-cache',
    },
    // @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
    processHandler: pipe(xmlResult => xmlResult.$('methodResponse/params/param/value/*'), xamel2json),
};
