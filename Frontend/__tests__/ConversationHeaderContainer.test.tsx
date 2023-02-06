window.Notification = { permission: '' } as any;

jest.mock('react-loadable', () => {
    return {
        default: {
            Map: () => () => null,
        },
    };
});

jest.mock('../../services/HeartbeatController');
jest.mock('../../store', () => ({
    useCachedSelector: jest.fn(),
}));

import * as React from 'react';
import { render } from '@testing-library/react';

import ConversationHeaderContainer from '../ConversationHeaderContainer';
import NotificationPromoLink from '../../components/NotificationPromoLink';

declare var global: NodeJS.Global & { FLAGS: Record<string, boolean> };

function setPermission(value) {
    (window.Notification as any).permission = value;
}

function wrapperFactory(flags, permission) {
    window.flags = flags;
    setPermission(permission);
    return render(
        <ConversationHeaderContainer />,
    ).baseElement;
}

describe('ConversationHeaderContainer', () => {
    describe('should be empty on internal', () => {
        const flags = { internal: '1' };

        it('permission - \'\'', () => {
            const wrapper = wrapperFactory(flags, 'foot');
            expect(wrapper).toHaveTextContent('');
        });

        it('permission - foo', () => {
            const wrapper = wrapperFactory(flags, 'foo');

            expect(wrapper).toHaveTextContent('');
        });

        it('permission - granted', () => {
            const wrapper = wrapperFactory(flags, 'granted');
            expect(wrapper).toHaveTextContent('');
        });

        afterEach(() => {
            window.flags = {};
            setPermission('');
        });
    });

    describe('external', () => {
        describe('should be empty without NOTIFICATION_SUGGEST', () => {
            const flags = { internal: '0', notificationSuggest: '0' };

            it('permission - \'\'', () => {
                const wrapper = wrapperFactory(flags, '');
                expect(wrapper).toHaveTextContent('');
            });

            it('permission - foo', () => {
                const wrapper = wrapperFactory(flags, 'foo');

                expect(wrapper).toHaveTextContent('');
            });

            it('permission - granted', () => {
                const wrapper = wrapperFactory(flags, 'granted');
                expect(wrapper).toHaveTextContent('');
            });

            afterEach(() => {
                window.flags = {};
                setPermission('');
            });
        });

        describe('should not be empty with NOTIFICATION_SUGGEST', () => {
            const flags = { internal: '0', notificationSuggest: '1' };
            let promoContent: string;

            it('permission - \'\'', () => {
                promoContent = render(<NotificationPromoLink onClick={() => {}} />).baseElement.innerHTML;
                const wrapper = wrapperFactory(flags, '');
                expect(wrapper).toContainHTML(promoContent);
            });

            it('permission - foo', () => {
                promoContent = render(<NotificationPromoLink onClick={() => {}} />).baseElement.innerHTML;
                const wrapper = wrapperFactory(flags, 'foo');

                expect(wrapper).toContainHTML(promoContent);
            });

            it('permission - granted', () => {
                const wrapper = wrapperFactory(flags, 'granted');
                expect(wrapper).toHaveTextContent('');
            });

            afterEach(() => {
                window.flags = {};
                setPermission('');
            });
        });
    });
});
