import pytest

from mail.nwsmtp.tests.lib.users import Users, User, BigMLUser, Assessor, UserWithAppPasswordEnabled, HostedUser, CorpList


@pytest.fixture
def prod_sender() -> User:
    return User("first@yandex.ru", token='xoauth-token-for-first')


@pytest.fixture
def prod_rcpt() -> User:
    return User("second@yandex.ru")


@pytest.fixture
def prod_second_rcpt() -> User:
    return User("third@yandex.ru")


@pytest.fixture
def corp_sender() -> User:
    return User("first@yandex-team.ru")


@pytest.fixture
def corp_rcpt() -> User:
    return User("second@yandex-team.ru")


@pytest.fixture
def big_ml() -> BigMLUser:
    ml = BigMLUser("otdel@bigmltest.yaconnect.com", is_ml=True)
    for email in ("first@bigmltest.yaconnect.com", "second@bigmltest.yaconnect.com"):
        ml.add_subscriber(BigMLUser(email))
    return ml


@pytest.fixture
def empty_big_ml() -> BigMLUser:
    ml = BigMLUser("empty@bigmltest.yaconnect.com", is_ml=True)
    return ml


@pytest.fixture
def corp_ml() -> User:
    ml = User("corp_ml@yandex-team.ru", is_ml=True)
    for email in ("first_subscriber@yandex-team.ru",
                  "second_subscriber@yandex-team.ru"):
        ml.add_subscriber(User(email))
    return ml


@pytest.fixture
def corp_ml_not_registered_in_blackbox() -> CorpList:
    ml = User("corp_ml_not_in_bb@yandex-team.ru", is_ml=True, is_registered_in_blackbox=False)
    for email in ("corp_ml_not_in_bb_sub1@yandex-team.ru",
                  "corp_ml_not_in_bb_sub2@yandex-team.ru"):
        ml.add_subscriber(User(email))
    return ml


@pytest.fixture
def empty_corp_ml() -> CorpList:
    return User("empty_corp_ml@yandex-team.ru", is_ml=True)


@pytest.fixture
def assessor() -> User:
    return Assessor("partner1@yandex-team.ru")


@pytest.fixture
def user_with_app_password_enabled() -> User:
    return UserWithAppPasswordEnabled("app_password_enabled@yandex.ru")


@pytest.fixture
def user_with_blocked_email() -> User:
    return User("blocked_email@yandex.ru", is_email_blocked=True)


@pytest.fixture
def bad_karma_rcpt() -> User:
    return User("badkarmarcpt@yandex.ru", is_bad_karma=True)


@pytest.fixture
def temp_bad_karma_rcpt() -> User:
    return User("tempbadkarmarcpt@yandex.ru", is_bad_karma=True, is_temp_bad_karma=True)


@pytest.fixture
def unknown_rcpt() -> User:
    return User("unknownrcpt@yandex.ru")


@pytest.fixture
def passwordless_sender() -> User:
    return User("passwordless@yandex.ru", empty_password=True)


@pytest.fixture
def hosted_noeula_user() -> HostedUser:
    return HostedUser("noeulahosted@yandex.ru", is_no_eula=True)


@pytest.fixture
def rcpt_from_ratesrv_zero_domain() -> CorpList:
    return User("devnull@yandex-team.ru")


@pytest.fixture
def bad_karma_hosted_user() -> HostedUser:
    return HostedUser("badkarmahosted@testtrest.yaconnect.com", is_bad_karma=True)


@pytest.fixture
def bad_karma_phone_comfirmed_user() -> User:
    return User("badkarmaphoneconfirmeduser@yandex.ru", is_bad_karma=True, is_no_eula=True, is_phone_confirmed=True)


@pytest.fixture
def mdbreg_user() -> User:
    return User("mdbreg@yandex.ru", is_mdbreg=True)


@pytest.fixture
def zero_suid_user() -> User:
    return User("zerosuid@yandex.ru", is_zero_suid=True)


@pytest.fixture
def bad_karma_user() -> User:
    return User("badkarmauser@yandex.ru", is_bad_karma=True, is_no_eula=True, is_phone_confirmed=False)


@pytest.fixture
def threshold_karma_user() -> User:
    return User("thresholdkarma@yandex.ru", is_threshold_karma=True, is_no_eula=True, is_phone_confirmed=False)


@pytest.fixture
def corp_list_rcpt() -> CorpList:
    return CorpList("corplist@yandex-team.ru", is_empty_1000_suid=True)


@pytest.fixture
def nonexistent_sender() -> User:
    return User("nonexistent@yandex.ru")


@pytest.fixture
def virtual_alias_rcpt() -> User:
    return User("virtualalias@yandex.ru")


@pytest.fixture
def rcpt_with_exchange() -> CorpList:
    rcpt = User("exchange@yandex-team.ru", is_ml=True)
    rcpt.add_subscriber(User("exchange@ld.yandex.ru"))
    rcpt.add_subscriber(User("exchange@mail.yandex-team.ru"))
    return rcpt


@pytest.fixture
def users(prod_sender, prod_rcpt, prod_second_rcpt,
          corp_sender, corp_rcpt, corp_list_rcpt, big_ml, corp_ml, corp_ml_not_registered_in_blackbox,
          assessor, user_with_app_password_enabled, passwordless_sender, mdbreg_user, zero_suid_user,
          user_with_blocked_email, hosted_noeula_user, rcpt_from_ratesrv_zero_domain,
          bad_karma_user, bad_karma_rcpt, temp_bad_karma_rcpt, bad_karma_hosted_user, threshold_karma_user,
          virtual_alias_rcpt, empty_corp_ml, empty_big_ml, rcpt_with_exchange, bad_karma_phone_comfirmed_user) -> Users:
    users = Users()
    for user in (prod_sender, prod_rcpt, prod_second_rcpt,
                 corp_sender, corp_rcpt, corp_list_rcpt, big_ml, corp_ml, corp_ml_not_registered_in_blackbox,
                 assessor, user_with_app_password_enabled, passwordless_sender, mdbreg_user, zero_suid_user,
                 user_with_blocked_email, hosted_noeula_user, rcpt_from_ratesrv_zero_domain,
                 bad_karma_user, bad_karma_rcpt, temp_bad_karma_rcpt, bad_karma_hosted_user, threshold_karma_user,
                 virtual_alias_rcpt, empty_corp_ml, empty_big_ml, rcpt_with_exchange, bad_karma_phone_comfirmed_user):
        users.add(user)
        if user.is_ml:
            for subscriber in user.subscribers.values():
                users.add(subscriber)
    return users
