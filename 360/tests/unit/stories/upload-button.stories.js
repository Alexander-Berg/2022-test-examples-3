import { storiesOf, specs, describe, it, snapshot, mockFunction, mount } from '../.storybook/facade';

import React from 'react';
import UploadButton from '@ps-int/ufo-rocks/lib/components/upload-button';
import '@ps-int/ufo-rocks/lib/components/upload-button/index.styl';

export default storiesOf('Upload Button ', module)
    .add('default', ({ kind, story }) => {
        const upload = mockFunction();

        const onUploadButtonChange = (files, reset) => global.Promise.resolve(upload(files)).then(reset, reset);

        const component = <UploadButton
            width="max"
            text={i18n('%ufo_upload')}
            title={i18n('%ufo_upload_files')}
            onChange={onUploadButtonChange}
        />;

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('calls upload', () => {
                const wrapper = mount(component);

                const file = new Blob(['file contents'], { type: 'text/plain' });
                wrapper.find('.upload-button__attach').simulate('change', { target: { files: [file] } });
                expect(upload).toHaveBeenCalledTimes(1);
            });

            it('doesn`t call upload, when no files were selected', () => {
                const wrapper = mount(component);
                wrapper.find('.upload-button__attach').simulate('change', { target: { files: [] } });
                expect(upload).toHaveBeenCalledTimes(1);
            });
        }));

        return component;
    }).add('disabled', ({ kind, story }) => {
        const upload = mockFunction();

        const onUploadButtonChange = (files, reset) => global.Promise.resolve(upload(files)).then(reset, reset);

        const component = <UploadButton
            width="max"
            text={i18n('%ufo_upload')}
            title={i18n('%ufo_upload_files')}
            disabled={true}
            onChange={onUploadButtonChange}
        />;

        specs(() => describe(kind, () => {
            snapshot(story, component);
        }));

        return component;
    });
