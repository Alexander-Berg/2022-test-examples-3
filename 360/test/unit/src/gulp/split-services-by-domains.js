'use strict';

import { describe } from 'ava-spec';
import splitServicesByDomains from '../../../../src/gulp/helpers/split-services-by-domains.js';
import config from '../../../../dist/yandex-services.json';

describe('#splitServicesByDomains', (it) => {
    it('Должен правильно разделить конфиг по доменам', (t) => {
        const { mail: { uz: uzMail, 'yandex-team': yt }, disk: { uz: uzDisk } } = config;

        const splitting = {
            "uz": {
                "mail": {
                    "url": "https://mail.yandex.uz"
                },
                "disk": {
                    "url": "https://disk.yandex.uz"
                }
            },

            "yandex-team": {
                "mail": {
                    "url": "https://mail.yandex-team.ru"
                }
            }
        };

        t.deepEqual(
            splitServicesByDomains({ mail: { uz: uzMail, 'yandex-team': yt }, disk: { uz: uzDisk } }),
            splitting
        );
    });
});
