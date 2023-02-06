import pytest


@pytest.fixture
def mds_namespace():
    return 'test-mds-namespace'


@pytest.fixture
def mds_expire():
    return 'test-mds-expire'


@pytest.fixture
def mds_client(client_mocker, mds_namespace, mds_expire):
    from mail.payments.payments.interactions.mds import MDSClient

    class ConcreteMDSClient(MDSClient):
        NAMESPACE = mds_namespace
        EXPIRE = mds_expire

    return client_mocker(ConcreteMDSClient)
