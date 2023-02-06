import {assert} from 'chai';
import axios from 'axios';
import fs from 'fs-extra';
import https from 'https';
import * as path from 'path';

describe('Портал: Главная', () => {
    it('Файл robots', async () => {
        const file = await fs.readFile(
            path.resolve(process.cwd(), '../public/robots/default.txt'),
            'utf8',
        );

        const {data: robotsData} = await axios.get('/robots.txt', {
            baseURL: process.env.E2E_URL,
            httpsAgent: new https.Agent({
                rejectUnauthorized: false,
            }),
        });

        assert.equal(
            robotsData,
            file,
            `Не совпадает содержимое robots.txt c ожидаемым`,
        );
    });
});
