import React from 'react';
import {Provider} from 'react-redux';
import HTML5Backend from 'react-dnd-html5-backend';
import {DragDropContextProvider} from 'react-dnd';
import {render, wait, waitForElement} from 'react-testing-library';

import getStore from 'entries/index/getStore';

const customRender = (node, options) => {
  return render(
    <div>
      <DragDropContextProvider backend={HTML5Backend}>
        <Provider store={getStore(true)}>{node}</Provider>
      </DragDropContextProvider>
    </div>,
    options
  );
};

const customWait = (callback, options) => {
  return wait(callback, {
    timeout: 29500,
    ...options
  });
};

const customWaitForElement = (callback, options) => {
  return waitForElement(callback, {
    timeout: 29500,
    ...options
  });
};

const rtl = require('react-testing-library');

module.exports = {
  ...rtl,
  render: customRender,
  wait: customWait,
  waitForElement: customWaitForElement
};
