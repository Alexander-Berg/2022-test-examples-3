import React, { useEffect } from 'react';

import { Captcha } from '../../desktop';
import { setup } from '../../../internal/__mock__/request';

const styles = `
  * {
    animation: none !important;
  }

  [data-testid=container] {
    display: inline-block;
    width: 400px;
    height: 460px;
    background-color: #fff;
  }
`;

export const CheckboxCase = () => {
  useEffect(() => {
    setup({ kind: 'ok', delay: 0 });
  }, []);

  return (
    <>
      <style>{styles}</style>
      <div data-testid="container">
        <Captcha />
      </div>
    </>
  );
};
