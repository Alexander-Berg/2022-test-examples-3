# coding: utf-8


def login_user(admin_client, user, password=''):
    admin_client.login(username=user.username, password=password)
