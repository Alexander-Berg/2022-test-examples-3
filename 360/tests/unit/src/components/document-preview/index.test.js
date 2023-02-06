import DocumentPreview from '../../../../../src/components/document-preview';

import React from 'react';
import { render } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

const runTest = (state, resourceId) => {
    const store = getStore(Object.assign({
        rootResourceId: resourceId,
        services: { docviewer: 'https://docviewer.yandex.ru/' },
        ua: {},
        environment: {}
    }, state));
    const component = render(
        <Provider store={store}>
            <DocumentPreview
                resourceId={resourceId}
            />
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

const resourceId = 'resourceId1';

describe('document-preview component (web) =>', () => {
    it('Нет превью', () => {
        runTest({
            resources: {
                [resourceId]: {
                    id: resourceId,
                    meta: {
                        ext: 'exe',
                        mediatype: 'executable'
                    }
                }
            }
        }, resourceId);
    });

    it('Есть превью', () => {
        runTest({
            resources: {
                [resourceId]: {
                    id: resourceId,
                    meta: {
                        ext: 'doc',
                        mediatype: 'word',
                        defaultPreview: 'preview-link-for-doc'
                    }
                }
            }
        }, resourceId);
    });

    it('Нет превью, но есть ссылка на DV', () => {
        runTest({
            resources: {
                [resourceId]: {
                    id: resourceId,
                    dvSearch: '?url=dv-url-for-zip',
                    meta: {
                        ext: 'zip',
                        mediatype: 'compressed'
                    }
                }
            }
        }, resourceId);
    });

    it('Есть превью и ссылка на DV', () => {
        runTest({
            resources: {
                [resourceId]: {
                    id: resourceId,
                    dvSearch: '?url=dv-url-for-fb2',
                    meta: {
                        ext: 'fb2',
                        mediatype: 'compressed',
                        defaultPreview: 'preview-link-for-fb2'
                    }
                }
            }
        }, resourceId);
    });

    it('Вирусный файл (с превью и ссылкой на DV)', () => {
        runTest({
            resources: {
                [resourceId]: {
                    id: resourceId,
                    dvSearch: '?url=dv-url-for-virus',
                    virus: true,
                    meta: {
                        ext: 'epub',
                        mediatype: 'compressed',
                        defaultPreview: 'preview-link-for-virus'
                    }
                }
            }
        }, resourceId);
    });
});

describe('document-preview component (APP) =>', () => {
    beforeEach(() => {
        global.APP = true;
    });
    afterEach(() => {
        global.APP = false;
    });

    it('Без превью', () => {
        runTest({
            rootResourceId: resourceId,
            resources: {
                [resourceId]: {
                    id: resourceId,
                    meta: {
                        ext: 'rar',
                        mediatype: 'archive'
                    }
                }
            }
        }, resourceId);
    });

    it('С превью', () => {
        runTest({
            rootResourceId: resourceId,
            resources: {
                [resourceId]: {
                    id: resourceId,
                    meta: {
                        ext: 'xlsx',
                        mediatype: 'excel',
                        defaultPreview: 'preview-link-for-xlsx'
                    }
                }
            }
        }, resourceId);
    });
});
