import React, { useRef } from 'react';
import { screen, render } from '@testing-library/react';
import { RichHtmlEditor } from './RichHtmlEditor';

jest.useFakeTimers('modern');

jest.mock('entry/utils/getCrmConfig', () => () => null);
jest.mock('./ckeditor', () => {});

const editor = {
  mode: 'wysiwyg',
  on: jest.fn().mockImplementation((_, callback) => callback()),
  destroy: jest.fn(),
  getData: jest.fn().mockImplementation(() => 'data'),
  fire: jest.fn(),
  editable: () => ({
    attachListener: jest.fn(),
  }),
  setData: jest.fn(),
  focus: jest.fn(),
  createRange: jest.fn().mockImplementation(() => ({
    moveToElementEditStart: jest.fn(),
  })),
  getSelection: jest.fn().mockImplementation(() => ({
    selectRanges: jest.fn(),
  })),
  ui: {
    contentsElement: {
      $: {
        style: {},
      },
    },
  },
};
const ckeditor = {
  replace: jest.fn().mockImplementation(() => editor),
  env: {
    ie: false,
  },
};
global.CKEDITOR = ckeditor;

const resizeObserver = {
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
};
// eslint-disable-next-line
(window as any).ResizeObserver = jest.fn().mockImplementation(() => resizeObserver)

const EditorTestRef = ({ getRef }) => {
  const ckeditorRef = useRef(null);
  getRef(ckeditorRef.current);

  return <RichHtmlEditor ckeditorRef={ckeditorRef} />;
};

describe('RichHtmlEditor', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('props.value', () => {
    describe('when defined', () => {
      it('sets textarea value', () => {
        const value = 'value';
        render(<RichHtmlEditor value={value} />);

        expect(screen.queryByDisplayValue(value)).toBeInTheDocument();
      });
    });
  });

  describe('props.defaultValue', () => {
    describe('when defined', () => {
      it('sets textarea default value', () => {
        const defaultValue = 'defaultValue';
        render(<RichHtmlEditor defaultValue={defaultValue} />);

        expect(screen.queryByDisplayValue(defaultValue)).toBeInTheDocument();
      });
    });
  });

  describe('props.startupMode', () => {
    describe('when defined', () => {
      it('adds startupMode to config', () => {
        render(<RichHtmlEditor startupMode="startupMode" />);

        expect(ckeditor.replace.mock.calls[0][1]).toStrictEqual({ startupMode: 'startupMode' });
      });
    });
  });

  describe('props.resizable', () => {
    describe('when true', () => {
      it('adds resizable and props.defaultHeight to config', () => {
        const defaultHeight = 500;
        render(<RichHtmlEditor resizable defaultHeight={defaultHeight} />);

        expect(ckeditor.replace.mock.calls[0][1]).toStrictEqual({
          resize_enabled: true,
          resize_dir: 'vertical',
          height: defaultHeight,
        });
      });

      it('disables resizeObserver', () => {
        const { container } = render(<RichHtmlEditor resizable />);

        expect(resizeObserver.observe).not.toBeCalled();
        expect(container.querySelector('.b_resizable')).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it('enables resizeObserver', () => {
        const { container } = render(<RichHtmlEditor resizable={false} />);

        expect(resizeObserver.observe).toBeCalled();
        expect(container.querySelector('.b_resizable')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.autogrow', () => {
    describe('when defined', () => {
      it('adds autogrow to config', () => {
        render(<RichHtmlEditor autogrow />);

        expect(ckeditor.replace.mock.calls[0][1]).toStrictEqual({
          autoGrow_enable: true,
          removePlugins: 'resize,resizewithwindow',
        });
      });

      it('sets min and max height to config', () => {
        render(<RichHtmlEditor autogrow />);

        expect(editor.ui.contentsElement.$.style).toStrictEqual({
          maxHeight: '300px',
          minHeight: '80px',
        });
      });
    });
  });

  describe('props.name', () => {
    describe('when defined', () => {
      it('fires CKEDITOR.replace with name', () => {
        const name = 'name';
        render(<RichHtmlEditor name={name} />);

        expect(ckeditor.replace.mock.calls[0][0]).toBe(name);
      });
    });

    describe('when undefined', () => {
      it('fires CKEDITOR.replace with default name', () => {
        render(<RichHtmlEditor />);

        expect(ckeditor.replace.mock.calls[0][0]).toBe('editor');
      });
    });
  });

  describe('props.editorConfig', () => {
    describe('when defined', () => {
      it('fires CKEDITOR.replace with editorConfig', () => {
        const editorConfig = { field: 'value' };
        render(<RichHtmlEditor editorConfig={editorConfig} />);

        expect(ckeditor.replace.mock.calls[0][1]).toStrictEqual(editorConfig);
      });
    });
  });

  describe('props.focus', () => {
    describe('when defined', () => {
      it('fires CKEDITOR.focus', () => {
        render(<RichHtmlEditor focus />);

        expect(editor.focus).toBeCalled();
      });
    });

    describe('when undefined', () => {
      it("doesn't call CKEDITOR.focus", () => {
        render(<RichHtmlEditor />);

        expect(editor.focus).not.toBeCalled();
      });
    });
  });

  describe('props.onBlur', () => {
    describe('when defined', () => {
      it('fires with editor data', () => {
        const onBlur = jest.fn();
        render(<RichHtmlEditor onBlur={onBlur} />);

        expect(editor.on.mock.calls.find((call) => call[0] === 'blur')).toBeTruthy();
        expect(onBlur).toBeCalledWith(editor.getData.mock.results[0].value);
      });
    });
  });

  describe('props.onChange', () => {
    describe('when defined', () => {
      it('fires with editor data', async () => {
        const onChange = jest.fn();
        render(<RichHtmlEditor onChange={onChange} />);
        jest.advanceTimersByTime(300);

        expect(editor.on.mock.calls.find((call) => call[0] === 'change')).toBeTruthy();
        expect(onChange).toBeCalledWith(editor.getData.mock.results[0].value);
      });
    });
  });

  describe('props.onFocus', () => {
    describe('when defined', () => {
      it('fires when focused', async () => {
        const onFocus = jest.fn();
        render(<RichHtmlEditor onFocus={onFocus} />);
        jest.advanceTimersByTime(300);

        expect(editor.on.mock.calls.find((call) => call[0] === 'focus')).toBeTruthy();
        expect(onFocus).toBeCalled();
      });
    });
  });

  describe('props.onInstanceReady', () => {
    describe('when defined', () => {
      it('fires on mount', () => {
        const onInstanceReady = jest.fn();
        render(<RichHtmlEditor onInstanceReady={onInstanceReady} />);

        expect(onInstanceReady).toBeCalledWith(editor);
      });
    });
  });

  describe('props.resizeHash', () => {
    describe('when changes', () => {
      it('fires resize', () => {
        const { rerender } = render(<RichHtmlEditor resizeHash={'1'} />);

        expect(editor.fire).not.toBeCalledWith('triggerResize');

        rerender(<RichHtmlEditor resizeHash={'2'} />);

        expect(editor.fire).toBeCalledWith('triggerResize');
      });
    });
  });

  describe('props.textAreaPadding', () => {
    describe('when true', () => {
      it('adds paddings', () => {
        const { container } = render(<RichHtmlEditor textAreaPadding />);

        expect(container.querySelector('.b_padding')).toBeInTheDocument();
      });
    });

    describe('when false', () => {
      it('removes paddings', () => {
        const { container } = render(<RichHtmlEditor textAreaPadding={false} />);

        expect(container.querySelector('.b_padding')).not.toBeInTheDocument();
      });
    });
  });

  describe('props.height', () => {
    describe("when equals 'fix'", () => {
      it('fixes height', () => {
        const { container } = render(<RichHtmlEditor height="fix" />);

        expect(container.querySelector('.b_height_fix')).toBeInTheDocument();
      });
    });

    describe("when equals 'auto'", () => {
      it('sets auto height', () => {
        const { container } = render(<RichHtmlEditor height="auto" />);

        expect(container.querySelector('.b_height_auto')).toBeInTheDocument();
      });
    });
  });

  describe('props.theme', () => {
    describe("when equals 'inline'", () => {
      it('adds inline className', () => {
        const { container } = render(<RichHtmlEditor theme="inline" />);

        expect(container.querySelector('.b_theme_inline')).toBeInTheDocument();
      });
    });
  });

  describe('props.ckeditorRef', () => {
    describe('when defined', () => {
      it('receives ckeditor', () => {
        const getRef = jest.fn();
        const { rerender } = render(<EditorTestRef getRef={getRef} />);
        rerender(<EditorTestRef getRef={getRef} />);

        expect(getRef).toBeCalledWith(editor);
      });
    });
  });
});
