import React, { FC } from 'react';
import { Login as LoginDesktop, LoginPropsType } from '../..';

const styles = `
.Wrapper {
  padding: 20px;
}

.Login-Wrapper {
  margin-bottom: 10px;
}
`;

const WrappedLogin: FC<LoginPropsType & { loginClassName?: string }> = ({ className, loginClassName, ...props }) => (
  <div className={className ? `Login-Wrapper ${className}` : 'Login-Wrapper' }>
    <LoginDesktop {...props} className={loginClassName} />
  </div>
);

export const Link = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <WrappedLogin theme="link" loginClassName="Login_link_1" />
    <WrappedLogin theme="link" hasPicture loginClassName="Login_link_2" />
  </div>
);
