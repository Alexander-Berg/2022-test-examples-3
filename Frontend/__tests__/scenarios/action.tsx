import React, { FC } from 'react';
import { Login as LoginDesktop, LoginPropsType } from '../..';

const styles = `
.Wrapper {
  padding: 20px;
}

.Login-Wrapper {
  margin-bottom: 10px;
}

.dark {
  background-color: black;
}
`;

const WrappedLogin: FC<LoginPropsType & { loginClassName?: string }> = ({ className, loginClassName, ...props }) => (
  <div className={className ? `Login-Wrapper ${className}` : 'Login-Wrapper' }>
    <LoginDesktop {...props} className={loginClassName} />
  </div>
);

export const Action = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <WrappedLogin theme="action" loginClassName="Login_action_1" />
    <WrappedLogin theme="action" hasPicture loginClassName="Login_action_2" />
    <WrappedLogin theme="action" white loginClassName="Login_action_3" className="dark" />
    <WrappedLogin theme="action" white hasPicture loginClassName="Login_action_4" className="dark" />
  </div>
);
