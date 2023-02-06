import {getBreadcrumbsFromLocation, isValidBreadcrumbs} from '../utils';
import {validBreadcrumbs, invalidBreadcrumbs} from './fixtures';

declare const test: jest.It;

describe('JmfValuesDispatcher', () => {
    test.each(validBreadcrumbs)('"%p" is valid breadcrumbs', input => {
        expect(isValidBreadcrumbs(input)).toBeTruthy();
    });

    test.each(invalidBreadcrumbs)('"%p" is not valid path', input => {
        expect(isValidBreadcrumbs(input)).toBeFalsy();
    });

    test.each`
        paths                                         | parents                                  | result
        ${['*']}                                      | ${['ticket@81821703']}                   | ${'*'}
        ${['*', 'order-ticket']}                      | ${['ticket@81821703']}                   | ${'*'}
        ${['*', 'ticket']}                            | ${['ticket@81821703']}                   | ${'ticket'}
        ${['*', 'ticket-order']}                      | ${['ticket@81821703', 'order@81821703']} | ${'ticket-order'}
        ${['*', 'ticket-order', 'ticket-user-order']} | ${['ticket@81821703', 'order@81821703']} | ${'ticket-order'}
        ${['*', 'order', 'user']}                     | ${['ticket@81821703']}                   | ${'*'}
    `('$paths, $parents => $result', ({paths, parents, result}) => {
        expect(getBreadcrumbsFromLocation(paths, parents)).toBe(result);
    });
});
