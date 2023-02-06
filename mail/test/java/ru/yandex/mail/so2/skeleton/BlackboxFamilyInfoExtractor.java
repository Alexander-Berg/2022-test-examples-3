package ru.yandex.mail.so2.skeleton;

import java.util.Collections;
import java.util.List;

import org.apache.http.concurrent.FutureCallback;

import ru.yandex.blackbox.BlackboxClient;
import ru.yandex.blackbox.BlackboxFamilyInfo;
import ru.yandex.blackbox.BlackboxFamilyInfoRequest;
import ru.yandex.blackbox.BlackboxNotFoundException;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.ImmutableHttpHostConfig;
import ru.yandex.http.util.AbstractFilterFutureCallback;
import ru.yandex.http.util.NotFoundException;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.mail.so.factors.SoFactor;
import ru.yandex.mail.so.factors.SoFunctionInputs;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractor;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorFactoryContext;
import ru.yandex.mail.so.factors.extractors.SoFactorsExtractorsRegistry;
import ru.yandex.mail.so.factors.types.JsonObjectSoFactorType;
import ru.yandex.mail.so.factors.types.SoFactorType;
import ru.yandex.mail.so.factors.types.StringSoFactorType;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public class BlackboxFamilyInfoExtractor implements SoFactorsExtractor {
    private static final List<SoFactorType<?>> INPUTS =
        Collections.singletonList(StringSoFactorType.STRING);
    private static final List<SoFactorType<?>> OUTPUTS =
        Collections.singletonList(JsonObjectSoFactorType.JSON_OBJECT);

    protected final BlackboxClient blackboxClient;

    public BlackboxFamilyInfoExtractor(
        final String name,
        final SoFactorsExtractorFactoryContext context,
        final IniConfig config)
        throws ConfigException
    {
        ImmutableHttpHostConfig blackboxConfig =
            new HttpHostConfigBuilder(config).build();
        blackboxClient =
            context.asyncClientRegistrar().registerClient(
                name,
                new BlackboxClient(
                    context.asyncClientRegistrar().reactor(),
                    blackboxConfig),
                blackboxConfig);
    }

    @Override
    public void close() {
    }

    @Override
    public List<SoFactorType<?>> inputs() {
        return INPUTS;
    }

    @Override
    public List<SoFactorType<?>> outputs() {
        return OUTPUTS;
    }

    @Override
    public void extract(
        final SoFactorsExtractorContext context,
        final SoFunctionInputs inputs,
        final FutureCallback<? super List<SoFactor<?>>> callback)
    {
        String familyId = inputs.get(0, StringSoFactorType.STRING);
        if (familyId == null) {
            callback.completed(NULL_RESULT);
            return;
        }
        BlackboxClient client = blackboxClient.adjust(context.httpContext());
        client.familyInfo(
            new BlackboxFamilyInfoRequest(familyId),
            context.requestsListener().createContextGeneratorFor(client),
            new Callback(callback));
    }

    @Override
    public void registerInternals(final SoFactorsExtractorsRegistry registry)
        throws ConfigException
    {
        BlackboxFamilyInfoExtractorFactory.INSTANCE.registerInternals(
            registry);
    }

    private static class Callback
        extends AbstractFilterFutureCallback<
            BlackboxFamilyInfo,
            List<SoFactor<?>>>
    {
        Callback(final FutureCallback<? super List<SoFactor<?>>> callback) {
            super(callback);
        }

        @Override
        public void failed(final Exception e) {
            if (e instanceof BlackboxNotFoundException) {
                callback.failed(new NotFoundException(e));
            } else {
                callback.failed(e);
            }
        }

        @Override
        public void completed(final BlackboxFamilyInfo familyInfo) {
            callback.completed(
                Collections.singletonList(
                    JsonObjectSoFactorType.JSON_OBJECT.createFactor(
                        familyInfo.toJsonObject(
                            BasicContainerFactory.INSTANCE))));
        }
    }
}

