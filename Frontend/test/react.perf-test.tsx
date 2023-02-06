/* global describe, it */

import * as Benchmark from 'benchmark';
import * as React from 'react';
import { renderToString } from 'react-dom/server';

interface IButtonProps {
    onClick?: React.MouseEventHandler;
}

const Button: React.FunctionComponent<IButtonProps> = ({ onClick, children }) => (
    <button onClick={onClick}>{children}</button>
);

function withEmptyFc<P>(WrappedComponent: React.ComponentType<P>): React.FunctionComponent<P> {
    return (props: P) => <WrappedComponent {...props} />;
}

const FcButton = withEmptyFc(Button);

describe('performance renderToString', () => {
    it('without baobab', function(done) {
        new Benchmark.Suite()
            // Проверяем издержки на пустые обёртки, чтобы проще сравнить с замедлением от Баобаб.
            .add('without baobab FC', () => renderToString(<Button />))
            .add('without baobab FC with functions HOC', () => renderToString(<FcButton />))
            // eslint-disable-next-line no-console
            .on('cycle', (event: { target: string; }) => console.log(String(event.target)))
            .on('complete', () => done())
            .run({ async: true });
    }, 120000);
});
