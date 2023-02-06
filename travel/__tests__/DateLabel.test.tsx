import {render, cleanup} from '@testing-library/react';

import DateLabel from '../DateLabel';

describe('<DateLabel />', () => {
    const date = new Date('2019-01-01');

    afterEach(cleanup);

    it('should pass className', () => {
        const className = 'my-class';
        const {container} = render(
            <DateLabel startDate={date} endDate={date} className={className} />,
        );

        if (!container.firstElementChild) {
            throw new Error('container not contain children');
        }

        expect(container.firstElementChild.getAttribute('class')).toEqual(
            className,
        );
    });

    it('should wrapped in span', () => {
        const {container} = render(
            <DateLabel startDate={date} endDate={date} />,
        );

        if (!container.firstElementChild) {
            throw new Error('container not contain children');
        }

        expect(container.firstElementChild.tagName).toEqual('SPAN');
    });
});
