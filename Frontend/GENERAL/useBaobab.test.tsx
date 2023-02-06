import * as React from 'react';

import { mount, render } from 'enzyme';
import { assert } from 'chai';

import { useBaobab } from './useBaobab';
import { Sender } from '../common/Sender/Sender';
import { Baobab } from '../common/Baobab/Baobab';
import { withDynamic } from '../common/Baobab/_dynamic/Baobab_dynamic';
import { withRootAccess } from '../common/Baobab/_rootAccess/Baobab_rootAccess';
import { withCache } from '../common/Baobab/_cache/Baobab_cache';
import { INodeContext, INodeInfo, ISsrNodeInfo } from '../common/Baobab/Baobab.typings/Baobab';
import { BaobabAppendTo } from '../components/BaobabAppendTo/BaobabAppendTo';
import { ILoggable } from '../typings/Component';
import { getBaobabInitState } from '..';

const Logger = withRootAccess(withDynamic<INodeContext, typeof Baobab>(Baobab));
const LoggerWithCache = withCache(Logger);

interface IContainerProps {
    hasClose?: boolean;
}

// Если не мемоизировать, то при ререндере в дереве появится новый пустой рут,
// но все изменения вложенных нод будут происходить в старом руте - это багофича
const ContainerBaobabData = { name: 'container', attrs: { service: 'web' } };
const Container: React.FC<IContainerProps> = props => {
    const { BaobabProvider, nextContext } = useBaobab(ContainerBaobabData);

    return (
        <BaobabProvider value={nextContext}>
            {props.children}
            {props.hasClose ? <Button>Close</Button> : null}
        </BaobabProvider>
    );
};

interface IButtonProps extends ILoggable {
    children: string;
}

const Button: React.FC<IButtonProps> = props => {
    // В этом компоненте nodeData не мемоизируется, поэтому при каждом ререндере ноды будут пересоздаваться
    useBaobab({ name: 'button', attrs: { text: props.children } }, props.logNode);
    return <button>{props.children}</button>;
};

const MemoButton: React.FC<IButtonProps> = props => {
    // При ререндере считаем ноды с одинаковыми text одной и той же нодой
    const baobabData = React.useMemo<INodeInfo>(
        () => ({ name: 'button', attrs: { text: props.children } }),
        [props.children],
    );
    useBaobab(baobabData, props.logNode);

    return <button>{props.children}</button>;
};
const restoreInfo: ISsrNodeInfo<INodeContext> = {
    id: 'r-root',
    context: { ui: 'NodeJS', service: 'test', genInfo: { prefix: 'rid-' } },
};

const baobabState = getBaobabInitState({
    reqId: '1569...-12681',
    service: 'ugc',
    table: 'ugc',
    pageUrl: 'yandex.ru/ugcpub',
    slots: ['123456,0,5'],
    Sender,
});

describe('useBaobab', function() {
    it('log nodes after render without mount', function() {
        const logger = new Logger(baobabState);

        render(
            <BaobabAppendTo logger={logger}>
                <Container>
                    <Button>OK</Button>
                    <Button>Cancel</Button>
                </Container>
            </BaobabAppendTo>,
        );

        assert.deepEqual(logger.getRootSendNode(), {
            attrs: { 'schema-ver': 0, service: 'web' },
            id: 'j23p0',
            name: 'container',
            children: [
                {
                    attrs: { text: 'OK' },
                    id: 'j23p1',
                    name: 'button',
                },
                {
                    attrs: { text: 'Cancel' },
                    id: 'j23p2',
                    name: 'button',
                },
            ],
        });
    });

    it('log nodes after render without mount and use logNode', function() {
        const logger = new Logger(baobabState);

        render(
            <BaobabAppendTo logger={logger}>
                <Container>
                    <Button logNode={{ name: 'ok', attrs: { extraOk: 'value-ok' } }}>OK</Button>
                    <Button logNode={{ name: 'cancel', attrs: { extraCancel: 'value-cancel' } }}>Cancel</Button>
                </Container>
            </BaobabAppendTo>,
        );

        assert.deepEqual(logger.getRootSendNode(), {
            attrs: { 'schema-ver': 0, service: 'web' },
            id: 'j23p0',
            name: 'container',
            children: [
                {
                    attrs: { text: 'OK', extraOk: 'value-ok' },
                    id: 'j23p1',
                    name: 'ok',
                },
                {
                    attrs: { text: 'Cancel', extraCancel: 'value-cancel' },
                    id: 'j23p2',
                    name: 'cancel',
                },
            ],
        });
    });

    it('append nodes to the tree on rerender when baobab data is not memoised', function() {
        const logger = new Logger(baobabState);

        const ContainerWrapper: React.FC<IContainerProps> = props => {
            return (
                <BaobabAppendTo logger={logger}>
                    <Container hasClose={props.hasClose}>
                        <Button>OK</Button>
                        <Button>Cancel</Button>
                    </Container>
                </BaobabAppendTo>
            );
        };

        const component = mount(<ContainerWrapper />);
        component.setProps({ hasClose: true });

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // При ререндере useBaobab вызывается с новыми объектами nodeData,
                        // поэтому в дереве добавились новые ноды
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p3',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p5',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p6',
                            name: 'button',
                        },
                    ],
                });
                component.setProps({ hasClose: false });

                return new Promise(resolve => setTimeout(resolve, 10));
            }).then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // При ререндере useBaobab вызывается с новыми объектами nodeData,
                        // поэтому в дереве добавились новые ноды
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p3',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p5',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p6',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p7',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p9',
                            name: 'button',
                        },
                    ],
                });
            });
    });

    it('reuse nodes on rerender when using LoggerWithCache', function() {
        const logger = new LoggerWithCache(baobabState);

        const ContainerWrapper: React.FC<IContainerProps> = props => {
            return (
                <BaobabAppendTo logger={logger}>
                    <Container hasClose={props.hasClose}>
                        <Button>OK</Button>
                        <Button>Cancel</Button>
                    </Container>
                </BaobabAppendTo>
            );
        };

        const component = mount(<ContainerWrapper />);
        component.setProps({ hasClose: true });

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // При ререндере useBaobab вызывается с новыми объектами nodeData,
                        // поэтому в дереве добавились новые ноды
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p3',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p5',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p6',
                            name: 'button',
                        },
                    ],
                });

                component.setProps({ hasClose: false });
                return new Promise(resolve => setTimeout(resolve, 10));
            })
            .then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // После ререндера переиспользованы старые ноды
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p3',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p5',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p6',
                            name: 'button',
                        },
                    ],
                });
            });
    });

    it('update tree on rerender when baobab data is memoised', function() {
        const logger = new Logger(baobabState);

        const ContainerWrapper: React.FC<IContainerProps> = props => {
            return (
                <BaobabAppendTo logger={logger}>
                    <Container hasClose={props.hasClose}>
                        <MemoButton>OK</MemoButton>
                        <MemoButton>Cancel</MemoButton>
                    </Container>
                </BaobabAppendTo>
            );
        };

        const component = mount(<ContainerWrapper />);
        component.setProps({ hasClose: true });

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // При ререндере useBaobab вызывается с мемоизированными объектами nodeData,
                        // поэтому ноды не пересоздаются, а обновляются
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p3',
                            name: 'button',
                        },
                    ],
                });

                component.setProps({ hasClose: false });

                return new Promise(resolve => setTimeout(resolve, 10));
            })
            .then(() => {
                assert.deepEqual(logger.getRootSendNode(), {
                    attrs: { service: 'web', 'schema-ver': 0 },
                    id: 'j23p0',
                    name: 'container',
                    children: [
                        // При ререндере useBaobab вызывается с мемоизированными объектами nodeData,
                        // поэтому ноды не пересоздаются, а обновляются
                        {
                            attrs: { text: 'OK' },
                            id: 'j23p1',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Cancel' },
                            id: 'j23p2',
                            name: 'button',
                        },
                        {
                            attrs: { text: 'Close' },
                            id: 'j23p3',
                            name: 'button',
                        },
                    ],
                });
            });
    });

    it('create tree with restored root node', function() {
        const logger = new Logger(baobabState);
        const parentNode = logger.restoreNode(restoreInfo);

        const ContainerWrapper: React.FC<IContainerProps> = props => {
            return (
                <BaobabAppendTo logger={logger} parentNode={parentNode}>
                    <Container hasClose={props.hasClose}>
                        <Button>OK</Button>
                        <Button>Cancel</Button>
                    </Container>
                </BaobabAppendTo>
            );
        };

        mount(<ContainerWrapper />);

        return new Promise(resolve => setTimeout(resolve, 10))
            .then(() => {
                assert.deepEqual(logger.createSendNode(parentNode), {
                    id: 'r-root',
                    name: '',
                    children: [{
                        id: 'rid-0',
                        name: 'container',
                        attrs: { service: 'web' },
                        children: [
                            {
                                attrs: { text: 'OK' },
                                id: 'rid-1',
                                name: 'button',
                            },
                            {
                                attrs: { text: 'Cancel' },
                                id: 'rid-2',
                                name: 'button',
                            },
                        ],
                    }],
                });
            });
    });
});
