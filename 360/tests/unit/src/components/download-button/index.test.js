import DownloadButton from '../../../../../src/components/download-button';
import { OPERATION_STATES } from '../../../../../src/store/constants';

import React from 'react';
import { render, mount } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

jest.mock('../../../../../src/store/async-actions');
import { download } from '../../../../../src/store/async-actions';

jest.mock('../../../../../src/store/actions');
import { openDownloadVirusDialog } from '../../../../../src/store/actions';

const rootResourceId = 'root-resource-id';
const nestedResourceId = 'nested-resource-id';

const getState = (resourceId, { type, virus, antiFileSharing, downloadState } = {}) => getStore({
    rootResourceId,
    resources: Object.assign({
        [rootResourceId]: {
            meta: {}
        }
    }, {
        [resourceId]: {
            type,
            virus,
            meta: {}
        }
    }),
    environment: {
        antiFileSharing,
        experiments: { flags: {} }
    },
    operations: {
        download: downloadState ?
            {
                [resourceId]: downloadState
            } :
            {}
    }
});

const runTest = ({ resourceId, hasText, hasIcon } = {}, state) => {
    const component = render(
        <Provider store={getState(resourceId, state)}>
            <DownloadButton
                resourceId={resourceId}
                hasText={hasText}
                hasIcon={hasIcon}
            />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

describe('download-button =>', () => {
    it('дефолтное состояние', () => {
        runTest({
            resourceId: rootResourceId
        });
    });

    it('с иконкой', () => {
        runTest({
            resourceId: rootResourceId,
            hasIcon: true
        });
    });

    it('без текста (только иконка)', () => {
        runTest({
            resourceId: rootResourceId,
            hasIcon: true,
            hasText: false
        });
    });

    it('для корневой папки ("Скачать всё")', () => {
        runTest({
            resourceId: rootResourceId
        }, {
            type: 'dir'
        });
    });

    it('для альбома ("Скачать всё")', () => {
        runTest({
            resourceId: rootResourceId
        }, {
            type: 'album'
        });
    });

    it('для вложенной папки ("Скачать")', () => {
        runTest({
            resourceId: nestedResourceId
        }, {
            type: 'dir'
        });
    });

    it('в процессе получения ссылки на скачивание', () => {
        runTest({
            resourceId: rootResourceId
        }, {
            downloadState: OPERATION_STATES.IN_PROGRESS
        });
    });

    it('клик по кнопке', () => {
        const component = mount(
            <Provider store={getState(nestedResourceId)}>
                <DownloadButton resourceId={nestedResourceId}/>
            </Provider>
        );
        expect(popFnCalls(download).length).toEqual(0);
        component.simulate('click');
        const downloadCalls = popFnCalls(download);
        expect(downloadCalls.length).toEqual(1);
        expect(downloadCalls[0]).toEqual([nestedResourceId]);
        component.unmount();
    });

    it('вирусный файл', () => {
        const component = mount(
            <Provider store={getState(nestedResourceId, { virus: true })}>
                <DownloadButton resourceId={nestedResourceId}/>
            </Provider>
        );

        expect(component.render()).toMatchSnapshot();
        component.simulate('click');

        expect(popFnCalls(download).length).toEqual(0);
        const openDownloadVirusDialogCalls = popFnCalls(openDownloadVirusDialog);
        expect(openDownloadVirusDialogCalls.length).toEqual(1);
        expect(openDownloadVirusDialogCalls[0]).toEqual([nestedResourceId]);

        component.unmount();
    });
});
