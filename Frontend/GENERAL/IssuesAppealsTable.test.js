import React from 'react';
import { render } from 'enzyme';

import { IssuesAppealsTable } from './IssuesAppealsTable';
import { getWrappedContent, getServiceLink } from './IssuesAppealsTable.utils';

const appealsMock = [
    {
        id: 0,
        issue: {
            name: { ru: 'Отсутствует описание' },
        },
        message: 'Не проблема',
        service: {
            name_i18n: { ru: 'Имя сервиса' },
            slug: 'serviceSlug',
        },
        state: 'requested',
    },
];

describe('Should render IssuesAppealsTable', () => {
    it('without items', () => {
        const wrapper = render(
            <IssuesAppealsTable
                appeals={[]}
                onApproveAppeal={jest.fn()}
                onRejectAppeal={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with items', () => {
        const wrapper = render(
            <IssuesAppealsTable
                appeals={appealsMock}
                onApproveAppeal={jest.fn()}
                onRejectAppeal={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});

describe('Should render utils components for IssuesAppealsTable', () => {
    it('ServiceLink', () => {
        const wrapper = render(
            getServiceLink(appealsMock[0].service)
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Wrapper', () => {
        const content = <div />;
        const wrapper = render(getWrappedContent(content));

        expect(wrapper).toMatchSnapshot();
    });

    it('Wrapper with className', () => {
        const content = <div />;
        const wrapper = render(getWrappedContent(content, 'content'));

        expect(wrapper).toMatchSnapshot();
    });
});
