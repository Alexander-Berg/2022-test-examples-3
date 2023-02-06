import React from 'react';
import { render } from '@testing-library/react';
import { RootPageFragment } from '~/test/jest/utils';
import { TableRow } from '~/src/features/Oebs/components/TableRow/TableRow';

jest.mock('~/src/common/hooks/useInternalServiceUrl');

describe('Oebs-TableRow', () => {
    it('should render table row without link', () => {
        const wrapper = new RootPageFragment(
            render(
                <table>
                    <tbody>
                        <TableRow
                            issue={null}
                            className={'TableRow'}
                        />
                    </tbody>
                </table>,
            ),
        );

        expect(wrapper.container?.querySelector('a.TableRow')).toBeFalsy();
        expect(wrapper.container?.querySelector('tr.TableRow')).toBeDefined();
    });

    it('should render table row with link', () => {
        const wrapper = new RootPageFragment(
            render(
                <TableRow
                    issue={'TEST-1'}
                    className={'TableRow'}
                />,
            ),
        );

        expect(wrapper.container?.querySelector('tr.TableRow')).toBeFalsy();
        expect(wrapper.container?.querySelector('a.TableRow')).toBeDefined();
        expect(wrapper.container?.querySelector('a.TableRow')?.getAttribute('href')).toBeDefined();
    });
});
