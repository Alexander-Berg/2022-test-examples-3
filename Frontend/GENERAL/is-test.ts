import { canUseDOM } from './can-use-dom';
import { getParams } from './params';

export function getTest() {
  const { test } = canUseDOM ? getParams(window.location.search) : { test: 'false' };

  return test;
}

export function isTest() {
  return getTest() === 'true';
}
