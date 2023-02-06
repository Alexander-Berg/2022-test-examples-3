import { Role } from 'src/java/definitions';
import { shops } from './shopModel';

export const userInfo = {
  avatar: '',
  login: 'user',
  name: 'usename',
  role: Role.MANAGER,
  shops: shops.map(el => el.id),
  userId: 1,
};
