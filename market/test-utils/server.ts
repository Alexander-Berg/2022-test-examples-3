// eslint-disable-next-line import/no-extraneous-dependencies
import { rest } from 'msw';
// eslint-disable-next-line import/no-extraneous-dependencies
import { setupServer } from 'msw/node';

import { ViewerDto } from '@/dto';

type User = Omit<ViewerDto, 'permissions'> & {
  roles: ViewerDto['permissions'];
};

const handlers = [
  rest.get('/api/v1/getUser', (req, res, ctx) => {
    const user: User = {
      id: '8',
      login: 'withcher',
      fullName: 'Geralt of Rivia',
      avatarId: '0/0-0',
      lastLogin: new Date().toISOString(),
      roles: [],
    };

    return res(ctx.status(200), ctx.json(user));
  }),
];

export const server = setupServer(...handlers);
