import * as React from 'react';
import { mount } from 'enzyme';

import { LcAlign, TextStyleType } from '@yandex-turbo/components/lcTypes/lcTypes';
import { IProps, LcRichTextBase, RichType } from '../LcRichText';

describe('Компонент LcRichText', () => {
    let liveEditSpy: jest.Mock<JSX.Element>;

    beforeEach(() => {
        liveEditSpy = jest.fn(() => (
            <div id={'live-edit'}>Редактор</div>
        ));

        window._LC_ = {
            getComponent: jest.fn().mockResolvedValue({
                LcRichtextLiveEdit: liveEditSpy,
            }),
            liveEditable: false,
        };
    });

    it('должен отрендериться richtext, если нет настроек для live редактора', done => {
        const props: IProps = {
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(window._LC_!.getComponent).not.toBeCalled();
            expect(liveEditSpy).not.toBeCalled();

            expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>Текст</span></div>');

            done();
        });
    });

    it('должен отрендериться richtext, если нет переменной window._LC_', done => {
        delete window._LC_;

        const props: IProps = {
            liveEdit: {
                id: 'block-id',
                controls: [TextStyleType.BOLD],
                draftContent: 'Текст редактора',
            },
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        lcRichText.setProps({ liveEditable: true });

        setImmediate(() => {
            lcRichText.update();

            expect(liveEditSpy).not.toBeCalled();

            expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>Текст</span></div>');

            done();
        });
    });

    it('должен отрендериться richtext, если нет переменной window._LC_.getComponent', done => {
        delete window._LC_!.getComponent;

        const props: IProps = {
            liveEdit: {
                id: 'block-id',
                controls: [TextStyleType.BOLD],
                draftContent: 'Текст редактора',
            },
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        lcRichText.setProps({ liveEditable: true });

        setImmediate(() => {
            lcRichText.update();

            expect(liveEditSpy).not.toBeCalled();

            expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>Текст</span></div>');

            done();
        });
    });

    it('должен отрендериться richtext, если window._LC_.getComponent вернула "undefined"', done => {
        window._LC_!.getComponent!.mockResolvedValue(undefined);

        const props: IProps = {
            liveEdit: {
                id: 'block-id',
                controls: [TextStyleType.BOLD],
                draftContent: 'Текст редактора',
            },
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(liveEditSpy).not.toBeCalled();

            expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>Текст</span></div>');

            done();
        });
    });

    it('должен отрендериться richtext, если window._LC_.getComponent не вернула LcRichtextLiveEdit', done => {
        window._LC_!.getComponent!.mockResolvedValue({});

        const props: IProps = {
            liveEdit: {
                id: 'block-id',
                controls: [TextStyleType.BOLD],
                draftContent: 'Текст редактора',
            },
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(liveEditSpy).not.toBeCalled();

            expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>Текст</span></div>');

            done();
        });
    });

    it('должен отрендериться live-редактор текста, если window._LC_.liveEditable = true', done => {
        window._LC_!.liveEditable = true;

        const props: IProps = {
            liveEdit: {
                id: 'block-id',
                controls: [TextStyleType.BOLD],
                draftContent: 'Текст редактора',
            },
            content: 'Текст',
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(lcRichText.find('#live-edit').length).toBe(1);

            expect(window._LC_!.getComponent).toHaveBeenCalledTimes(1);
            expect(window._LC_!.getComponent).toBeCalledWith('LcRichtextLiveEdit');

            expect(liveEditSpy).toHaveBeenCalledTimes(1);
            expect(liveEditSpy).toBeCalledWith(
                {
                    anchors: [],
                    className: 'lc-rich-text lc-rich-text_align_center',
                    controls: ['bold'],
                    liveEditId: 'block-id',
                    text: 'Текст редактора',
                },
                {}
            );

            done();
        });
    });

    it('должен отрендериться richtext с ссылкой с плавным проскроллом, если это isLpcMode', done => {
        const props: IProps = {
            content: [
                {
                    block: RichType.link,
                    attrs: { href: '#anchor', target: '_self' },
                    content: 'anchor',
                },
            ],
            align: LcAlign.CENTER,
            isLpcMode: true,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(lcRichText.find('a').prop('data-smooth-scroll')).toEqual('true');

            done();
        });
    });

    it('должен отрендериться richtext с ссылкой с плавным проскроллом, если withSmoothScroll = "true"', done => {
        const props: IProps = {
            content: [
                {
                    block: RichType.link,
                    attrs: { href: '#anchor', target: '_self', 'data-smoothScroll': 'true' },
                    content: 'anchor',
                },
            ],
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        setImmediate(() => {
            lcRichText.update();

            expect(lcRichText.find('a').prop('data-smooth-scroll')).toEqual('true');

            done();
        });
    });

    it('должны вычищаться теги meta', () => {
        const props: IProps = {
            content: [
                '<meta charset="utf-8">',
                'Some content',
            ],
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span></span><span>Some content</span></div>');
    });

    it('должны экранироваться xss', () => {
        const props: IProps = {
            content: [
                '<script',
                {
                    block: RichType.b,
                    content: '>alert(1)<',
                },
                '/script>',
            ],
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);

        expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>&lt;script</span><b><span>&gt;alert(1)&lt;</span></b><span>/script&gt;</span></div>');
    });

    it('Не должны экранироваться включения типа &nbsp;', () => {
        const props: IProps = {
            content: [
                '&nbsp;',
                {
                    block: RichType.b,
                    content: 'some text',
                },
                '&mdash;',
            ],
            align: LcAlign.CENTER,
        };
        const lcRichText = mount(<LcRichTextBase {...props} />);
        expect(lcRichText.html()).toBe('<div class="lc-rich-text lc-rich-text_align_center"><span>&nbsp;</span><b><span>some text</span></b><span>—</span></div>');
    });
});
