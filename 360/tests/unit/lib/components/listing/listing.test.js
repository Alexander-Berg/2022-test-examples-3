import React from 'react';
import { mount } from 'enzyme';

import { resourcesSelectorCreator } from '../../../../../lib/store/selectors/resources';
import Listing from '../../../../../lib/components/listing';

const rootResourceId = 'root-resource-id';
const resources = {
    [rootResourceId]: {
        id: rootResourceId,
        loading: false,
        completed: true,
        children: ['resource-1', 'resource-2', 'resource-3'],
        type: 'dir',
        name: 'Root directory',
        defaultPreview: '',
        meta: {}
    },
    'resource-1': {
        id: 'resource-1',
        type: 'file',
        name: 'Moi-trusiki.jpg',
        defaultPreview: 'image-preview-url',
        meta: {
            mediatype: 'image'
        }
    },
    'resource-2': {
        id: 'resource-2',
        type: 'file',
        name: 'Документ.docx',
        defaultPreview: 'document-preview-url',
        meta: {
            mediatype: 'document'
        }
    },
    'resource-3': {
        id: 'resource-3',
        type: 'file',
        name: 'Тыж программист.mp3',
        meta: {
            mediatype: 'audio'
        }
    }
};

const resourcesSelector = ({ resources }) => resources[rootResourceId].children.map((resourceId) => resources[resourceId]);
const getResourcesSelector = resourcesSelectorCreator(resourcesSelector, (resources) => resources);
const getListingProps = (resources) => {
    return {
        currentResource: resources[rootResourceId],
        resources: getResourcesSelector({ resources }),
        completed: resources[rootResourceId].completed,
        loading: resources[rootResourceId].loading,
        isTouch: false,
        OSFamily: 'Windows',
        requestFetchNext: () => {}
    };
};

describe('listing', () => {
    describe('re-renders', () => {
        it('should not re-render if none of listing resources changed (but current resource changed)', () => {
            const wrapper = mount(
                <Listing {...getListingProps(resources)}/>
            );
            wrapper.render();
            const listingItems = wrapper.find('ListingItems').instance();
            const listingItemsRenderSpy = jest.spyOn(listingItems, 'render');
            wrapper.setProps(getListingProps(Object.assign({}, resources, {
                [rootResourceId]: Object.assign({}, resources[rootResourceId], {
                    loading: true
                })
            })));
            expect(listingItemsRenderSpy).toHaveBeenCalledTimes(0);
            wrapper.unmount();
        });

        it('should re-render if any of listing resources changed', () => {
            const wrapper = mount(
                <Listing {...getListingProps(resources)}/>
            );
            wrapper.render();
            const listingItems = wrapper.find('ListingItems').instance();
            const listingItemsRenderSpy = jest.spyOn(listingItems, 'render');
            wrapper.setProps(getListingProps(Object.assign({}, resources, {
                'resource-1': Object.assign({}, resources['resource-1'], {
                    public: true
                })
            })));
            expect(listingItemsRenderSpy).toHaveBeenCalledTimes(1);
            wrapper.unmount();
        });

        it('_checkBottom should not throw when listing was unmounted', () => {
            const wrapper = mount(
                <Listing
                    {...Object.assign({}, getListingProps(resources), { completed: false })}
                />
            );
            const listingInstance = wrapper.instance();
            expect(() => {
                listingInstance._checkBottom();
            }).not.toThrow();
            wrapper.unmount();

            // _checkBottom can be called by throttle after unmount
            expect(() => {
                listingInstance._checkBottom();
            }).not.toThrow();
        });
    });
});
