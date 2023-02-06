package ru.yandex.mail.so2.skeleton;

import java.io.IOException;

import ru.yandex.collection.Pattern;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so2.skeleton.config.ImmutableSo2SkeletonConfig;
import ru.yandex.parser.config.ConfigException;

public class So2SkeletonTestServer
    extends So2SkeletonServer<ImmutableSo2SkeletonConfig>
{
    public So2SkeletonTestServer(final ImmutableSo2SkeletonConfig config)
        throws ConfigException, IOException
    {
        super(config);
        register(new Pattern<>("", true), new So2SkeletonHandler(this));
    }

    @Override
    protected SoFactorsExtractorsRegistry createExtractorsRegistry()
        throws ConfigException
    {
        SoFactorsExtractorsRegistry extractorsRegistry =
            super.createExtractorsRegistry();
        extractorsRegistry.registerExtractorFactory(
            "blackbox_family_info",
            BlackboxFamilyInfoExtractorFactory.INSTANCE);
        return extractorsRegistry;
    }
}

