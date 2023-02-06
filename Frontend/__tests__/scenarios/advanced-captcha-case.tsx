import React, { useEffect } from 'react';

import { useParams } from '@yandex-int/captcha/internal/lib/query-string';
import { Status, AdvancedCaptcha, useCaptchaState } from '@yandex-int/captcha/Captcha';

const styles = `
  * {
    animation: none !important;
  }

  [data-testid=container] {
    display: inline-block;
    background-color: #fff;
    padding-right: 200px;
    padding-bottom: 100px;
  }

  [data-width=wide] {
    width: 700px;
  }
`;

export const AdvancedCaptchaCase = () => {
  const { status, width } = useParams<{ status: Status; width: 'wide' }>();
  const { captchaProps } = useCaptchaState();

  useEffect(() => {
    captchaProps.refreshResources();
  }, []);

  return (
    <>
      <style>{styles}</style>
      <div data-testid="container" data-width={width}>
        <AdvancedCaptcha {...captchaProps} status={status} />
      </div>
    </>
  );
};
