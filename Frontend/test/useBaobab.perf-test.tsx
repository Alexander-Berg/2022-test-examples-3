/* global describe, it */

import * as Benchmark from 'benchmark';
import * as React from 'react';
import { renderToString } from 'react-dom/server';

import { Baobab } from '../common/Baobab/Baobab';
import { BaobabAppendTo } from '../components/BaobabAppendTo/BaobabAppendTo';
import { BaobabRoot } from '../components/BaobabRoot/BaobabRoot';
import { logClick } from '../helpers/eventConstruct';
import { useBaobab } from '../hooks/useBaobab';

interface IButtonProps {
    onClick?: React.MouseEventHandler;
}

const baobabData = {
    name: 'button',
    attrs: { about: 'baobab' },
    events: { onClick: logClick() },
};

const LogHookButton: React.FunctionComponent<IButtonProps> = ({ onClick, children }) => {
    const { node } = useBaobab(baobabData);
    const clickHandler: React.MouseEventHandler = event => {
        if (node) node.logClick(undefined, [event], {});
        if (onClick) onClick(event);
    };

    return <button onClick={clickHandler}>{children}</button>;
};

const baobabState = { prefix: 'id-' };

describe('performance renderToString', () => {
    it('without baobab', function(done) {
        const logger = new Baobab(baobabState);
        const pHook = logger.createRoot({ name: 'p', attrs: { ui: 'test', service: 'web' } });

        new Benchmark.Suite()
            // Проверяем useBaobab
            .add('node without root FC + useBaobab', () => renderToString(<LogHookButton />))
            .add('create logger + root node FC + useBaobab', () => {
                renderToString(<BaobabRoot Logger={Baobab} loggerProps={baobabState}><LogHookButton /></BaobabRoot>);
            })
            .add('root node FC + useBaobab', () => {
                renderToString(<BaobabAppendTo logger={logger}><LogHookButton /></BaobabAppendTo>);
            })
            .add('child node FC + useBaobab', () => {
                renderToString(<BaobabAppendTo logger={logger} parentNode={pHook}><LogHookButton /></BaobabAppendTo>);
            })
            // eslint-disable-next-line no-console
            .on('cycle', (event: { target: string; }) => console.log(String(event.target)))
            .on('complete', () => done())
            .run({ async: true });
    }, 120000);
});
