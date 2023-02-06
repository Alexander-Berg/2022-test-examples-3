import { storiesOf, specs, describe, it, snapshot, mockFunction, fireEvent, Simulate, wait, render, serialize } from '../.storybook/facade';

import React from 'react';

import CreateButton from '../../../components/rocks/create-resource-popup-with-anchor';
import '../../../components/rocks/create-resource-popup-with-anchor/index.styl';
import '@ps-int/ufo-rocks/lib/components/resource-icon';
import '@ps-int/ufo-rocks/lib/components/icon/plus2';

global.ns = Object.assign({}, global.ns, {
    events: {
        on: () => {},
        off: () => {}
    },
    action: {
        run: () => {}
    },
    Model: {
        get: () => ({
            save: () => {}
        })
    }
});

jest.mock('config', () => ({}));

export default storiesOf('Create resource popup with anchor ', module)
    .add('renders with office allowed and listing context', ({ kind, story }) => {
        const getComponent = ({ onFolderCreate, onEditorOpen, onCreateAlbum } = {}) => (
            <CreateButton
                isOfficeAllowed
                context="listing"
                popupCls="create-popup"
                onFolderCreate={onFolderCreate}
                onEditorOpen={onEditorOpen}
                onCreateAlbum={onCreateAlbum}
            />
        );

        const component = getComponent({
            onFolderCreate: mockFunction(),
            onEditorOpen: mockFunction(),
            onCreateAlbum: mockFunction()
        });

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it(story, () => {
                const tree = render(component);
                expect(tree.queryByText(i18n('%ufo_slice_toolbar__create'))).toBeInTheDOM();
                tree.unmount();
            });

            it('calls onFolderCreate', (done) => {
                const onFolderCreate = mockFunction();
                const tree = render(getComponent({ onFolderCreate }));
                Simulate.click(
                    tree.getByText(i18n('%ufo_slice_toolbar__create'))
                );

                const getCreateDir = () => document.querySelector('.create-resource-button .file-icon_dir_plus');
                wait(getCreateDir).then(() => {
                    fireEvent(
                        getCreateDir(),
                        new MouseEvent('click', { bubbles: true, cancelable: true })
                    );
                    expect(onFolderCreate).toHaveBeenCalledTimes(1);
                    tree.unmount();
                    done();
                });
            });

            [{
                iconSelector: '.file-icon_doc',
                fileExt: 'docx'
            }, {
                iconSelector: '.file-icon_xls',
                fileExt: 'xlsx'
            }, {
                iconSelector: '.file-icon_ppt',
                fileExt: 'pptx'
            }].forEach(({ iconSelector, fileExt }) => {
                it(`calls onEditorOpen ${fileExt}`, (done) => {
                    const onEditorOpen = mockFunction();
                    const tree = render(getComponent({ onEditorOpen }));
                    Simulate.click(
                        tree.getByText(i18n('%ufo_slice_toolbar__create'))
                    );

                    const getCreateOfficeFile = () => document.querySelector(`.create-resource-button ${iconSelector}`);
                    wait(getCreateOfficeFile).then(() => {
                        fireEvent(
                            getCreateOfficeFile(),
                            new MouseEvent('click', { bubbles: true, cancelable: true })
                        );
                        expect(onEditorOpen).toHaveBeenCalledWith(fileExt);
                        tree.unmount();
                        done();
                    });
                });
            });

            it('calls album', (done) => {
                const onEditorOpen = mockFunction();
                const onCreateAlbum = mockFunction();
                const tree = render(getComponent({ onEditorOpen, onCreateAlbum }));
                Simulate.click(
                    tree.getByText(i18n('%ufo_slice_toolbar__create'))
                );

                const getAlbum = () => document.querySelector('.create-resource-button .file-icon_album');
                wait(getAlbum).then(() => {
                    fireEvent(
                        getAlbum(),
                        new MouseEvent('click', { bubbles: true, cancelable: true })
                    );
                    expect(onEditorOpen).toHaveBeenCalledTimes(0);
                    expect(onCreateAlbum).toHaveBeenCalledTimes(1);
                    tree.unmount();
                    done();
                });
            });
        }));

        return component;
    }).add('renders without office and feed context', ({ kind, story }) => {
        const getComponent = ({ onFolderCreate, onEditorOpen } = {}) => (
            <CreateButton
                context="lenta"
                popupCls="create-popup"
                onFolderCreate={onFolderCreate}
                onEditorOpen={onEditorOpen}
            />
        );

        const component = getComponent({
            onFolderCreate: mockFunction(),
            onEditorOpen: mockFunction()
        });

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it(story, () => {
                const tree = render(component);
                expect(tree.queryByText(i18n('%ufo_slice_toolbar__create'))).toBeInTheDOM();
            });

            it('opens and closes', () => {
                const onFolderCreate = mockFunction();
                const tree = render(getComponent({ onFolderCreate }));
                Simulate.click(
                    tree.getByText(i18n('%ufo_slice_toolbar__create'))
                );
                expect(serialize(tree)).toMatchSnapshot();
                Simulate.click(
                    tree.getByText(i18n('%ufo_slice_toolbar__create'))
                );
                expect(serialize(tree)).toMatchSnapshot();
                tree.unmount();
            });
        }));
        return component;
    });
