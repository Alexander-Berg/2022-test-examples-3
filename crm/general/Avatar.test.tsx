import { render } from '@testing-library/react/pure';
import React from 'react';
import Avatar, { AvatarUser } from './Avatar';
import { avatarService } from './AvatarService';
import { ExternalPassportUser } from '../../types/ExternalPassportUser';

describe('Avatar', () => {
  describe('AvatarService', () => {
    describe('when unavailable', () => {
      beforeEach(() => {
        avatarService.setAvailable(false);
      });
      it('renders first and last name first letters', () => {
        avatarService.setAvailable(false);
        const user: AvatarUser = {
          id: 1,
          login: 'crm',
          name: 'CRM',
          first_name: 'Ivan',
          last_name: 'Petrov',
        };
        const { container } = render(<Avatar user={user} />);
        expect(container).toMatchSnapshot();
      });

      it('renders first name first letter', () => {
        const userWithoutLastName: AvatarUser = {
          id: 2,
          login: 'crm',
          name: '',
          first_name: 'Ivan',
        };
        const { container } = render(<Avatar user={userWithoutLastName} />);
        expect(container).toMatchSnapshot();
      });

      it('renders last name first letter', () => {
        const userWithoutFirstName: AvatarUser = {
          id: 3,
          login: 'crm',
          name: '',
          last_name: 'Petrov',
        };
        const { container } = render(<Avatar user={userWithoutFirstName} />);
        expect(container).toMatchSnapshot();
      });

      it('renders letters from name', () => {
        const userWithName: AvatarUser = {
          id: 4,
          login: 'with-name',
          name: 'With Name',
        };
        const { container } = render(<Avatar user={userWithName} />);
        expect(container).toMatchSnapshot();
      });

      it('renders letters from login', () => {
        const { container } = render(
          <Avatar login="userlogin" user={(null as never) as AvatarUser} />,
        );
        expect(container).toMatchSnapshot();
      });
    });

    describe('when available', () => {
      beforeEach(() => {
        avatarService.setAvailable(true);
      });

      it('renders avatar', () => {
        const user: AvatarUser = {
          id: 1,
          login: 'crm',
          name: 'CRM',
          first_name: 'Ivan',
          last_name: 'Petrov',
        };
        const { container } = render(<Avatar user={user} />);
        expect(container).toMatchSnapshot();
      });
    });
  });

  describe('ExternalUserAvatarService', () => {
    it('renders avatar', () => {
      const externalUser: ExternalPassportUser = {
        etype: 'ExternalPassportUser',
        login: 'external',
        avatarId: '4000217463',
      };
      const { container } = render(<Avatar user={externalUser} />);
      expect(container).toMatchSnapshot();
    });

    it('renders name letters', () => {
      const externalUserWithName: ExternalPassportUser = {
        etype: 'ExternalPassportUser',
        login: 'external',
        name: 'External User',
      };
      const { container } = render(<Avatar user={externalUserWithName} />);
      expect(container).toMatchSnapshot();
    });

    it('renders login letters', () => {
      const externalUserWithName: ExternalPassportUser = {
        etype: 'ExternalPassportUser',
        login: 'external',
      };
      const { container } = render(<Avatar user={externalUserWithName} />);
      expect(container).toMatchSnapshot();
    });
  });
});
