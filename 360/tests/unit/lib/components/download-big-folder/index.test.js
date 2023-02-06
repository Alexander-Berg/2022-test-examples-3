import React from 'react';
import { mount, render } from 'enzyme';

import { popFnCalls } from '../../../../../tests/unit/helpers';

import { Button } from '../../../../../lib/components/lego-components/Button';
import DownloadBigFolderDialog from '../../../../../lib/components/dialogs/download-big-folder';

jest.mock('../../../../../lib/helpers/go-to-custom-url-scheme', () => jest.fn());
import goToCustomUrlScheme from '../../../../../lib/helpers/go-to-custom-url-scheme';

const commonDialogProps = {
    title: 'title',
    titleTryDownload: 'titleTryDownload',
    buttonText: 'buttonText',
    buttonTextWindows: 'buttonTextWindows',
    description: 'description',
    descriptionTryDownload: 'descriptionTryDownload',
    descriptionOsHasApp: 'descriptionOsHasApp',
    descriptionNoApp: 'descriptionNoApp',
    descriptionNoInfoApp: 'descriptionNoInfoApp',
    linkAboutAppText: 'linkAboutAppText',
    linkAboutApp: 'linkAboutApp',
    folderId: '/disk/folder10gb'
};

const runTest = (props) => {
    const propsDialog = Object.assign({}, commonDialogProps, props);

    const component = render(
        <DownloadBigFolderDialog
            {...propsDialog}
        />
    );
    expect(component).toMatchSnapshot();
};

describe('Диалог для скачивания большой папки =>', () => {
    it('Windows+Chrome', () => {
        runTest({
            osFamily: 'Windows',
            browserName: 'Chrome'
        });
    });
    it('Windows+Firefox', () => {
        runTest({
            osFamily: 'Windows',
            browserName: 'Firefox'
        });
    });
    it('Windows+Opera', () => {
        runTest({
            osFamily: 'Windows',
            browserName: 'Opera'
        });
    });
    it('MacOS+Chrome', () => {
        runTest({
            osFamily: 'MacOS',
            browserName: 'Chrome'
        });
    });
    it('Linux+Chrome', () => {
        runTest({
            osFamily: 'Linux',
            browserName: 'Chrome'
        });
    });

    it('Windows+Chrome - схема запускается', async() => {
        const countMetrika = jest.fn();
        const getDownloadUrlScheme = jest.fn(() => Promise.resolve('urlScheme'));

        goToCustomUrlScheme.mockResolvedValue({ info: true, hasApp: true });

        const propsDialog = Object.assign({}, commonDialogProps, {
            osFamily: 'Windows',
            browserName: 'Chrome',

            getDownloadUrlScheme,
            countMetrika
        });

        const component = mount(
            <DownloadBigFolderDialog
                {...propsDialog}
            />
        );

        // метод для получения урла для скачивания ПО вызывается 1 раз
        expect(getDownloadUrlScheme).toHaveBeenCalledTimes(1);

        // подождем 2,5 секунды (схема в диалоге запускается через 2 секунды)
        await new Promise((resolve) => {
            setTimeout(() => resolve(), 2500);
        });

        // после этого должна посчитаеться метрика про запуск схемы
        const countMetrikaCalls = popFnCalls(countMetrika);
        expect(countMetrikaCalls.length).toEqual(3);
        expect(countMetrikaCalls[0]).toEqual(['open popup', 'with loader']);
        expect(countMetrikaCalls[1]).toEqual(['try open url scheme']);
        expect(countMetrikaCalls[2]).toEqual(['successed open url scheme']);

        expect(component.render()).toMatchSnapshot();

        component.unmount();
    });

    it('Windows+Chrome - схема не запускается', async() => {
        const countMetrika = jest.fn();
        const getDownloadUrlScheme = jest.fn(() => Promise.resolve('urlScheme'));

        goToCustomUrlScheme.mockResolvedValue({ info: true, hasApp: false });

        const propsDialog = Object.assign({}, commonDialogProps, {
            osFamily: 'Windows',
            browserName: 'Chrome',

            getDownloadUrlScheme,
            countMetrika
        });

        const component = mount(
            <DownloadBigFolderDialog
                {...propsDialog}
            />
        );

        // метод для получения урла для скачивания ПО вызывается 1 раз
        expect(getDownloadUrlScheme).toHaveBeenCalledTimes(1);

        // подождем 2,5 секунды (схема в диалоге запускается через 2 секунды)
        await new Promise((resolve) => {
            setTimeout(() => resolve(), 2500);
        });

        // после этого должна посчитаеться метрика про запуск схемы
        const countMetrikaCalls = popFnCalls(countMetrika);
        expect(countMetrikaCalls.length).toEqual(3);
        expect(countMetrikaCalls[0]).toEqual(['open popup', 'with loader']);
        expect(countMetrikaCalls[1]).toEqual(['try open url scheme']);
        expect(countMetrikaCalls[2]).toEqual(['failed open url scheme']);

        expect(component.render()).toMatchSnapshot();

        component.unmount();
    });

    it('MacOS+Chrome - схема не запускается, клик по желтой кнопке - скачивает ПО', () => {
        const onClose = jest.fn();
        const countMetrika = jest.fn();
        const getDownloadUrlScheme = jest.fn();
        const getLinkApp = jest.fn(() => Promise.resolve('linkApp'));

        const propsDialog = Object.assign({}, commonDialogProps, {
            osFamily: 'MacOS',
            browserName: 'Chrome',

            getDownloadUrlScheme,
            getLinkApp,
            countMetrika,
            onClose
        });

        const component = mount(
            <DownloadBigFolderDialog
                {...propsDialog}
            />
        );
        // метод для получения схемы не вызывается
        expect(getDownloadUrlScheme).toHaveBeenCalledTimes(0);

        // клик по желтой кнопке - вызывает метод для получения урла для скачивания ПО
        component
            .find(Button)
            .findWhere((button) => /download-big-folder-dialog__button/.test(button.props().className))
            .first()
            .simulate('click');

        expect(getLinkApp).toHaveBeenCalledTimes(1);

        // и считается метрика
        const countMetrikaCalls = popFnCalls(countMetrika);
        expect(countMetrikaCalls.length).toEqual(2);
        expect(countMetrikaCalls[0]).toEqual(['open popup', 'download through app']);
        expect(countMetrikaCalls[1]).toEqual(['click download app', 'MacOS']);

        component.unmount();
    });
});
