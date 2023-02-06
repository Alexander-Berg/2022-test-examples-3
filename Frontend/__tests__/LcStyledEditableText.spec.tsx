import * as React from 'react';
import { mount } from 'enzyme';

import { ILcStyledEditableTextProps } from '../LcStyledEditableText';

describe('Компонент LcStyledEditableText', () => {
    let LcStyledEditableText: React.ReactType<ILcStyledEditableTextProps>;
    let liveEditSpy: jest.Mock<JSX.Element>;

    beforeEach(() => {
        liveEditSpy = jest.fn(() => (
            <div id={'styled-live-edit'}>Редактор</div>
        ));

        // @ts-ignore
        window._LC_ = {
            getComponent: jest.fn().mockResolvedValue({
                LcStyledTextLiveEdit: liveEditSpy,
            }),
            liveEditable: false,
        };

        ({ LcStyledEditableText } = require('../LcStyledEditableText'));
    });

    it('должен отрендерить live редактор, если window._LC_.liveEditable = true', done => {
        window._LC_.liveEditable = true;

        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: 'Текст компонента',
        };

        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();
            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });

    it('должен вернуть стилизованный markdown текст, если отключен режим live редактирования', () => {
        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: '<span class="red">текcт</span><br><style>.red {color: red;}</style>',
        };
        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        expect(editableText.html()).toMatchSnapshot();
    });

    it('должен вернуть стилизованный markdown текст, если не передан live edit id', done => {
        const props: ILcStyledEditableTextProps = {
            text: '> Текст компонента',
        };
        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });

    it('должен вернуть стилизованный markdown текст, если нет переменной "window._LC_"', done => {
        jest.resetModules();

        delete window._LC_;

        ({ LcStyledEditableText } = require('../LcStyledEditableText'));

        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: '* Текст компонента',
        };

        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });

    it('должен вернуть стилизованный markdown текст, если нет переменной "window._LC_.getComponent"', done => {
        jest.resetModules();

        delete window._LC_.getComponent;

        ({ LcStyledEditableText } = require('../LcStyledEditableText'));

        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: '_Текст компонента_',
        };

        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });

    it('должен вернуть стилизованный markdown текст, если "window._LC_.getComponent" вернет undefined', done => {
        jest.resetModules();

        // @ts-ignore
        window._LC_.getComponent.mockResolvedValue(undefined);

        ({ LcStyledEditableText } = require('../LcStyledEditableText'));

        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: '- Текст компонента',
        };

        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });

    it('должен вернуть стилизованный markdown текст, если "window._LC_.getComponent" не вернет компонент LcStyledTextLiveEdit', done => {
        jest.resetModules();

        // @ts-ignore
        window._LC_.getComponent.mockResolvedValue({});

        ({ LcStyledEditableText } = require('../LcStyledEditableText'));

        const props: ILcStyledEditableTextProps = {
            liveEditId: 'live-edit-id',
            text: '# Текст компонента',
        };

        const editableText = mount(
            <LcStyledEditableText {...props} />
        );

        setImmediate(() => {
            editableText.update();

            expect(editableText.html()).toMatchSnapshot();

            done();
        });
    });
});
