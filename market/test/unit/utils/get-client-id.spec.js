const getClientId = require('./../../../utils/get-client-id');

describe('get client id', () => {
    const COUNT = 10000;
    const EPS = 0.015;

    function* generate() {
        for (let i = 0; i < COUNT; i++) {
            yield getClientId();
        }
    }

    test('should be uniform distribution', () => {
        const probabaility = () => 1 / 15;
        const ids = new Array(...generate());

        const histogram = ids.reduce((prev, uuid) => {
            const char = uuid.charAt(0);
            prev[char] = prev[char] ? prev[char] + 1 : 1;
            return prev;
        }, {});

        const mass = Object.keys(histogram).map((key) => histogram[key] / COUNT);

        mass.map((m) => {
            expect(Math.abs(m - probabaility())).toBeLessThan(EPS);
        });
    });
});
