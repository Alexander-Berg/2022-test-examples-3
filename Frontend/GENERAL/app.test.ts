import request from 'supertest';

import app from './app';
import { logger } from './lib/logger';
import { mockNetwork } from './utils/tests';

describe('App', () => {
    mockNetwork();

    test('Должен корректно обработать запросы вида /%C0%AE%C0%AE%C0%AF?param=value', async() => {
        const spyLogger = jest.spyOn(logger, 'warn');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?1\'=1&everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?1%20and%201034%3d01034--%20=1&everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?1%2c0)waitfor%20delay\'0%3a0%3a20\'--=1&everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?1%2b(select*from(select(sleep(20)))a)%2b=1&everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%C0%AE%C0%AE%C0%AF?cwd8t94qzl=1&everybodybecoolthisis=crasher')
            .expect(404, 'Not Found');

        await request(app)
            .options('/%dratuti')
            .expect(404, 'Not Found');

        expect(spyLogger).toHaveBeenCalledTimes(7);
    });
});
