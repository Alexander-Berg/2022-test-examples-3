import React from 'react';

import pagePaths from 'Constants/page-paths';
import Button from '@/components/_common/Button2';
import PersistentNotification from '@/components/_common/MGPersistentNotification';
import SurroundedInfoIcon from '@/icons/surrounded-info.svg';

function TestModeNotification() {
  return (
    <PersistentNotification
      icon={<SurroundedInfoIcon width={24} height={24} />}
      actions={
        <Button theme="second" type="link" url={pagePaths.REGISTRATION}>
          Завершить регистрацию
        </Button>
      }
    >
      Все ваши платежи проходят в&nbsp;тестовом режиме. Завершите регистрацию,
      чтобы начать принимать платежи от&nbsp;клиентов.
    </PersistentNotification>
  );
}

export default TestModeNotification;
