import { storiesOf, specs, describe, mockFunction, it, snapshot, mount } from '../.storybook/facade';

import React from 'react';
import { CreateFolderForm } from '../../../components/redux/components/dialogs/select-folder/create-folder-form';

jest.mock('../../../components/helpers/resource', () => ({
    validateName: () => {}
}));
jest.mock('../../../components/redux/store/lib/raw-fetch-model', () => jest.fn(
    () => Promise.resolve()
));
jest.mock('../../../components/redux/store/actions/dialogs', () => ({
    updateDialog: () => {}
}));

export default storiesOf('CreateFolderForm', module)
    .add('default', ({ kind, story }) => {
        const createFolder = mockFunction();

        const component = <CreateFolderForm
            folderId="/disk"
            createFolder={createFolder}
        />;

        specs(() => describe(kind, () => {
            const originalNsModelGet = ns.Model.get;
            beforeAll(() => {
                ns.Model.get = (modelName) => {
                    switch (modelName) {
                        case 'stateSelectFolder':
                            return {
                                getData: () => ({
                                    treeLoaded: true
                                })
                            };
                        case 'stateTree':
                            return {
                                get: () => '/parent-folder-id'
                            };
                    }
                };
            });
            afterAll(() => {
                ns.Model.get = originalNsModelGet;
            });

            snapshot(story, component);

            it('calls onSubmit callback', (done) => {
                expect.assertions(1);
                const wrapper = mount(component);
                wrapper.find('.create-folder-form__input-wrapper input[name="newFolderName"]').simulate(
                    'change', { target: { value: 'My new folder' } }
                );

                wrapper.find('.create-folder-form__input-wrapper input[name="newFolderName"]').simulate('submit');
                Promise.resolve().then(() => {
                    expect(createFolder).toHaveBeenCalledWith('/parent-folder-id', 'My new folder');
                    done();
                });
            });
        }));

        return component;
    });
