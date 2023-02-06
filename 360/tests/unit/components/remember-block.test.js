import '../noscript';
import React from 'react';
import { mount } from 'enzyme';
import { RememberBlock } from '../../../components/redux/components/remember-block/remember-block';
import { Button } from '@ps-int/ufo-rocks/lib/components/lego-components/Button';

jest.mock('@ps-int/ufo-rocks/lib/components/virtual-grid/grids/layouts', () => ({
    maybeFetchLayout: jest.fn(() => Promise.resolve()),
    getLoadedLayout: jest.fn(() => () => {}),
}));

describe('RememberBlock', () => {
    Ya.Rum.sendHeroElement = jest.fn;

    const props = {
        createAlbumFromBlock: jest.fn(),
        openResourceById: jest.fn(),
        setScrollToResource: jest.fn(),
        fetchRememberBlock: jest.fn(),
        goToUrl: jest.fn(),
        useWowGrid: true,
        selectedResources: [],
        highlighted: [],
        onToggleSelected: jest.fn(),
        rememberBlock: {
            isLoading: false,
            id: '000000157105677876403100000001568985645112',
            title_1: 'Яркий день',
            title_2: '20 мая 2019',
            generation_type: 'default_WEEKEND',
            photoslice_date: '2019-05-20T14:33:13.000Z',
            photoslice_link_text: 'Все фото за день',
            bestResourceId: '/photounlim/2019-06-04 13-04-23.JPG',
            isRemoved: false,
            resourcesSizesByIds: {
                '/photounlim/file1': {
                    beauty: 1.2,
                    height: 2560,
                    width: 1440
                },
                '/photounlim/file2': {
                    beauty: -1.2,
                    height: 4160,
                    width: 3120
                },
                '/photounlim/file3': {
                    beauty: 2.2,
                    height: 1080,
                    width: 1920
                },
            }
        },
        scrollToResource: {},
        resources: [
            {
                canPlayVideo: false,
                defaultPreview: 'file1preview',
                disabled: undefined,
                etime: 1559642667,
                id: '/photounlim/file1',
                mediatype: 'image',
                meta: {
                    drweb: 1,
                    etime: 1559642667,
                    ext: 'jpg',
                    file_id: '3076099327c1f34ad9cf76cf682873a827cdd75f84aef4ddf3a7fd8367c0ef9a',
                    hasPreview: true,
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    photoslice_time: 1559642667,
                    resource_id: '4017426232:3076099327c1f34ad9cf76cf682873a827cdd75f84aef4ddf3a7fd8367c0ef9a',
                    size: 2070339,
                    sizes: []
                },
                mtime: 1563185947,
                parents: [
                    { id: '/disk', name: 'Файлы' },
                    { id: '/photo', name: 'Все фото' }
                ],
                state: {},
                name: 'File 1',
                type: 'file',
                xxxlPreview: 'file1XXXLPreview'
            },
            {
                canPlayVideo: false,
                defaultPreview: 'file2preview',
                disabled: undefined,
                etime: 1529402591,
                id: '/photounlim/file2',
                mediatype: 'image',
                meta: {
                    drweb: 1,
                    etime: 1529402591,
                    ext: 'jpg',
                    file_id: '0215f3716bf7694700cf2c291332d5ba155aeef2eb863c494947c65d36c63d03',
                    hasPreview: true,
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    photoslice_time: 1559642667,
                    resource_id: '4017426232:0215f3716bf7694700cf2c291332d5ba155aeef2eb863c494947c65d36c63d03',
                    size: 156508,
                    sizes: []
                },
                mtime: 1562244269,
                parents: [
                    { id: '/disk', name: 'Файлы' },
                    { id: '/photo', name: 'Все фото' }
                ],
                state: {},
                name: 'File 2',
                type: 'file',
                xxxlPreview: 'file2XXXLPreview'
            },
            {
                canPlayVideo: false,
                defaultPreview: 'file3preview',
                disabled: undefined,
                etime: 1559642667,
                id: '/photounlim/file3',
                mediatype: 'image',
                meta: {
                    drweb: 1,
                    etime: 1559642667,
                    ext: 'jpg',
                    file_id: '3076099327c1f34ad9cf76cf682873a827cdd75f84aef4ddf3a7fd8367c0ef9a',
                    hasPreview: true,
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    photoslice_time: 1559642667,
                    resource_id: '4017426232:3076099327c1f34ad9cf76cf682873a827cdd75f84aef4ddf3a7fd8367c0ef9a',
                    size: 2070339,
                    sizes: []
                },
                mtime: 1563185947,
                parents: [
                    { id: '/disk', name: 'Файлы' },
                    { id: '/photo', name: 'Все фото' }
                ],
                state: {},
                name: 'File 3',
                type: 'file',
                xxxlPreview: 'file3XXXLPreview'
            },
        ],
        isEmpty: false,
        album: undefined,
        isAndroid: false,
        isIosSafari: false,
        OSFamily: 'MacOS',
        staticHost: 'https://yandex.net',
        idContext: '/remember/000000157105677876403100000001568985645112'
    };

    it('should mount RememberBlock', (done) => {
        const wrapper = mount(<RememberBlock {...props} />);
        setTimeout(() => {
            wrapper.update();
            expect(wrapper).toMatchSnapshot();
            done();
        }, 100);
    });

    it('should open photoslice on button click', () => {
        const wrapper = mount(<RememberBlock {...props} />);
        wrapper.find('.remember-block__buttons').find(Button).at(0).simulate('click');

        expect(props.goToUrl).toHaveBeenCalledWith('/client/photo?from=1558362793000');
    });
});
