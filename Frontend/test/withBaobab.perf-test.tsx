/* global describe, it */

import * as Benchmark from 'benchmark';
import * as React from 'react';
import { renderToString } from 'react-dom/server';

import { Baobab } from '../common/Baobab/Baobab';
import { BaobabAppendTo } from '../components/BaobabAppendTo/BaobabAppendTo';
import { BaobabRoot } from '../components/BaobabRoot/BaobabRoot';
import { logClick } from '../helpers/eventConstruct';
import { withBaobab } from '../hocs/withBaobab';

interface IButtonProps {
    onClick?: React.MouseEventHandler;
}

const Button: React.FunctionComponent<IButtonProps> = ({ onClick, children }) => (
    <button onClick={onClick}>{children}</button>
);

const baobabData = {
    name: 'button',
    attrs: { about: 'baobab' },
    events: { onClick: logClick() },
};

const LogHocButton = withBaobab(baobabData, Button);

const baobabState = { prefix: 'id-' };

describe('performance renderToString', () => {
    it('without baobab', function(done) {
        const logger = new Baobab(baobabState);
        const pHoc = logger.createRoot({ name: 'p', attrs: { ui: 'test', service: 'web' } });

        new Benchmark.Suite()
            // Проверяем withBaobab
            .add('node without root FC + withBaobab', () => renderToString(<LogHocButton />))
            .add('create logger + root node FC + withBaobab', () => {
                renderToString(<BaobabRoot Logger={Baobab} loggerProps={baobabState}><LogHocButton /></BaobabRoot>);
            })
            .add('root node FC + withBaobab', () => {
                renderToString(<BaobabAppendTo logger={logger}><LogHocButton /></BaobabAppendTo>);
            })
            .add('child node FC + withBaobab', () => {
                renderToString(<BaobabAppendTo logger={logger} parentNode={pHoc}><LogHocButton /></BaobabAppendTo>);
            })
            // eslint-disable-next-line no-console
            .on('cycle', (event: { target: string; }) => console.log(String(event.target)))
            .on('complete', () => done())
            .run({ async: true });
    }, 120000);
});
