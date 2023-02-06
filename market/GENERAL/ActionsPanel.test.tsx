import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { ActionsPanel, tabs } from './ActionsPanel';

test('correct render ActionsPanel', () => {
  const { app } = setupWithReatom(<ActionsPanel />);
  app.getByText(tabs[0].content);
  app.getByText(tabs[1].content);
});
