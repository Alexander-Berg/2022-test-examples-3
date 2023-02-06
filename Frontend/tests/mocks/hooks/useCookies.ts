/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/explicit-function-return-type */
import { ICookies } from 'neo/hooks/useCookies';

jest.mock('neo/hooks/useCookies', () => {
  const cookie: Record<string, string> = {
    nc: 'useCookieContainerTest=someValue#useCookieCounterTest=12',
    otherContainerName: 'useCookieContainerTest=otherValue',
    otherCounterName: 'useCookieCounterTest=100500',
  };

  return {
    useCookies: (): ICookies => {
      return {
        get: (name: string) => {
          return cookie[name] ?? '';
        },
        set: (name: string, value: any) => {
          cookie[name] = value;
        },
        remove: (name: string) => {
          delete cookie[name];
        },
      };
    },
  };
});
