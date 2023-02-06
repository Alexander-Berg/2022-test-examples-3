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

const WrappedLogin: FC<LoginPropsType & { loginClassName?: string } > = ({ className, loginClassName, ...props }) => (
  <div className={className ? `Login-Wrapper ${className}` : 'Login-Wrapper' }>
    <LoginDesktop {...props} className={loginClassName} />
  </div>
);

export const Serp = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <WrappedLogin theme="serp" loginClassName="Login_serp_1" />
    <WrappedLogin theme="serp" hasPicture className="Login_serp_2" />
  </div>
);
