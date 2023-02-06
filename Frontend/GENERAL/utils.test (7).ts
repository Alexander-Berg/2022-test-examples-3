import {
    NO_SEGMENTS,

    combineSorted,
    getCombinedSegmentsKeyByIds,
    getCombinedSegmentsKeyByKeys,
    getCombinedSegmentsKeyFromSortedSegmentKeys,
} from './utils';
import { Segmentation } from './types/dictionaries';

const SEGMENTATIONS: Segmentation[] = [{
    id: 1,
    key: 'locations',
    name: 'Locations',
    segments: [
        { id: 1, key: 'VLA', name: 'VLA', priority: 1 },
        { id: 9, key: 'SAS', name: 'SAS', priority: 1 },
        { id: 10, key: 'MAN', name: 'MAN', priority: 1 },
        { id: 42, key: 'MYT', name: 'MYT', priority: 1 },
    ],
}, {
    id: 3,
    key: 'dbaas_db',
    name: 'База данных',
    segments: [
        { id: 4, key: 'dbaas_clickhouse', name: 'ClickHouse', priority: 1 },
        { id: 5, key: 'dbaas_mongodb', name: 'MongoDB', priority: 1 },
        { id: 6, key: 'dbaas_mysql', name: 'MySQL', priority: 1 },
        { id: 7, key: 'dbaas_pgsql', name: 'PostgreSQL', priority: 1 },
        { id: 8, key: 'dbaas_redis', name: 'Redis', priority: 1 },
        { id: 182, key: 'dbaas_kafka', name: 'Kafka', priority: 2 },
        { id: 183, key: 'dbaas_elasticsearch', name: 'Elasticsearch', priority: 2 },
    ],
}, {
    id: 74,
    key: 'logbroker',
    name: 'Logbroker Segmentation',
    segments: [
        { id: 108, key: 'lbkx', name: 'LBKX', priority: 1 },
        { id: 109, key: 'logbroker_SAS', name: 'Logbroker (SAS)', priority: 1 },
        { id: 110, key: 'logbroker_VLA', name: 'Logbroker (VLA)', priority: 1 },
        { id: 111, key: 'logbroker_MAN', name: 'Logbroker (MAN)', priority: 1 },
        { id: 112, key: 'logbroker_MYT', name: 'Logbroker (MYT)', priority: 1 },
        { id: 113, key: 'logbroker_IVA', name: 'Logbroker (IVA)', priority: 1 },
        { id: 114, key: 'logbroker-prestable_SAS', name: 'Logbroker prestable (SAS)', priority: 1 },
        { id: 115, key: 'logbroker-prestable_VLA', name: 'Logbroker prestable (VLA)', priority: 1 },
        { id: 116, key: 'logbroker-prestable_MAN', name: 'Logbroker prestable (MAN)', priority: 1 },
        { id: 117, key: 'logbroker-prestable_MYT', name: 'Logbroker prestable (MYT)', priority: 1 },
        { id: 118, key: 'logbroker-prestable_IVA', name: 'Logbroker prestable (IVA)', priority: 1 },
        { id: 217, key: 'logbroker-yc', name: 'YC Logbroker', priority: 1 },
    ],
}, {
    id: 75,
    key: 'yt_cluster',
    name: 'Кластер YT',
    segments: [
        { id: 119, key: 'hahn', name: 'Hahn', priority: 1 },
        { id: 120, key: 'arnold', name: 'Arnold', priority: 1 },
        { id: 121, key: 'seneca-sas', name: 'Seneca (SAS)', priority: 1 },
        { id: 122, key: 'seneca-vla', name: 'Seneca (VLA)', priority: 1 },
        { id: 123, key: 'seneca-man', name: 'Seneca (MAN)', priority: 1 },
        { id: 124, key: 'freud', name: 'Freud', priority: 1 },
        { id: 125, key: 'hume', name: 'Hume', priority: 1 },
        { id: 126, key: 'landau', name: 'Landau', priority: 1 },
        { id: 127, key: 'bohr', name: 'Bohr', priority: 1 },
        { id: 128, key: 'zeno', name: 'Zeno', priority: 1 },
        { id: 129, key: 'locke', name: 'Locke', priority: 1 },
        { id: 130, key: 'markov', name: 'Markov', priority: 1 },
        { id: 131, key: 'vanga', name: 'Vanga', priority: 1 },
    ],
}, {
    id: 76,
    key: 'yp_segment',
    name: 'Сегмент YP',
    segments: [
        { id: 132, key: 'dev', name: 'Dev', priority: 1 },
        { id: 133, key: 'default', name: 'Default', priority: 1 },
    ],
}, {
    id: 78,
    key: 'sandbox_type',
    name: 'Sandbox type',
    segments: [
        { id: 170, key: 'sandbox_linux_bare_metal', name: 'linux (bare metal)', priority: 0 },
        { id: 171, key: 'sandbox_linux_yp', name: 'linux (YP, запуск в porto)', priority: 0 },
        { id: 172, key: 'sandbox_windows', name: 'windows (YP)', priority: 0 },
        { id: 173, key: 'sandbox_mac_mini', name: 'MacMini', priority: 0 },
    ],
}, {
    id: 79,
    key: 'distbuild_segment',
    name: 'Сегмент DistBuild',
    segments: [
        { id: 184, key: 'distbuild_autocheck', name: 'Autocheck', priority: 0 },
        { id: 185, key: 'distbuild_user', name: 'User', priority: 0 },
    ],
}];

describe('combineSorted', () => {
    it('Should return all possible combinations of input segments', () => {
        const actual = combineSorted([
            ['1', '9', '10', '42'], // VLA, SAS, MAN, MYT
            ['184', '185'], // distbuild_autocheck, distbuild_user
        ], SEGMENTATIONS);

        expect(actual.length).toBe(8);
        expect(actual).toContain('VLA.distbuild_autocheck');
        expect(actual).toContain('VLA.distbuild_user');
        expect(actual).toContain('SAS.distbuild_autocheck');
        expect(actual).toContain('SAS.distbuild_user');
        expect(actual).toContain('MAN.distbuild_autocheck');
        expect(actual).toContain('MAN.distbuild_user');
        expect(actual).toContain('MYT.distbuild_autocheck');
        expect(actual).toContain('MYT.distbuild_user');
    });

    it('Should sort segment keys by segmentation order', () => {
        const actual = combineSorted([
            // segments in random order
            ['184'], // distbuild_autocheck
            ['132'], // dev
            ['108'], // lbkx
            ['119'], // hahn
            ['4'], // dbaas_clickhouse
            ['1'], // VLA
            ['170'], // sandbox_linux_bare_metal
        ], SEGMENTATIONS);

        expect(actual).toContain('VLA.dbaas_clickhouse.lbkx.hahn.dev.sandbox_linux_bare_metal.distbuild_autocheck');
    });
});

describe('getCombinedSegmentsKeyByIds', () => {
    it('Should return joined sorted segments keys', () => {
        expect(getCombinedSegmentsKeyByIds([4, 1, 119], SEGMENTATIONS))
            .toBe('VLA.dbaas_clickhouse.hahn');
    });

    it('Should return a special key when there are no segments', () => {
        expect(getCombinedSegmentsKeyByIds([], SEGMENTATIONS)).toBe(NO_SEGMENTS);
    });
});

describe('getCombinedSegmentsKeyByKeys', () => {
    it('Should return joined sorted segments keys', () => {
        expect(getCombinedSegmentsKeyByKeys(['dbaas_clickhouse', 'VLA', 'hahn'], SEGMENTATIONS))
            .toBe('VLA.dbaas_clickhouse.hahn');
    });

    it('Should return a special key when there are no segments', () => {
        expect(getCombinedSegmentsKeyByKeys([], SEGMENTATIONS)).toBe(NO_SEGMENTS);
    });
});

describe('getCombinedSegmentsKeyFromSortedSegmentKeys', () => {
    it('Should return joined keys in order they were passed', () => {
        expect(getCombinedSegmentsKeyFromSortedSegmentKeys(['world', 'hello', '42']))
            .toBe('world.hello.42');
    });

    it('Should return a special key when there are no segments', () => {
        expect(getCombinedSegmentsKeyFromSortedSegmentKeys([])).toBe(NO_SEGMENTS);
    });
});
