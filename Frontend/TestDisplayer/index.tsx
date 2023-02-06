import React from 'react';
import { isTest } from '../../../utils/is-test';

import './TestDisplayer.css';

export function TestDisplayer() {
  if (isTest()) {
    return <div className="TestDisplayer">You are in test mode</div>;
  }

  return <></>;
}
