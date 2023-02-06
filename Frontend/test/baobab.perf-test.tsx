/* global describe, it */

import * as Benchmark from 'benchmark';

import {
    Baobab,
    createChildContext,
    createRootContext,
} from '../common/Baobab/Baobab';

const prefix = 'id-';
const baobabState = { prefix };

describe('performance renderToString', () => {
    it('without baobab', function(done) {
        const logger = new Baobab(baobabState);
        const rootNode = logger.createRoot({ name: 'p', attrs: { ui: 'test', service: 'web' } });
        const nodeContext = createRootContext({ name: 'c', attrs: { ui: 'test', service: 'web' } }, prefix);

        new Benchmark.Suite()
            .add('create child context', () => {
                createChildContext(rootNode, { name: 'c', attrs: { ui: 'test', service: 'web' } });
            })
            .add('create child context (new subservice)', () => {
                createChildContext(rootNode, { name: 'c', attrs: { ui: 'test', service: 'web', subservice: 'video' } });
            })
            .add('create root context', () => {
                createRootContext({ name: 'c', attrs: { ui: 'test', service: 'web' } }, prefix);
            })
            .add('create node', () => {
                logger.createNode({ name: 'c', attrs: { ui: 'test', service: 'web' } }, nodeContext);
            })
            .add('create child node', () => {
                logger.createChild(rootNode, { name: 'c', attrs: { ui: 'test', service: 'web' } });
            })
            .add('crete root node', () => {
                logger.createRoot({ name: 'p', attrs: { ui: 'test', service: 'web' } });
            })
            // eslint-disable-next-line no-console
            .on('cycle', (event: { target: string; }) => console.log(String(event.target)))
            .on('complete', () => done())
            .run({ async: true });
    }, 120000);
});
