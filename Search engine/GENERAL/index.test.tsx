import React from 'react';
import {render, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {getSuggestOptionTestId} from '@metrics/react';

import {ApiCommonMethodReturnType} from '../../../api';
import {server, rest} from '../../../../jestSetup/mswServerSetup';
import {QueryClientProvider} from '../../../components/QueryClientProvider';

import {UserSuggest} from './index';

import data from './mocks/data.json';

describe('UserSuggest', () => {
    beforeEach(() => {
        server.use(
            rest.get(
                'https://search.yandex-team.ru/suggest',
                (_req, res, ctx) => {
                    return res(
                        ctx.json<ApiCommonMethodReturnType<'getUsers'>>(data),
                    );
                },
            ),
        );
    });

    test('default placeholder', async () => {
        const screen = render(
            <QueryClientProvider>
                <UserSuggest onChange={jest.fn} />
            </QueryClientProvider>,
        );

        expect(screen.getByText(/None/i).closest('button')).toBeInTheDocument();
    });
    test('owner button click should triggered onChange callback', async () => {
        const handleChange = jest.fn();

        const screen = render(
            <QueryClientProvider>
                <UserSuggest onChange={handleChange} ownerLogin="some_login" />
            </QueryClientProvider>,
        );

        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        const ownerButton = screen.getByText(/Me/i).closest('button')!;

        expect(ownerButton).toBeInTheDocument();

        userEvent.click(ownerButton);

        expect(handleChange).toHaveBeenCalledTimes(1);
    });

    test('user can select options', async () => {
        const handleClick = jest.fn();
        const testId = 'UserSuggest';

        const screen = render(
            <QueryClientProvider>
                <UserSuggest
                    onChange={handleClick}
                    nullOption="All"
                    testId={testId}
                />
            </QueryClientProvider>,
        );

        const button = await screen.findByTestId(testId);

        await waitFor(() => expect(button).not.toBeDisabled());

        userEvent.click(button);

        const option = await screen.findByTestId(
            getSuggestOptionTestId(testId, 0),
        );

        expect(option).toMatchSnapshot();

        userEvent.click(option);

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
