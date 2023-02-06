import { widgets, WidgetsManager } from '../widgets';
import '../../typings/global.d';

export const getWidget = () => {
  const props = {
    reset: jest.fn(),
    destroy: jest.fn(),
    showError: jest.fn(),
    callback: jest.fn(),
    onTokenReset: jest.fn(),
  };

  const widget = WidgetsManager.createWidget(props);

  return { widget, props };
};

export const getInvisibleWidget = () => {
  const props = {
    reset: jest.fn(),
    destroy: jest.fn(),
    showError: jest.fn(),
    callback: jest.fn(),
    onTokenReset: jest.fn(),
    execute: jest.fn(),
  };

  const widget = WidgetsManager.createWidget(props);

  return { widget, props };
};

export const initSmartCaptcha = () => {
  window.smartCaptcha = {
    reset: jest.fn(),
    destroy: jest.fn(),
    showError: jest.fn(),
    execute: jest.fn(),
    render: jest.fn(),
    subscribe: jest.fn(),
    getResponse: jest.fn(),
    _origin: '',
    _test: 'false',
  };
};

export const flush = () => {
  widgets.splice(0, widgets.length);
};
