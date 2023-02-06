const request = require('supertest');
const { setValue } = require('../../server/lib/control-agent/container-id');
const app = require('../app');
jest.mock('os');

describe('control agent', () => {
    afterEach(() => {
        setValue(null);
        jest.spyOn(global.Math, 'random').mockRestore();
    });
    it('Первый запуск /new_container', async() => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.12345678);

        const response1 = await request(app)
            .get('/list_endpoints');

        expect(response1.status).toBe(200);
        expect(response1.text).toBe('{}');

        const response2 = await request(app)
            .post('/new_container');

        expect(response2.status).toBe(200);
        expect(response2.text).toBe('{"name":"4jbflt6ibod5okvz.sas_1234567","port":80}');

        const response3 = await request(app)
            .get('/list_endpoints');

        expect(response3.status).toBe(200);
        expect(response3.text).toBe('{"4jbflt6ibod5okvz.sas.yp-c.yandex.net:80":"4jbflt6ibod5okvz.sas_1234567"}');

        const response4 = await request(app)
            .get('/porto/4jbflt6ibod5okvz.sas_1234567');

        expect(response4.status).toBe(200);
        expect(response4.text).toBe('{"translator":"OK","port":80,"state":"running"}');
    });

    it('Повторный запуск /new_container', async() => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.88888888);

        const response1 = await request(app)
            .post('/new_container');

        expect(response1.status).toBe(200);
        expect(response1.text).toBe('{"name":"4jbflt6ibod5okvz.sas_8888888","port":80}');

        const response2 = await request(app)
            .post('/new_container');

        expect(response2.status).toBe(409);
    });

    it('/porto missing', async() => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.77777777);

        await request(app).post('/new_container');

        const response = await request(app)
            .get('/porto/4jbflt6ibod5okvz.sas_1234567');

        expect(response.status).toBe(200);
        expect(response.text).toBe('{"translator":"FAIL","port":0,"state":"missing"}');
    });

    it('/porto delete', async() => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.66666666);

        const response1 = await request(app)
            .delete('/porto/4jbflt6ibod5okvz.sas_6666666');

        expect(response1.status).toBe(200);
        expect(response1.text).toBe('{"translator":"FAIL","port":0,"state":"missing"}');

        await request(app).post('/new_container');

        const response2 = await request(app)
            .delete('/porto/4jbflt6ibod5okvz.sas_6666666');

        expect(response2.status).toBe(200);
        expect(response2.text).toBe('{"state":"destroyed"}');
    });
});
