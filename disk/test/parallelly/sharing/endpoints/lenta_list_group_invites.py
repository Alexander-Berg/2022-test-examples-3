# -*- coding: utf-8 -*-

from test.common.sharing import CommonSharingMethods

from mpfs.core.social.share.invite import GroupInvites


class LentaListGroupInvitesTestCase(CommonSharingMethods):
    """Тестовый класс для ручки `lenta_list_group_invites`."""
    api_method_name = 'lenta_list_group_invites'
    invite_status_key = 'status'
    ctime_key = 'ctime'

    def test_new_users_after_accepted(self):
        """Протестировать, что новые пользователи (не подтвердившие и не отклонившие) в независимости от времени
        создания инвайта идут после подтвердивших, даже если подтвердили раньше."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя, но не подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid  # тк limit по умолчанию 20, то не передаем его
            }
        )
        invites = response['data']
        assert len(invites) == 2
        # Несмотря на то, что приглашение было выслано после подтверждения, но сначала показываем подтвержденные
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.NEW

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_new_users_after_accepted_and_rejected(self):
        """Протестировать, что новые приглашенные пользователи в независимости от времени создания приглашения
        будут возвращены после подтвердивших и отказавшихся."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и ничего не делаем
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )

        # Инвайтим второго пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        hash_ = self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_3, hash=hash_)

        # Инвайтим третьего пользователя и принимаем приглашение
        self.json_ok('user_init', {'uid': self.uid_6})
        self.xiva_subscribe(self.uid_6)
        hash_ = self.invite_user(
            uid=self.uid_6,
            email=self.email_6,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_6, hash=hash_)

        # Инвайтим четвертого пользователя и ничего не делаем
        self.json_ok('user_init', {'uid': self.uid_7})
        self.xiva_subscribe(self.uid_7)
        self.invite_user(
            uid=self.uid_7,
            email=self.email_7,
            path=shared_folder_path
        )

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
            }
        )
        invites = response['data']
        assert len(invites) == 4
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.REJECTED
        assert invites[2][self.invite_status_key] == GroupInvites.Status.NEW
        assert invites[3][self.invite_status_key] == GroupInvites.Status.NEW

        assert invites[0][self.ctime_key] >= invites[1][self.ctime_key]
        assert invites[2][self.ctime_key] >= invites[3][self.ctime_key]

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_alternation_in_accepted_and_rejected(self):
        """Протестировать корректную последовательность (чередование) среди подтвердивших
        и отказавшихся пользователей."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        hash_ = self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_3, hash=hash_)

        # Инвайтим третьего пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_6})
        self.xiva_subscribe(self.uid_6)
        hash_ = self.invite_user(
            uid=self.uid_6,
            email=self.email_6,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_6, hash=hash_)

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid  # тк limit по умолчанию 20, то не передаем его
            }
        )
        invites = response['data']
        assert len(invites) == 3
        # несмотря на то, что приглашение было выслано после подтверждения, но сначала показываем подтвержденные
        # не забываем что первый пользователь в ответе = последний выполнивший действие у нас в коде
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.REJECTED
        assert invites[2][self.invite_status_key] == GroupInvites.Status.ACTIVATED

        assert invites[0][self.ctime_key] >= invites[1][self.ctime_key] >= invites[2][self.ctime_key]

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_user_with_multiple_rejects(self):
        """Протестировать поведение в случае, когда один из участников отклонял несколько приглашений."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим пользователя и отклоняем второй раз приглашение
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_1, hash=hash_)

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid  # тк limit по умолчанию 20, то не передаем его
            }
        )
        invites = response['data']
        # Второй раз не должен попасть
        assert len(invites) == 1

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_limit(self):
        """Протестировать корректную работу лимита."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        hash_ = self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_3, hash=hash_)

        # Инвайтим третьего пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_6})
        self.xiva_subscribe(self.uid_6)
        hash_ = self.invite_user(
            uid=self.uid_6,
            email=self.email_6,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_6, hash=hash_)

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 2
            }
        )
        invites = response['data']
        assert len(invites) == 2
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.REJECTED

        assert invites[0][self.ctime_key] >= invites[1][self.ctime_key]

        iteration_key = response['iteration_key']
        assert iteration_key

    def test_iteration_key_when_returned_all_results(self):
        """Протестировать корректное значение ключа `iteration_key` в ответе,
        когда были возвращены все пользователи."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_1, hash=hash_)

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 1
            }
        )
        invites = response['data']
        iteration_key = response['iteration_key']
        assert len(invites) == 1
        assert not iteration_key

    def test_request_with_iteration_key(self):
        """Протестировать правильную отдачу c переданным ключом `iteration_key`."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        hash_ = self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_3, hash=hash_)

        # Инвайтим третьего пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_6})
        self.xiva_subscribe(self.uid_6)
        hash_ = self.invite_user(
            uid=self.uid_6,
            email=self.email_6,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_6, hash=hash_)

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 2
            }
        )
        invites = response['data']
        iteration_key = response['iteration_key']
        assert len(invites) == 2
        assert iteration_key
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.REJECTED

        assert invites[0][self.ctime_key] >= invites[1][self.ctime_key]

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'iteration_key': iteration_key
            }
        )
        invites = response['data']
        assert len(invites) == 1
        assert invites[0][self.invite_status_key] == GroupInvites.Status.REJECTED

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_iteration_key_for_new_users(self):
        """Протестировать корректную работу (ветку), когда передан ключ `iteration_key` для новых пользователей."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя и отклоняем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        hash_ = self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )
        self.reject_invite(uid=self.uid_3, hash=hash_)

        # Инвайтим третьего пользователя и ничего не делаем
        self.json_ok('user_init', {'uid': self.uid_6})
        self.xiva_subscribe(self.uid_6)
        self.invite_user(
            uid=self.uid_6,
            email=self.email_6,
            path=shared_folder_path
        )

        # Инвайтим четвертого пользователя и ничего не делаем
        self.json_ok('user_init', {'uid': self.uid_7})
        self.xiva_subscribe(self.uid_7)
        self.invite_user(
            uid=self.uid_7,
            email=self.email_7,
            path=shared_folder_path
        )

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 3
            }
        )
        invites = response['data']
        iteration_key = response['iteration_key']
        assert len(invites) == 3
        assert iteration_key
        assert invites[0][self.invite_status_key] == GroupInvites.Status.REJECTED
        assert invites[1][self.invite_status_key] == GroupInvites.Status.ACTIVATED
        assert invites[2][self.invite_status_key] == GroupInvites.Status.NEW

        assert invites[0][self.ctime_key] >= invites[1][self.ctime_key]

        # В этом запросе ключ будет только для новых пользователей, тк при предыдущем запросе
        # первые 2 из 3 были определившимися, а последний новым, значит при последующих запросах будут
        # возвращаться только новые.
        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'iteration_key': iteration_key
            }
        )
        invites = response['data']
        assert len(invites) == 1
        assert invites[0][self.invite_status_key] == GroupInvites.Status.NEW

        iteration_key = response['iteration_key']
        assert not iteration_key

    def test_request_with_all_non_new_users_and_then_new(self):
        """Проверить случай когда мы получаем пользователей первым запросом и выбираем ровно всех решившихся.
        Следующий запрос будет получать верхушку с нерешившихся.
        """
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)

        # Инвайтим второго пользователя, но не подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        self.invite_user(
            uid=self.uid_3,
            email=self.email_3,
            path=shared_folder_path
        )

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 1
            }
        )
        invites = response['data']
        assert len(invites) == 1
        assert invites[0][self.invite_status_key] == GroupInvites.Status.ACTIVATED

        iteration_key = response['iteration_key']
        assert iteration_key

        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'iteration_key': iteration_key
            }
        )
        invites = response['data']
        assert len(invites) == 1
        assert invites[0][self.invite_status_key] == GroupInvites.Status.NEW

    def test_non_owner_request(self):
        """Проверить что не владелец получает 403."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)
        self.json_error(
            self.api_method_name,
            {
                'uid': self.uid_1,
                'gid': gid,
            },
            code=202
        )

    def test_limit_0(self):
        """Проверить корректность ответа при limit равном 0."""
        # Создаем группу
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        gid = self.create_group(uid=self.uid, path=shared_folder_path)

        # Инвайтим первого пользователя и подтверждаем приглашение
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)
        response = self.json_ok(
            self.api_method_name,
            {
                'uid': self.uid,
                'gid': gid,
                'limit': 0
            }
        )
        assert not response['data']
        assert response['iteration_key'] is None
