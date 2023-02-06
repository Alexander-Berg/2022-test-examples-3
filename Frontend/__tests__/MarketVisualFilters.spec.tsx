import * as React from 'react';
import { mount } from 'enzyme';
import { MarketVisualFilters } from '../MarketVisualFilters';
import mock from './mock';

describe('MarketVisualFilters', () => {
    describe('События', () => {
        let handleValueChange;
        let handleResetClick;
        let filter;

        beforeEach(() => {
            handleValueChange = jest.fn();
            handleResetClick = jest.fn();

            filter = mount(
                //@ts-ignore
                <MarketVisualFilters
                    {...mock}
                    onValueChange={handleValueChange}
                    onResetClick={handleResetClick}
                />
            );
        });

        it('должен вызываться onResetClick', () => {
            filter.find('.market-visual-filters__reset').simulate('click');

            expect(handleResetClick).toHaveBeenCalledTimes(1);
        });

        it('должен вызываться handleValueChange', () => {
            filter.find('[data-autotest-id="14899397"]').simulate('change');

            expect(handleValueChange).toHaveBeenCalledTimes(1);
            expect(handleValueChange).toBeCalledWith({ filterId: '14871214', value: '14899397' });
        });

        it('handleValueChange не должен вызываться на disabled значении', () => {
            filter.find('[data-autotest-id="14896295"]').simulate('change');

            expect(handleValueChange).not.toBeCalled();
        });
    });
});
