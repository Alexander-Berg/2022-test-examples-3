import pytest
from rest_framework import status, permissions
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.test import APIRequestFactory
from fan.testutils.matchers import assert_status_code
from fan.utils.permissions import additional_method_permissions, any_permission


class EndpointPermission(permissions.BasePermission):
    def has_permission(self, request, view):
        return EndpointPermission in request.user_permissions


class AnotherPermission(permissions.BasePermission):
    def has_permission(self, request, view):
        return AnotherPermission in request.user_permissions


class AdditionalMethodPermission(permissions.BasePermission):
    def has_permission(self, request, view):
        return AdditionalMethodPermission in request.user_permissions


class ErrorPermission(permissions.BasePermission):
    def has_permission(self, request, view):
        raise


class Endpoint(APIView):
    permission_classes = (EndpointPermission,)

    def get(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)

    @additional_method_permissions((AdditionalMethodPermission,))
    def put(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)

    @additional_method_permissions((any_permission(AdditionalMethodPermission, AnotherPermission),))
    def post(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)


class EndpointWithAnotherPermission(APIView):
    permission_classes = (any_permission(EndpointPermission, AnotherPermission),)

    def get(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)

    @additional_method_permissions((AdditionalMethodPermission,))
    def put(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)


class EndpointWithCombinationOfPermissions(APIView):
    permission_classes = (
        EndpointPermission,
        any_permission(AnotherPermission, AdditionalMethodPermission),
    )

    def get(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)


class EndpointWithErrorPermission(APIView):
    permission_classes = (any_permission(EndpointPermission, ErrorPermission),)

    def get(self, *args, **kwargs):
        return Response("OK", status=status.HTTP_200_OK)


@pytest.fixture
def endpoint():
    return Endpoint.as_view()


@pytest.fixture
def endpoint_with_another_permisison():
    return EndpointWithAnotherPermission.as_view()


@pytest.fixture
def endpoint_with_error_permisison():
    return EndpointWithErrorPermission.as_view()


@pytest.fixture
def endpoint_with_combination_of_permisisons():
    return EndpointWithCombinationOfPermissions.as_view()


def test_user_without_permissions(endpoint):
    response = do_request(endpoint, "get", [])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_user_with_permission(endpoint):
    response = do_request(endpoint, "get", [EndpointPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_with_method_permissions(endpoint):
    response = do_request(endpoint, "put", [EndpointPermission, AdditionalMethodPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_does_not_have_additional_method_permission(endpoint):
    response = do_request(endpoint, "put", [EndpointPermission])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_user_has_excessive_permissions(endpoint):
    response = do_request(endpoint, "get", [EndpointPermission, AdditionalMethodPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_method_permissions_does_not_overwrite_endpoint_permissions(endpoint):
    response = do_request(endpoint, "put", [EndpointPermission, AdditionalMethodPermission])
    response = do_request(endpoint, "get", [AdditionalMethodPermission])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)
    response = do_request(endpoint, "get", [EndpointPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_not_any_permission(endpoint_with_another_permisison):
    response = do_request(endpoint_with_another_permisison, "get", [])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_user_has_endpoint_permission(endpoint_with_another_permisison):
    response = do_request(endpoint_with_another_permisison, "get", [EndpointPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_another_permission(endpoint_with_another_permisison):
    response = do_request(endpoint_with_another_permisison, "get", [AnotherPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_both_permissions(endpoint_with_another_permisison):
    response = do_request(
        endpoint_with_another_permisison, "get", [AnotherPermission, EndpointPermission]
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_any_permission_in_additional_method_permissions(endpoint):
    response = do_request(endpoint, "post", [EndpointPermission, AnotherPermission])
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_not_any_permission_in_additional_method_permissions(endpoint):
    response = do_request(endpoint, "post", [EndpointPermission])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_user_has_additional_method_permissions_with_any_permission(
    endpoint_with_another_permisison,
):
    response = do_request(
        endpoint_with_another_permisison, "put", [EndpointPermission, AdditionalMethodPermission]
    )
    assert_status_code(response, status.HTTP_200_OK)


def test_user_has_not_additional_method_permissions_with_any_permission(
    endpoint_with_another_permisison,
):
    response = do_request(endpoint_with_another_permisison, "put", [EndpointPermission])
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_user_has_right_combination_of_permissions(endpoint_with_combination_of_permisisons):
    response = do_request(
        endpoint_with_combination_of_permisisons,
        "get",
        [EndpointPermission, AdditionalMethodPermission],
    )
    assert_status_code(response, status.HTTP_200_OK)


@pytest.mark.parametrize("permissions_list", ([EndpointPermission], [AdditionalMethodPermission]))
def test_user_has_wrong_combination_of_permissions(
    endpoint_with_combination_of_permisisons, permissions_list
):
    response = do_request(endpoint_with_combination_of_permisisons, "get", permissions_list)
    assert_status_code(response, status.HTTP_403_FORBIDDEN)


def test_lazy_any_permission_check(endpoint_with_error_permisison):
    response = do_request(endpoint_with_error_permisison, "get", [EndpointPermission])
    assert_status_code(response, status.HTTP_200_OK)


def do_request(endpoint, method, permissions):
    request = APIRequestFactory().generic(method, "")
    request.user_permissions = permissions
    return endpoint(request)
