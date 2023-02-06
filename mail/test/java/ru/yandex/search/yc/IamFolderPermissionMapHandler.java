package ru.yandex.search.yc;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeRequest;
import yandex.cloud.priv.servicecontrol.v1.AccessServiceOuterClass.AuthorizeResponse;
import yandex.cloud.priv.servicecontrol.v1.PR.Resource;

import ru.yandex.search.yc.iam.IamResourceFilter;

public class IamFolderPermissionMapHandler
    implements BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse>,
    GrpcHandlerSupplier<AuthorizeRequest, AuthorizeResponse>
{
    private final Map<String, Set<String>> folders;
    private final String expectedToken;

    /**
     *
     * @param expectedToken token
     * @param folders - map folder -> set of permissions
     */
    public IamFolderPermissionMapHandler(
        final String expectedToken,
        final Map<String, Set<String>> folders)
    {
        this.folders = folders;
        this.expectedToken = expectedToken;
    }

    public IamFolderPermissionMapHandler(
        final String expectedToken,
        final String folder,
        final String... permissions)
    {
        this(
            expectedToken,
            Collections.singletonMap(
                folder,
                new LinkedHashSet<>(Arrays.asList(permissions))));
    }

    @Override
    public AuthorizeResponse apply(
        final AuthorizeRequest request,
        final StreamObserver<AuthorizeResponse> observer)
    {
        List<Resource> resourceList = request.getResourcePathList();
        for (Resource resource : resourceList) {
            if (!expectedToken.equals(request.getIamToken())) {
                observer.onError(new StatusRuntimeException(Status.UNAUTHENTICATED));
                return null;
            }

            if (!IamResourceFilter.RESOURCE_TYPE_FOLDER.equals(resource.getType())) {
                observer.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
                return null;
            }

            Set<String> permissions = folders.get(resource.getId());
            if (permissions == null || !permissions.contains(request.getPermission())) {
                observer.onError(new StatusRuntimeException(Status.PERMISSION_DENIED));
                return null;
            }
        }
        AuthorizeResponse response = AuthorizeResponse.newBuilder().build();
        observer.onNext(response);
        observer.onCompleted();
        return null;
    }

    @Override
    public BiFunction<AuthorizeRequest, StreamObserver<AuthorizeResponse>, AuthorizeResponse> next() {
        return this;
    }
}
