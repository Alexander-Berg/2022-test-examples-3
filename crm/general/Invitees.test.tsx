import React from 'react';
import { screen, render } from '@testing-library/react';
import { User } from 'types/entities/user';
import { Invitees } from './Invitees';

const inviteesStub: User[] = [
  {
    id: 0,
    login: 'user1',
    name: 'name1',
  },
  {
    id: 1,
    login: 'user2',
    name: 'name2',
  },
];

describe('TimelineComment/Invitees', () => {
  describe('props.invitees', () => {
    describe('when defined', () => {
      it('renders array of invitee', () => {
        render(<Invitees invitees={inviteesStub} />);

        inviteesStub.forEach((user) => expect(screen.getByText(user.name)).toBeInTheDocument());
      });
    });
  });
});
