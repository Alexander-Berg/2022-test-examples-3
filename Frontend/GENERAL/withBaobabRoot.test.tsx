import { assert } from 'chai';
import * as React from 'react';
import { renderToString } from 'react-dom/server';

import { Sender } from '../common/Sender/Sender';
import { Baobab } from '../common/Baobab/Baobab';
import { INodeContext } from '../common/Baobab/Baobab.typings/Baobab';
import { logClick } from '../helpers/eventConstruct';
import { getBaobabInitState } from '../helpers/getBaobabInitState';

import { withBaobab } from './withBaobab';
import { withBaobabRoot } from './withBaobabRoot';

interface IButtonProps {
    onClick?: React.MouseEventHandler;
}

const ButtonComponent: React.FunctionComponent<IButtonProps> = props => (
    // @ts-ignore
    <button onClick={props.onClick} baobabid={props.baobabNode.id}>{props.children}</button>
);

const Button = withBaobab(
    {
        name: 'button',
        attrs: { about: 'baobab' },
        events: { onClick: logClick() },
    },
    ButtonComponent,
);

interface IMessageBoxProps {
    buttonName: string
}

// @ts-ignore
const MessageBox = withBaobab<IMessageBoxProps>({
    name: 'message-box',
})(props => (
    // @ts-ignore
    <div baobabid={props.baobabNode.id}>
        <div>{props.children}</div>
        <Button logNode={{ name: 'accept' }}>{props.buttonName}</Button>
    </div>
));

interface IAppProps {
    message: string
}

const App = (props: IAppProps) => (
    <MessageBox buttonName={'OK'}>
        {props.message}
    </MessageBox>
);

const LoggableApp = withBaobabRoot<IAppProps, INodeContext>({
    name: '$page',
    attrs: { ui: 'desktop', service: 'ugc' },
}, Baobab)(App);

const baobabState = getBaobabInitState({
    reqId: '1569...-12681',
    service: 'ugc',
    table: 'ugc',
    pageUrl: 'yandex.ru/ugcpub',
    slots: ['123456,0,5'],
    Sender,
});

describe('withBaobabRoot', () => {
    it('should pass proper baobabId to rendered components', function() {
        const renderedContent = renderToString(<LoggableApp baobab={baobabState} message={'Message'} />);
        assert.strictEqual(
            renderedContent,
            '<div baobabid="j23p1"><div>Message</div><button baobabid="j23p2">OK</button></div>',
        );
    });
});
