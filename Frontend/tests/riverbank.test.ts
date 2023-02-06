/* eslint-disable */

import request from 'supertest';
import { FetchDataSuccess } from '../common/serverResponseTypings';
import { createRiverbank } from '../riverbank-web-server/riverbank';

const createEndpointTest = (url: string) => {
    let firstResultFromClickhouse: FetchDataSuccess | undefined = undefined;
    let resultFromCache: FetchDataSuccess | undefined = undefined;
    let forceResultFromClickhouse: FetchDataSuccess | undefined = undefined;
    return ({ force }: { force?: boolean } = { force: false }) => (done: (error?: unknown) => void) => {
        request(createRiverbank())
            .get(`${url}${force ? '&force=1' : ''}`)
            .expect(200)
            .then(response => {
                const result = response.body
                expect(result).toMatchSnapshot();
                let fromCache = false;
                if (firstResultFromClickhouse === undefined) {
                    firstResultFromClickhouse = result;
                } else if (force) {
                    forceResultFromClickhouse = result;
                    expect(JSON.stringify(firstResultFromClickhouse.payload))
                        .toEqual(JSON.stringify(forceResultFromClickhouse?.payload));
                } else {
                    fromCache = true;
                    resultFromCache = result;
                    expect(JSON.stringify(firstResultFromClickhouse.payload))
                        .toEqual(JSON.stringify(resultFromCache?.payload));
                }
                expect(fromCache).toEqual(result.fromCache);
                done();
            })
            .catch(err => done(err));
    };
}

describe('riverbank', () => {
    beforeAll(() => {
        jest.setTimeout(30_000);
        jest.retryTimes(2);
    });

    afterAll(() => {
        jest.setTimeout(5_000);
        jest.retryTimes(0);
    });

    const testCsp = createEndpointTest('/api/session/bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547?tsFrom=1655469547&tsTo=1655471347&provider=csp');
    it('csp data from clikchouse', testCsp());
    it('csp data from cache', testCsp());
    it('force csp data from clikchouse', testCsp({ force: true }));
    it('csp data from cache after force', testCsp());

    const testTimestamps = createEndpointTest('/api/session/timestamps/bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547?tsFrom=1655469547');
    it('timestamps from clikchouse', testTimestamps());

    const testGogol = createEndpointTest('/api/session/bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547?tsFrom=1655469547&tsTo=1655471347&provider=gogol');
    it('gogol data from clikchouse', testGogol());
    it('gogol data from cache', testGogol());
    it('force gogol data from clikchouse', testGogol({ force: true }));
    it('gogol data from cache after force', testGogol());

    const testStrm = createEndpointTest('/api/session/bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547?tsFrom=1655469547&tsTo=1655471347&provider=strm');
    it('strm data from clikchouse', testStrm());
    it('strm data from cache', testStrm());
    it('force strm data from clikchouse', testStrm({ force: true }));
    it('strm data from cache after force', testStrm());

    const testPerflog = createEndpointTest('/api/session/bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547?tsFrom=1655469547&tsTo=1655471347&provider=perflog');
    it('perflog data from clikchouse', testPerflog());
    it('perflog data from cache', testPerflog());
    it('force perflog data from clikchouse', testPerflog({ force: true }));
    it('perflog data from cache after force', testPerflog());

    const testErrorBooster = createEndpointTest('/api/session/242a58c6c3f552ba0da3557d1a35504f3e26ea2d1d10xWEBx9311x1657632680?tsFrom=1657632680&tsTo=1657634480&provider=errorBooster');
    it('errorBooster data from clikchouse', testErrorBooster());
    it('errorBooster data from cache', testErrorBooster());
    it('force errorBooster data from clikchouse', testErrorBooster({ force: true }));
    it('errorBooster data from cache after force', testErrorBooster());

    const testDrm = createEndpointTest('/api/session/2c12e356e8c967dd45d99196f721596abb3cb4dc9ee9xWEBx9344x1657141895?tsFrom=1657141895&tsTo=1657143695&provider=drm');
    it('drm data from clikchouse', testDrm());
    it('drm data from cache', testDrm());
    it('force drm data from clikchouse', testDrm({ force: true }));

    const testOttAcs = createEndpointTest('/api/session/4cdd69876fb0262e87f15b613195c84e081af6cb134bxWEBx9392x1658146586?tsFrom=1658146586&tsTo=1658148386&provider=ottAcs');
    it('ott access logs from clikchouse', testOttAcs());
    it('ott access logs from cache', testOttAcs());
    it('force ott access logs from clikchouse', testOttAcs({ force: true }));

    const testOttApp = createEndpointTest('/api/session/4cdd69876fb0262e87f15b613195c84e081af6cb134bxWEBx9392x1658146586?tsFrom=1658146586&tsTo=1658148386&provider=ottApp');
    it('ott app logs from clikchouse', testOttApp());
    it('ott app logs from cache', testOttApp());
    it('force ott app logs from clikchouse', testOttApp({ force: true }));

    const testGenMusic = createEndpointTest('/api/session/ril1y2ztrf3p3a5l9fqa4ox5kz1h77dz64qk53x2mqp3xMIOx0582x1658992146?tsFrom=1658992146&tsTo=1658993946&provider=genMusic');
    it('generative music data from clikchouse', testGenMusic());
    it('generative music data from cache', testGenMusic());
    it('force generative music data from clikchouse', testGenMusic({ force: true }));
});
