import React from 'react';
import { shallow } from 'enzyme';

import AbcDutyDetails__Replacement from 'b:abc-duty-details e:replacement';

const { BEM_LANG } = process.env;

describe('Should render a temporary replacement', () => {
    const onDelete = jest.fn();
    const onRestore = jest.fn();

    onDelete.mockName('onDelete');
    onRestore.mockName('onRestore');

    it('in default state', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Replacement
                replacement={{
                    person: {
                        login: 'john.doe',
                        name: {
                            [BEM_LANG]: 'John Doe'
                        }
                    },
                    startDate: new Date(Date.UTC(2000, 1, 1)),
                    endDate: new Date(Date.UTC(2000, 1, 10))
                }}
                onDelete={onDelete}
                onRestore={onRestore}
                isDeleted={false}
                canEditDutySettings={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in deleted state', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Replacement
                replacement={{
                    person: {
                        login: 'john.doe',
                        name: {
                            [BEM_LANG]: 'John Doe'
                        }
                    },
                    startDate: new Date(Date.UTC(2000, 1, 1)),
                    endDate: new Date(Date.UTC(2000, 1, 10))
                }}
                onDelete={onDelete}
                onRestore={onRestore}
                isDeleted
                canEditDutySettings={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in common state with edit permissions', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Replacement
                replacement={{
                    person: {
                        login: 'john.doe',
                        name: {
                            [BEM_LANG]: 'John Doe'
                        }
                    },
                    startDate: new Date(Date.UTC(2000, 1, 1)),
                    endDate: new Date(Date.UTC(2000, 1, 10))
                }}
                onDelete={onDelete}
                onRestore={onRestore}
                isDeleted={false}
                canEditDutySettings
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in deleted state with edit permissions', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Replacement
                replacement={{
                    person: {
                        login: 'john.doe',
                        name: {
                            [BEM_LANG]: 'John Doe'
                        }
                    },
                    startDate: new Date(Date.UTC(2000, 1, 1)),
                    endDate: new Date(Date.UTC(2000, 1, 10))
                }}
                onDelete={onDelete}
                onRestore={onRestore}
                isDeleted
                canEditDutySettings
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('in loading state with edit permissions', () => {
        const wrapper = shallow(
            <AbcDutyDetails__Replacement
                replacement={{
                    person: {
                        login: 'john.doe',
                        name: {
                            [BEM_LANG]: 'John Doe'
                        }
                    },
                    startDate: new Date(Date.UTC(2000, 1, 1)),
                    endDate: new Date(Date.UTC(2000, 1, 10))
                }}
                onDelete={onDelete}
                onRestore={onRestore}
                isDeleted={false}
                isLoading
                canEditDutySettings
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
