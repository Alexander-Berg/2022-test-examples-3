# -*- coding: utf-8 -*-

from test.common.sharding import CommonShardingMethods
from test.fixtures.users import user_1, user_3, user_4
from mpfs.core.user.dao.user import UserDAO
from mpfs.metastorage.mongo.util import manual_route


class UserDAOTestCase(CommonShardingMethods):

    def test_find_existed_user(self):
        self.create_user(self.uid, noemail=True, shard=self.mongodb_unit2)
        doc = UserDAO().find_one({'_id': self.uid})
        assert doc

    def test_find_all_users(self):
        other_users = (user_1, user_3, user_4)
        for user in other_users:
            self.create_user(user.uid, noemail=True, shard=self.mongodb_unit2)

        with manual_route(self.mongodb_unit2):
            all_user_infos = [UserDAO().find_one({'_id': user.uid}) for user in other_users]

        for user in other_users:
            assert user.uid in [user_info['_id'] for user_info in all_user_infos]
