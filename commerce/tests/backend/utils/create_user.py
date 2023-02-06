from commerce.adv_backend.tests import factory


def create_test_user(login, role, group='common'):
    factory.UserRoleFactory.create(
        user=factory.UserFactory.create(username=login),
        group=group,
        role=role
    )
