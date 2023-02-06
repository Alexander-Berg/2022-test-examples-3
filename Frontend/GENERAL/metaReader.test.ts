import { readContentData } from './metaReader';
import { htmlVcsFrameEvents, htmlVcsFrame } from './metaReader.test.snapshot';

const meta = {
    author: 'robot-frontend@yandex-team.ru',
    branch: 'release/turbo/v0.642.0',
    build: 'https://sandbox.yandex-team.ru/task/1068590660',
    commit: '87454971463',
    date: new Date('2021-09-08T17:19:27.000Z'),
    env: 'production',
    host: 'undefined.search.yandex.net',
    presearch: false,
    reqid: '7777777777777777-6666666666666666666-xxxxxxxxxxxxxxxx-BAL-0000',
    url: 'https://l7test.yandex.ru/jobs/locations/moscow?dump=eventlog&eventlog_format=json&json_dump_requests=RENDERER&concat_json_dump=html_comment',
};

describe('metaReader', () => {
    describe('readContent', () => {
        it('htmlVcsFrameEvents', () => {
            const res = readContentData(htmlVcsFrameEvents);
            expect(res.errors).toHaveLength(0);
            expect(res.dump).toHaveProperty('TURBOPAGES_SUBGRAPH');
            expect(res.eventlog).toHaveLength(200);
            expect(res.meta).toMatchObject(meta);
        });

        it('htmlVcsFrame', () => {
            const res = readContentData(htmlVcsFrame);
            expect(res.errors).toHaveLength(1);
            expect(res.errors).toEqual(['debuginfo: not found']);
            expect(res.dump).toHaveProperty('TURBOPAGES_SUBGRAPH');
            expect(res.eventlog).toEqual(null);
            expect(res.meta).toMatchObject(meta);
        });
    });
});

// htmlVcsFrame
