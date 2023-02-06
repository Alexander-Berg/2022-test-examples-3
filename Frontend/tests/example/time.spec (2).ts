import { bootstrap } from 'server/bootstrap';

describe('GET /api/example/time', () => {
    beforeEach(async () => {
        await bootstrap();
    });

    test('example', () => {
        expect(1).toBe(1);
    });
});
