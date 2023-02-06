import { shallow, ShallowWrapper } from 'enzyme';
import React from 'react';
import { Service } from './Service';

import { getService } from '../testData/testData';
import { Service as IService } from '../../../redux/types/requests';

const checkServiceLink = (serviceLinkWrapper: ShallowWrapper, service: IService) => {
    expect(serviceLinkWrapper.prop('view')).toEqual('default');
    expect(serviceLinkWrapper.prop('target')).toEqual('_blank');
    expect(serviceLinkWrapper.prop('href')).toEqual(`/services/${service.slug}/`);
    expect(serviceLinkWrapper.prop('children')).toEqual(service.name.ru);
};

describe('Service', () => {
    it('Should have service and parent links with correct props', () => {
        const newService = getService(100);
        const newParentService = getService(200);

        const wrapper = shallow(
            <Service
                newService={newService}
                newParentService={newParentService}
            />,
        );

        const serviceBlock = wrapper.find('.Requests-Service');

        const newServiceLink = serviceBlock.find('.Requests-ServicePart_type_newService');
        const newParentServiceLink = serviceBlock.find('.Requests-ServicePart_type_newParentService');

        checkServiceLink(newServiceLink, newService);
        checkServiceLink(newParentServiceLink, newParentService);
    });

    it('Should render no new parent link', () => {
        const newService = getService(100);

        const wrapper = shallow(
            <Service
                newService={newService}
            />,
        );

        const serviceBlock = wrapper.find('.Requests-Service');

        const newParentServiceLink = serviceBlock.find('.Requests-ServicePart_type_newParentService');

        expect(newParentServiceLink).toHaveLength(0);
    });
});
