from __future__ import unicode_literals, absolute_import

import logging
import os
import time

import pytest
from market.sre.tools.rtc.nanny.manager import ServiceRepoManager  # noqa
from market.sre.tools.rtc.nanny.models.service import Service  # noqa

# Run functional tests only with robot's token
pytestmark = pytest.mark.skipif(
    condition=not (
        os.getenv("NANNY_TOKEN_FOR_TESTS") and os.getenv("SANDBOX_TOKEN_FOR_TESTS")
    ),
    reason="Integration tests require NANNY_TOKEN_FOR_TESTS and SANDBOX_TOKEN_FOR_TESTS",
)


def test_list_service_ids(
    real_login, real_manager
):  # type: (unicode, ServiceRepoManager) -> None
    service_ids = real_manager.list_service_ids(real_login)
    logging.info("Number of service ids %d", len(service_ids))
    logging.info("Service ids: %s", service_ids)
    assert service_ids


def test_get_service(
    real_nanny_service_id, real_manager
):  # type: (unicode, ServiceRepoManager) -> None
    service = real_manager.get_service(real_nanny_service_id)
    assert service.runtime_attrs
    assert service.runtime_attrs.content
    assert service.runtime_attrs.snapshot_id
    assert service.info_attrs
    assert service.info_attrs.content
    assert service.info_attrs.snapshot_id
    assert service.auth_attrs
    assert service.auth_attrs.content
    assert service.auth_attrs.snapshot_id
    logging.info("Service id %s", service.id)
    assert service.id


def test_create_service(
    real_nanny_service_id,  # type: unicode
    real_new_nanny_service_id,  # type: unicode
    real_new_category,  # type: unicode
    real_manager,  # type: ServiceRepoManager
):  # type: (...) -> None
    template_service = real_manager.get_service(real_nanny_service_id)
    template_service.id = "{}_for_service".format(real_new_nanny_service_id)
    template_service.info_attrs.category = real_new_category
    template_service.auth_attrs.owners.logins = []
    template_service.auth_attrs.owners.groups = []
    logging.info("Creating service %s", template_service.id)
    service = real_manager.create_service(
        template_service, "create service for functional tests"
    )

    assert service.runtime_attrs
    assert service.runtime_attrs.content
    assert service.runtime_attrs.snapshot_id
    assert service.info_attrs
    assert service.info_attrs.content
    assert service.info_attrs.snapshot_id
    assert service.auth_attrs
    assert service.auth_attrs.content
    assert service.auth_attrs.snapshot_id
    logging.info("Service id %s", service.id)
    # Ensure service.id is different from template service's id
    assert service.id != real_manager.get_service(real_nanny_service_id).id
    logging.info("Sleeping")
    time.sleep(5)
    logging.info("Deleting service %s", template_service.id)
    real_manager.delete_service(service)


def test_get_info_attrs(
    real_nanny_service_id, real_manager
):  # type: (unicode, ServiceRepoManager) -> None
    info_attrs = real_manager.get_info_attrs(real_nanny_service_id)
    assert info_attrs.content
    assert info_attrs.snapshot_id


def test_put_info_attrs(
    real_new_nanny_service, real_manager
):  # type: (Service, ServiceRepoManager) -> None
    real_new_nanny_service.info_attrs.desc = "test put_info_attrs"
    info_attrs = real_manager.put_info_attrs(
        real_new_nanny_service.id,
        real_new_nanny_service.info_attrs,
        "test put_info_attrs",
    )
    assert info_attrs.content
    assert info_attrs.snapshot_id


def test_get_runtime_attrs(
    real_nanny_service_id, real_manager
):  # type: (unicode, ServiceRepoManager) -> None
    runtime_attrs = real_manager.get_runtime_attrs(real_nanny_service_id)
    assert runtime_attrs.content
    assert runtime_attrs.snapshot_id


def test_runtime_info_attrs(
    real_new_nanny_service, real_manager
):  # type: (Service, ServiceRepoManager) -> None
    runtime_attrs = real_manager.put_runtime_attrs(
        real_new_nanny_service.id,
        real_new_nanny_service.runtime_attrs,
        "test put_runtime_attrs",
    )
    assert runtime_attrs.content
    assert runtime_attrs.snapshot_id


def test_get_auth_attrs(
    real_nanny_service_id, real_manager
):  # type: (unicode, ServiceRepoManager) -> None
    auth_attrs = real_manager.get_auth_attrs(real_nanny_service_id)
    assert auth_attrs.content
    assert auth_attrs.snapshot_id


def test_put_auth_attrs(
    real_new_nanny_service, real_manager
):  # type: (Service, ServiceRepoManager) -> None
    real_new_nanny_service.auth_attrs.owners.groups.append("test_group")
    auth_attrs = real_manager.put_auth_attrs(
        real_new_nanny_service.id,
        real_new_nanny_service.auth_attrs,
        "test put_auth_attrs",
    )

    assert auth_attrs.content
    assert auth_attrs.snapshot_id
