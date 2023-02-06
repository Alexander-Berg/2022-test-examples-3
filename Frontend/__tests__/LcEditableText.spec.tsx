import * as React from 'react';
import { mount } from 'enzyme';

import { IProps } from '../LcEditableText';

describe('Компонент LcEditableText', () => {
    let LcEditableText: React.ReactType<IProps>;
    let liveEditSpy: jest.Mock<JSX.Element>;

    beforeEach(() => {
        liveEditSpy = jest.fn(() => (
            <div id={'live-edit'}>Редактор</div>
        ));

        window._LC_ = {
            getComponent: jest.fn().mockResolvedValue({
                LcPlainTextLiveEdit: liveEditSpy,
            }),
            liveEditable: false,
        };

        ({ LcEditableText } = require('../LcEditableText'));
    });

    it('должен отрендерить live редактор, если window._LC_.liveEditable = true', done => {
        window._LC_!.liveEditable = true;

        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toEqual('<div id="live-edit">Редактор</div>');

            done();
        });
    });

    it('должен вернуть текст, если отключен режим live редактирования', () => {
        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };
        const editableText = mount(
            <LcEditableText {...props} />
        );

        expect(editableText.text()).toEqual('Текст компонента');
    });

    it('должен вернуть текст, если не передан live edit id', done => {
        const props: IProps = {
            text: 'Текст компонента',
        };
        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.text()).toEqual('Текст компонента');

            done();
        });
    });

    it('должен вернуть текст, если нет переменной "window._LC_"', done => {
        jest.resetModules();

        delete window._LC_;

        ({ LcEditableText } = require('../LcEditableText'));

        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.text()).toEqual('Текст компонента');

            done();
        });
    });

    it('должен вернуть текст, если нет переменной "window._LC_.getComponent"', done => {
        jest.resetModules();

        delete window._LC_!.getComponent;

        ({ LcEditableText } = require('../LcEditableText'));

        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.text()).toEqual('Текст компонента');

            done();
        });
    });

    it('должен вернуть текст, если "window._LC_.getComponent" вернет undefined', done => {
        jest.resetModules();

        window._LC_!.getComponent!.mockResolvedValue(undefined);

        ({ LcEditableText } = require('../LcEditableText'));

        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.text()).toEqual('Текст компонента');

            done();
        });
    });

    it('должен вернуть текст, если "window._LC_.getComponent" не вернет компонент LcPlainTextLiveEdit', done => {
        jest.resetModules();

        window._LC_!.getComponent!.mockResolvedValue({});

        ({ LcEditableText } = require('../LcEditableText'));

        const props: IProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.text()).toEqual('Текст компонента');

            done();
        });
    });
});
