import * as React from 'react';
import { shallow } from 'enzyme';
import { DescriptionPresenter } from '../Description';
import { DescriptionMeta } from '../Meta/Description-Meta';

describe('Блок Description', () => {
    it('Соответствует снепшоту', () => {
        const wrapper = shallow(
            <DescriptionPresenter>
                <div className="content">Случайный контент</div>
                <DescriptionMeta>
                    <div className="date">29.10.2020</div>
                    <div className="sociality-controls">stub</div>
                </DescriptionMeta>
            </DescriptionPresenter>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
