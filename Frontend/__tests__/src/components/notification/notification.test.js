import React from 'react';
import { mount } from 'enzyme';
import { addTranslation, setTankerProjectId } from 'react-tanker';

import { withRegistry } from '@bem-react/di';
import { compose } from '@bem-react/core';

import registry from '../../../../src/components/registry';

import NotificationBase from '../../../../src/components/notification/index';
import { Menu } from '../../../../src/components/notification/menu';
import NotificationCenterContext from '../../../../src/components/notification-center/context';

const Notification = withRegistry(registry)(NotificationBase);

const services = [
    {
        block_collection: 'znatoki',
        icon_src: 'https://avatars.mds.yandex.net/get-ya_notification_center/872891/znatoki-icon/svg',
    },
];

setTankerProjectId('global_notifications');
addTranslation(LANG, require('../../../../src/i18n/' + LANG + '.js'));

describe('Notification', () => {
    const getState = () => ({ services, config: { logging: { exp_boxes: '123' } } });
    const dispatch = jest.fn(cb => Promise.resolve());

    const context = {
        store: { getState, dispatch },
        metrika: { countClick: jest.fn(), countActions: jest.fn() },
    };

    const recordId = 123456789;
    const position = 1;
    const block = {
        service: 'znatoki',
        type: 'some_type',
        collectionId: 'znatoki',
        recordId,
        is_read: false,
        action_link: '//yandex.ru/action',
        mtime: 1565973051997 - 86400000 * 10,
        message: [{ type: 'user', text: 'Harold' }, ' says he is tired…'],
        meta: { actor: { link: '//yandex.ru' }, counterParams: { custom: 'param' } },
    };
    const readBlock = Object.assign({}, block, { is_read: true });
    const blockCounterVars = {
        block_id: 123456789,
        custom: 'param',
        exp_boxes: '123',
        height: 0,
        position: 1,
        is_new: 0,
        is_read: 0,
        record_id: 123456789,
        service: 'znatoki',
        type: 'some_type',
    };
    const blockReadCounterVars = Object.assign({}, blockCounterVars, { is_read: 1 });
    const clickCounterStub = {
        cid: 73275,
        path: '3027.241.882',
        vars: blockCounterVars,
    };
    const clickReadCounterStub = {
        cid: 73275,
        path: '3027.241.882',
        vars: blockReadCounterVars,
    };
    const readCounterStub = {
        cid: 73277,
        path: '3027.241.3129',
        vars: blockCounterVars,
    };
    const kebabCounterStub = {
        path: '3027.241.2729.2374',
        cid: 73277,
        vars: blockCounterVars,
    };

    describe('types', () => {
        test('should render simple unread message', () => {
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Notification block={block} />
                </NotificationCenterContext.Provider>,
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render promo message', () => {
            const promo = Object.assign({}, block, { isPromoPreview: true });
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Notification block={promo} />
                </NotificationCenterContext.Provider>,
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render simple read message', () => {
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Notification block={readBlock} />
                </NotificationCenterContext.Provider>,
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render unread message with preview', () => {
            const previewBlock = Object.assign({}, block, {
                preview: 'image',
                preview_src: 'https://avatars.mds.yandex.net/get-ya_notification_center/872891/znatoki-icon/svg',
                meta: {
                    actor: { link: '//yandex.ru' },
                    image: { type: 'resource' },
                },
            });

            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Notification block={previewBlock} />
                </NotificationCenterContext.Provider>,
            );

            expect(component.html()).toMatchSnapshot();
        });
    });

    describe('events', () => {
        describe('click', function() {
            test('should send 2 clck on unread block', () => {
                const clckCounter = jest.fn();
                const clckContext = Object.assign({}, context, { clckCounter });
                const store = { getState };

                const component = mount(
                    <NotificationCenterContext.Provider value={clckContext}>
                        <Notification position={0} block={block} />
                    </NotificationCenterContext.Provider>,
                );
                document.getElementsByClassName = jest.fn((...args) =>
                    component.getDOMNode().getElementsByClassName(args),
                );

                component.find('.gnc-notifications-item__link').simulate('click');

                expect(clckCounter).toBeCalledTimes(2);
                expect(clckCounter).nthCalledWith(1, clickCounterStub);
                expect(clckCounter).nthCalledWith(2, readCounterStub);
            });

            test('should send 1 clck on read block', () => {
                const clckCounter = jest.fn();
                const clckContext = Object.assign({}, context, { clckCounter });

                const component = mount(
                    <NotificationCenterContext.Provider value={clckContext}>
                        <Notification position={0} block={readBlock} />
                    </NotificationCenterContext.Provider>,
                );

                document.getElementsByClassName = jest.fn((...args) =>
                    component.getDOMNode().getElementsByClassName(args),
                );
                component.find('.gnc-notifications-item__link').simulate('click');

                expect(clckCounter).toBeCalledTimes(1);
                expect(clckCounter).nthCalledWith(1, clickReadCounterStub);
            });
        });

        describe('kebab', () => {
            test('should handle kebab events', () => {
                const clckCounter = jest.fn();
                const clckContext = Object.assign({}, context, { clckCounter });

                const component = mount(
                    <NotificationCenterContext.Provider value={clckContext}>
                        <Notification position={0} block={block} />
                    </NotificationCenterContext.Provider>,
                );

                document.getElementsByClassName = jest.fn((...args) =>
                    component.getDOMNode().getElementsByClassName(args),
                );
                const menu = component.find(Menu);
                const kebab = menu.find('.gnc-notifications-item__menu-kebab');
                const option = menu.find('.gnc-notifications-item__popup-option');

                kebab.simulate('mouseenter');

                // Menu is opened / has one option
                expect(menu.state('popupVisible')).toBe(true);
                expect(option.length).toBe(1);

                option.simulate('click');

                // menu is closed
                expect(menu.state('popupVisible')).toBe(false);

                expect(clckCounter).toBeCalledTimes(2);
                expect(clckCounter).nthCalledWith(1, readCounterStub);
                expect(clckCounter).nthCalledWith(2, kebabCounterStub);
            });
        });
    });
});
