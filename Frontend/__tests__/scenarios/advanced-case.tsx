import React, { useEffect, useRef } from 'react';

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

export const AdvancedCase = () => {
  const failRef = useRef(false);

  useEffect(() => {
    setup({ kind: 'failed', delay: 0 });
  }, []);

  const onValidate = () => {
    if (failRef.current) {
      setup({ kind: 'ok', delay: 0 });
    } else {
      failRef.current = true;
    }
  };

  return (
    <>
      <style>{styles}</style>
      <div data-testid="container">
        <Captcha onValidate={onValidate} />
      </div>
    </>
  );
};
