import React from 'react';
import { cls } from 'news/components/App/App.cn';

import 'news/components/App/App.scss';
import 'news/components/App/App.desktop.scss';

export const AppDesktop = (chidlren: () => JSX.Element) => {
  return (
    <div className={cls()}>
      <div className={cls('content')} style={{ padding: '16px 0' }}>
        {chidlren()}
      </div>
    </div>
  );
};
