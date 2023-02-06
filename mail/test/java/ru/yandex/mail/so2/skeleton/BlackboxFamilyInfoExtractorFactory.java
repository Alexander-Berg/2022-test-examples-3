package ru.yandex.mail.so2.skeleton;

import java.util.List;

import ru.yandex.mail.so.factors.SoFunctionArgumentInfo;
import ru.yandex.mail.so.factors.extractors.IniConfigSoFactorsExtractorFactory;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractor;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorFactoryContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so.factors.types.JsonObjectSoFactorType;
import ru.yandex.mail.so.factors.types.SoFactorType;
import ru.yandex.mail.so.factors.types.StringSoFactorType;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public enum BlackboxFamilyInfoExtractorFactory
    implements IniConfigSoFactorsExtractorFactory
{
    INSTANCE;

    @Override
    public void close() {
    }

    @Override
    public SoFactorsExtractor createIniConfigExtractor(
        final String name,
        final List<SoFunctionArgumentInfo> inputs,
        final List<SoFactorType<?>> outputs,
        final SoFactorsExtractorFactoryContext context,
        final IniConfig config)
        throws ConfigException
    {
        return new BlackboxFamilyInfoExtractor(name, context, config);
    }

    @Override
    public void registerInternals(final SoFactorsExtractorsRegistry registry)
        throws ConfigException
    {
        registry.typesRegistry().registerSoFactorType(
            StringSoFactorType.STRING);
        registry.typesRegistry().registerSoFactorType(
            JsonObjectSoFactorType.JSON_OBJECT);
    }
}

