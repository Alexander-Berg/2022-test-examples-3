/* eslint-disable */
import { createSandbox, fake, SinonSandbox } from 'sinon';
import { merge } from 'lodash';
import * as nock from 'nock';
import * as proxyquire from 'proxyquire';
import anyTest, { TestInterface } from 'ava';
import * as origWordstat from '../../../services/wordstat';
import { ActivationPhraseCommonness } from '../../../db/tables/settings';
import config, { ConfigOverride } from '../../../services/config';

const test = anyTest as TestInterface<{ sandbox: SinonSandbox }>;

function requireWordstatWithConfig(wordstatConfig: ConfigOverride['wordstat'] = {}) {
    return proxyquire<typeof origWordstat>('../../../services/wordstat', {
        './config': {
            default: merge({}, config, {
                wordstat: {
                    url: 'https://wordstat.local/advq/search',
                    enabled: true,
                    ...wordstatConfig,
                },
            }),
        },
    });
}

const wordstatResponse = (count: number) => ({
    requests: [
        {
            stat: {
                total_count: count,
            },
        },
    ],
});

test.before(() => {
    nock('https://wordstat.local', {
        reqheaders: {
            'x-advq-customer': 'paskills',
        },
    })
        .persist()
        .get('/advq/search')
        .query({ words: 'a' })
        .reply(200, wordstatResponse(1))
        .get('/advq/search')
        .query({ words: '"a"' })
        .reply(200, wordstatResponse(2));
});

test.beforeEach(t => {
    t.context.sandbox = createSandbox();
});

test.afterEach.always(t => {
    t.context.sandbox.restore();
});

test('getTotalSearchCount: word is passed to endpoint as query parameter', async t => {
    const { getTotalSearchCount } = requireWordstatWithConfig({ exactQuery: false });

    const result = await getTotalSearchCount('a');

    t.is(result, 1);
});

test('getTotalSearchCount: word is quoted if exactQuery option is set', async t => {
    const { getTotalSearchCount } = requireWordstatWithConfig({ exactQuery: true });

    const result = await getTotalSearchCount('a');

    t.is(result, 2);
});

test('getActivationPhrasesCommonness: undefined or empty strings are ignored', async t => {
    const wordstat = requireWordstatWithConfig();
    const { getActivationPhrasesCommonness } = wordstat;

    const getTotalSearchCount = fake();
    t.context.sandbox.replace(wordstat, 'getTotalSearchCount', getTotalSearchCount);

    const result = await getActivationPhrasesCommonness([undefined, '']);

    t.true(getTotalSearchCount.notCalled);
    t.deepEqual(result, ['green' as ActivationPhraseCommonness, 'green' as ActivationPhraseCommonness]);
});

test('getActivationPhrasesCommonness: common phrases are labeled green', async t => {
    const wordstat = requireWordstatWithConfig({ threshold: 2 });
    const { getActivationPhrasesCommonness } = wordstat;

    const getTotalSearchCount = fake.returns(1);
    t.context.sandbox.replace(wordstat, 'getTotalSearchCount', getTotalSearchCount);

    const result = await getActivationPhrasesCommonness(['a']);

    t.deepEqual(result, ['green' as ActivationPhraseCommonness]);
});

test('getActivationPhrasesCommonness: uncommon phrases are labeled red', async t => {
    const wordstat = requireWordstatWithConfig({ threshold: 2 });
    const { getActivationPhrasesCommonness } = wordstat;

    const getTotalSearchCount = fake.returns(3);
    t.context.sandbox.replace(wordstat, 'getTotalSearchCount', getTotalSearchCount);

    const result = await getActivationPhrasesCommonness(['a']);

    t.deepEqual(result, ['red' as ActivationPhraseCommonness]);
});
