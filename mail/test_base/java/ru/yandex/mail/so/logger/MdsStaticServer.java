package ru.yandex.mail.so.logger;

import java.io.IOException;

import ru.yandex.collection.Pattern;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.util.request.RequestHandlerMapper;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;

public class MdsStaticServer
    extends BaseHttpServer<ImmutableBaseServerConfig>
{
    private final MdsRequestHandler mdsRequestHandler;

    public MdsStaticServer(final ImmutableBaseServerConfig config, final MdsStorageCluster storageCluster)
        throws IOException
    {
        super(config);
        mdsRequestHandler = new MdsRequestHandler(storageCluster);
        logger.info("MdsStaticServer: registering of handlers for '" + config.name() + "', port = " + port()
            + ", httpPort = " + httpPort());
        register(
            new Pattern<>(MdsRequestHandler.UPLOAD_URI + MdsStorageCluster.MDS_NAMESPACE + '/', true),
            mdsRequestHandler,
            RequestHandlerMapper.POST);
        register(
            new Pattern<>(MdsRequestHandler.DELETE_URI + MdsStorageCluster.MDS_NAMESPACE + '/', true),
            mdsRequestHandler,
            RequestHandlerMapper.GET, RequestHandlerMapper.POST);
        register(
            new Pattern<>(MdsRequestHandler.GET_URI + MdsStorageCluster.MDS_NAMESPACE + '/', true),
            mdsRequestHandler,
            RequestHandlerMapper.GET);
        register(
            new Pattern<>(MdsRequestHandler.HOSTNAME_URI, true),
            mdsRequestHandler,
            RequestHandlerMapper.GET);
        logger.info("MdsStaticServer: registering of handlers for '" + config.name() + "', port = " + port()
            + ", httpPort = " + httpPort() + " DONE");
    }

    public MdsRequestHandler handler() {
        return mdsRequestHandler;
    }
}
