import React from 'react';

import { useParams } from '@yandex-int/captcha/internal/lib/query-string';
import { Status, CheckboxCaptcha } from '@yandex-int/captcha/Captcha';
import { useCaptchaState } from '../../useCaptchaState';

const styles = `
  * {
    animation: none !important;
  }

  [data-testid=container] {
    display: inline-block;
    background-color: #fff;
    width: 400px;
  }
`;

export const CheckboxCaptchaCase = () => {
  const { status } = useParams<{ status: Status }>();
  const { captchaProps } = useCaptchaState();

  return (
    <>
      <style>{styles}</style>
      <div data-testid="container">
        <CheckboxCaptcha {...captchaProps} status={status} />
      </div>
    </>
  );
};
