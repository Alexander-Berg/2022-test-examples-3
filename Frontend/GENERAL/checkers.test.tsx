import * as React from 'react';
import { assert } from 'chai';
import { mount } from 'enzyme';

import { withBaobab } from '../hocs/withBaobab';
import { logClick, logTech } from './eventConstruct';
import { getBaobabInitState } from './getBaobabInitState';
import { SpySenderForTest } from '../common/Sender/SpySenderForTest';
import { withDynamic } from '../common/Baobab/_dynamic/Baobab_dynamic';
import { Baobab, IBaobab, INodeContext } from '../common/Baobab/Baobab';
import { BaobabAppendTo } from '../components/BaobabAppendTo/BaobabAppendTo';
import { getBaobabComponent, getNodeFromThis, isReactBaobabNode } from './checkers';

interface IButtonProps {
    dynValue?: number;
    onClick?: React.MouseEventHandler;
    onMount?: () => void;
}

const ButtonComponent: React.FunctionComponent<IButtonProps> = props => {
    const { onMount, onClick, children } = props;
    React.useEffect(() => {
        onMount && onMount();
    }, [onMount]);
    return <button onClick={onClick}>{children}</button>;
};

const LoggableButton = withBaobab({
    name: '$page',
    attrs: { ui: 'desktop', service: 'button' },
    events: { onClick: logClick(), onMount: logTech('mount') },
}, ButtonComponent);

const baobabState = getBaobabInitState({
    reqId: '1569...-12681',
    service: 'ugc',
    table: 'ugc',
    pageUrl: 'yandex.ru/ugcpub',
    slots: ['123456,0,5'],
    Sender: SpySenderForTest,
});

const Logger = withDynamic(Baobab);

describe('checkers', function() {
    let logger: IBaobab<INodeContext>;

    beforeEach(() => {
        logger = new Logger(baobabState);
    });

    describe('getBaobabComponent', function() {
        it('should get component for BaobabNode', function() {
            const component = mount(
                <BaobabAppendTo logger={logger}>
                    <LoggableButton logNode={{ name: 'ok-button' }} />
                </BaobabAppendTo>,
            );
            const buttonDomNode = component.find(ButtonComponent).getDOMNode();
            const baobabComponent = getBaobabComponent(buttonDomNode);

            assert.exists(baobabComponent);

            component.unmount();
        });
    });

    describe('getNodeFromThis', function() {
        it('should get node data from React internal fiber', function() {
            const component = mount(
                <BaobabAppendTo logger={logger}>
                    <LoggableButton logNode={{ name: 'ok-button' }} />
                </BaobabAppendTo>,
            );
            const buttonDomNode = component.find(ButtonComponent).getDOMNode();
            const internalInstanceKey = Object.keys(buttonDomNode)
                .find(key => key.startsWith('__reactInternalInstance$'));

            assert.exists(internalInstanceKey);

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const buttonInternalInstance = (buttonDomNode as Record<string, any>)[internalInstanceKey!];
            const baobabNodeInternalInstance = buttonInternalInstance.return.return.return;
            const node = getNodeFromThis(baobabNodeInternalInstance);

            assert.exists(node);
            assert.include(node!, { id: 'j23p0', name: 'ok-button' });
        });
    });

    describe('isReactBaobabNode', function() {
        let logger: IBaobab<INodeContext>;

        beforeEach(() => {
            logger = new Logger(baobabState);
        });

        it('should detect BaobabNode', function() {
            const component = mount(
                <BaobabAppendTo logger={logger}>
                    <LoggableButton logNode={{ name: 'ok-button' }} />
                </BaobabAppendTo>,
            );
            const buttonDomNode = component.find(ButtonComponent).getDOMNode();
            const internalInstanceKey1 = Object.keys(buttonDomNode)
                .find(key => key.startsWith('__reactInternalInstance$'));
            assert.exists(internalInstanceKey1);
            const internalInstanceKey: string = internalInstanceKey1 as string;

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const buttonInternalInstance = (buttonDomNode as Record<string, any>)[internalInstanceKey];
            const baobabNodeInternalInstance = buttonInternalInstance.return.return.return;
            assert.isTrue(isReactBaobabNode(baobabNodeInternalInstance));

            component.unmount();
        });
    });
});
