import React from 'react';
import { render } from 'enzyme';

import PerfectionAppeal from './PerfectionAppeal';

describe('Should render PerfectionAppeal', () => {
    it('closed', () => {
        const wrapper = render(
            <PerfectionAppeal
                isOpen={false}
                openForm={() => {}}
                closeForm={() => {}}
                onSubmit={() => {}}
                text="Message"
                setText={() => {}}
                loading={false}
                error={null}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('opened', () => {
        const wrapper = render(
            <PerfectionAppeal
                isOpen
                openForm={() => {}}
                closeForm={() => {}}
                onSubmit={() => {}}
                text="Message"
                setText={() => {}}
                loading={false}
                error={null}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with error', () => {
        const wrapper = render(
            <PerfectionAppeal
                isOpen
                openForm={() => {}}
                closeForm={() => {}}
                onSubmit={() => {}}
                text="Message"
                setText={() => {}}
                loading={false}
                error={{ data: { detail: 'Error' }, extra: 'Error' }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with loading', () => {
        const wrapper = render(
            <PerfectionAppeal
                isOpen
                openForm={() => {}}
                closeForm={() => {}}
                onSubmit={() => {}}
                text="Message"
                setText={() => {}}
                loading
                error={null}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
