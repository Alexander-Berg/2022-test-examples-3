import * as React from 'react';
import { mount } from 'enzyme';

import { Chat } from '../Chat';
import {
    IProps,
    BadgeType,
    ButtonIcon,
    CollapsedDesktop,
    CollapsedTouch,
    Lang,
    Theme,
    EventType,
    ServiceId,
} from '../Chat.types';

type TAddListenerMock = jest.Mock<{}>;
type TShowChatMock = jest.Mock<{}>;
type TRemoveAllListenersMock = jest.Mock<{}>;

type TChatWIdgetConstructorMock = jest.Mock<{
    addListener: TAddListenerMock;
    showChat: TShowChatMock;
    removeAllListeners: TRemoveAllListenersMock;
}>;

describe('Chat', () => {
    let showChatFunc: TShowChatMock;
    let addListenerFunc: TAddListenerMock;
    let removeAllListenersFunc: TRemoveAllListenersMock;
    let chatWidgetConstructor: TChatWIdgetConstructorMock;

    const props = {
        guid: '123abc',
        autocloseable: false,
        badgeType: BadgeType.dot,
        badgeCount: 2,
        badgeMaxCount: 99,
        buttonIcon: ButtonIcon.colored,
        buttonText: 'buttonText',
        collapsedDesktop: CollapsedDesktop.always,
        collapsedTouch: CollapsedTouch.never,
        iframeUrl: 'https://yandex.ru/chat?build=chamb',
        iframeUrlParams: {
            header: 'disabled',
        },
        isMobile: false,
        lang: Lang.ru,
        mountNode: document.body,
        serviceId: ServiceId.lc,
        title: 'Чаты',
        theme: Theme.light,
        unreadUrl: 'unreadUrl',
        chatUrl: '/chat',
    };

    beforeEach(() => {
        showChatFunc = jest.fn();
        addListenerFunc = jest.fn();
        removeAllListenersFunc = jest.fn();
        chatWidgetConstructor = jest.fn(() => ({
            addListener: addListenerFunc,
            showChat: showChatFunc,
            removeAllListeners: removeAllListenersFunc,
        }));

        global.Ya = {
            ChatWidget: chatWidgetConstructor,
        };
    });

    function initializeChat(props: IProps) {
        const component = mount(<Chat {...props} />);

        return component;
    }

    it('should initialize chatwidget with params', () => {
        initializeChat(props);

        const options = {
            ...props,
            chatUrl: undefined,
        };

        expect(chatWidgetConstructor).toHaveBeenCalledWith(options);
    });

    it('should set listeners after mount', () => {
        const instance = initializeChat(props).instance();

        expect(addListenerFunc).toHaveBeenCalledTimes(6);
        expect(addListenerFunc).toHaveBeenNthCalledWith(1, EventType.ChatShow, instance.chatShowHandler);
        expect(addListenerFunc).toHaveBeenNthCalledWith(2, EventType.ChatHide, instance.chatHideHandler);
        expect(addListenerFunc).toHaveBeenNthCalledWith(3, EventType.ChatEnter, instance.chatEnterHandler);
        expect(addListenerFunc).toHaveBeenNthCalledWith(4, EventType.ChatLeave, instance.chatLeaveHandler);
        expect(addListenerFunc).toHaveBeenNthCalledWith(5, EventType.UnreadCounterChange, instance.counterChangeHandler);
        expect(addListenerFunc).toHaveBeenNthCalledWith(6, EventType.AuthRequest, instance.authRequestHandler);
    });

    it('should call showChat on click', () => {
        const component = initializeChat({
            ...props,
            theme: Theme.hidden,
            className: 'className',
            customIcon: <div />,
        });

        component.find('div.className').simulate('click');

        expect(showChatFunc).toHaveBeenCalledWith({
            guid: props.guid,
        });
    });

    it('should call provided method onChatShow', () => {
        const onChatShow = jest.fn();

        const component = initializeChat({
            ...props,
            onChatShow,
        });

        const instance = component.instance();

        instance.chatShowHandler();

        expect(onChatShow).toHaveBeenCalledWith(instance.chatWidget);
    });

    it('should call provided method onChatHide', () => {
        const onChatHide = jest.fn();

        const component = initializeChat({
            ...props,
            onChatHide,
        });

        const instance = component.instance();

        instance.chatHideHandler();

        expect(onChatHide).toHaveBeenCalledWith(instance.chatWidget);
    });

    it('should call provided method onChatEnter', () => {
        const onChatEnter = jest.fn();

        const component = initializeChat({
            ...props,
            onChatEnter,
        });

        const instance = component.instance();

        const args = {
            chatId: 'chatId',
            guid: 'guid',
        };

        instance.chatEnterHandler(args);

        expect(onChatEnter).toHaveBeenCalledWith(instance.chatWidget, args);
    });

    it('should call provided method onChatLeave', () => {
        const onChatLeave = jest.fn();

        const component = initializeChat({
            ...props,
            onChatLeave,
        });

        const instance = component.instance();

        const args = {
            chatId: 'chatId',
            guid: 'guid',
        };

        instance.chatLeaveHandler(args);

        expect(onChatLeave).toHaveBeenCalledWith(instance.chatWidget, args);
    });

    it('should call provided method onCounterChange', () => {
        const onCounterChange = jest.fn();

        const component = initializeChat({
            ...props,
            onCounterChange,
        });

        const instance = component.instance();

        const args = {
            value: 123,
            lastTimestamp: 998,
        };

        instance.counterChangeHandler(args);

        expect(onCounterChange).toHaveBeenCalledWith(instance.chatWidget, args);
    });

    it('should remove listeners on unmount', () => {
        initializeChat(props).unmount();

        expect(removeAllListenersFunc).toHaveBeenCalled();
    });
});
