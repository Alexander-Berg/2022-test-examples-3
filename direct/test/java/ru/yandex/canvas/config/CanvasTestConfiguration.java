package ru.yandex.canvas.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.commons.collections.IteratorUtils;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.validation.beanvalidation.LocaleContextMessageInterpolator;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.filter.CharacterEncodingFilter;

import ru.yandex.canvas.Application;
import ru.yandex.canvas.controllers.FilesController;
import ru.yandex.canvas.controllers.RootController;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.MDSService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.html5.PhantomJsCreativesValidator;
import ru.yandex.canvas.service.scraper.ScraperService;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterScreenshot;
import ru.yandex.direct.utils.NamedThreadFactory;
import ru.yandex.testenv.common.mongo.embed.VersionExt;
import ru.yandex.testenv.common.mongo.embed.YaMongoDownloader;
import ru.yandex.testenv.common.mongo.embed.YaMongodStarter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.devtools.test.Paths.getSandboxResourcesRoot;

@Import({Application.class})
@Configuration
public class CanvasTestConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(CanvasTestConfiguration.class);
    public static final String DATABASE_NAME = "canvas";

    //todo заменить на сканирование аннотаций @Document https://st.yandex-team.ru/DIRECT-168124
    public static final List<String> MONGO_COLLECTIONS = Arrays.asList(
            "creative_batch",
            "files",
            "html5_batches",
            "idea",
            "sequence",
            "stock_video_additions",
            "video_additions",
            "video_files",
            "video_constructor_feed_files",
            "video_constructor_files",
            "audio_files",
            "html5_sources",
            "overlay_creatives",
            "overlay_bundles");

    private static final AtomicReference<MongodExecutable> mongoExecutable = new AtomicReference<>();
    private static final AtomicReference<IMongodConfig> mongoConfig = new AtomicReference<>();

    // Mocks
    @Bean
    public GridFsTemplate gridFsTemplate() {
        return mock(GridFsTemplate.class);
    }

    @Bean
    public AvatarsService avatarsService() {
        return mock(AvatarsService.class);
    }

    @Bean
    @Primary
    public DirectService directService() {
        return mock(DirectService.class);
    }

    @Bean
    public RTBHostExportService rtbHostExportService() {
        return mock(RTBHostExportService.class);
    }

    @Bean
    public ScraperService scraperService() {
        return mock(ScraperService.class);
    }

    @Bean
    public AuthRequestParams authRequestParams() {
        return mock(AuthRequestParams.class);
    }

    @Bean
    public AuthService authService() {
        return mock(AuthService.class);
    }

    @Bean
    public RootController rootController() {
        return mock(RootController.class);
    }

    @Bean
    public FilesController filesController() {
        return mock(FilesController.class);
    }

    @Bean
    public MDSService mdsService() {
        return mock(MDSService.class);
    }

    @Bean
    public SessionParams sessionParams() {
        return mock(SessionParams.class);
    }

    @Bean
    public VideoFilesRepository videoFilesRepository() {
        return mock(VideoFilesRepository.class);
    }

    // Test beans

    @Bean
    public ConstraintValidatorFactory constraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
        return new SpringConstraintValidatorFactory(beanFactory);
    }

    @Bean
    public MessageInterpolator messageInterpolator() {
        return new LocaleContextMessageInterpolator(new ResourceBundleMessageInterpolator());
    }

    @Bean
    public ValidatorFactory validatorFactory(
            ConstraintValidatorFactory constraintValidatorFactory,
            MessageInterpolator messageInterpolator) {
        return Validation.byProvider(HibernateValidator.class)
                .configure()
                .constraintValidatorFactory(constraintValidatorFactory)
                .messageInterpolator(messageInterpolator)
                .buildValidatorFactory();
    }

    @Bean
    public StillageService stillageService() {
        StillageService service = mock(StillageService.class);

        StillageFileInfo mockInfo = new StillageFileInfo();
        mockInfo.setUrl("http://my.file.url");
        mockInfo.setId("id");
        mockInfo.setFileSize(128);

        when(service.uploadFile(anyString(), any(URL.class))).thenReturn(mockInfo);
        when(service.uploadFile(anyString(), any(byte[].class))).thenReturn(mockInfo);

        return service;
    }

    @Bean
    public PhantomJsCreativesValidator phantomJsCreativesValidator() {
        PhantomJsCreativesValidator validator = mock(PhantomJsCreativesValidator.class);

        // No way to test phantomJsCreativesValidator with mocked StillageService
        when(validator.checkForExternalRequests(anyList())).thenReturn(List.of());

        return validator;
    }

    @Bean
    public ScreenshooterService screenshooterService() {
        ScreenshooterService service = mock(ScreenshooterService.class);

        ScreenShooterScreenshot mockResponse = new ScreenShooterScreenshot()
                .withUrl("http://my.screenshot.url")
                .withIsDone(true);

        when(service.getScreenshotFromUrl(anyString(), anyLong(), anyLong())).thenReturn(mockResponse);
        when(service.getScreenshotFromHtml(anyString(), anyLong(), anyLong())).thenReturn(mockResponse);

        return service;
    }


    // Test MongoDB
    @Bean
    public synchronized MongodExecutable embeddedMongoServer(IMongodConfig mongoConfig) throws IOException {
        String recipeMongoPort = System.getenv("RECIPE_MONGO_PORT");
        if (recipeMongoPort != null) {
            //если  запущен рецепт с монгой из ya make, то встроенную можно не стартовать
            logger.warn("RECIPE_MONGO mode. Skip start embeddedMongoServer");
            return null;
        }
        if (mongoExecutable.get() == null) {
            MongodStarter starter;
            if (getSandboxResourcesRoot() != null) {
                starter = YaMongodStarter.getDefaultInstance();
            } else {
                IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                        .defaults(Command.MongoD)
                        .artifactStore(new ExtractedArtifactStoreBuilder().defaults(Command.MongoD)
                                .download(new DownloadConfigBuilder()
                                        .defaultsForCommand(Command.MongoD)
                                        .build())
                                .downloader(new YaMongoDownloader()))
                        .build();
                starter = MongodStarter.getInstance(runtimeConfig);
            }

            MongodExecutable executable = starter.prepare(mongoConfig);
            executable.start();
            Thread shutdownHook = new NamedThreadFactory("GracefulShutdownHook").newThread(executable::stop);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            mongoExecutable.set(executable);
        }
        return mongoExecutable.get();
    }

    @Bean
    public IMongodConfig mongoConfig() throws IOException {
        if (mongoConfig.get() == null) {
            String localhost = InetAddress.getLoopbackAddress().getHostAddress();
            mongoConfig.set(new MongodConfigBuilder()
                    .version(VersionExt.V3_6_5)
                    .net(new Net(localhost, Network.getFreeServerPort(), Network.localhostIsIPv6()))
                    .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(true)
                            .useNoPrealloc(true)
                            .syncDelay(100000)
                            .build())
                    .build());
        }
        return mongoConfig.get();
    }

    @Bean
    public MongoClient client(IMongodConfig mongodConfig) {
        String recipeMongoPort = System.getenv("RECIPE_MONGO_PORT");
        if (recipeMongoPort != null) {
            logger.warn("RECIPE_MONGO client. Port = {}", recipeMongoPort);
            return new MongoClient("127.0.0.1", Integer.valueOf(recipeMongoPort));
        }
        logger.warn("embeddedMongoServer. Port = {}", mongodConfig.net().getPort());
        return new MongoClient(mongodConfig.net().getBindIp(), mongodConfig.net().getPort());
    }

    @Bean
    @Primary
    public MongoDbFactory mongoDbFactory(MongoClient client) {
        MongoDbFactory factory = new SimpleMongoDbFactory(client, DATABASE_NAME);
        List<String> existing = IteratorUtils.toList(factory.getDb().listCollectionNames().iterator());

        if (!MONGO_COLLECTIONS.containsAll(existing) || !existing.containsAll(MONGO_COLLECTIONS)) {
            factory.getDb().drop();
            MONGO_COLLECTIONS
                    .forEach(collectionName -> factory.getDb().createCollection(collectionName));
        }
        return factory;
    }

    @Bean
    public MockMvcBuilderCustomizer defaultCharacterEncodingMockMvcBuilderCustomizer() {
        return builder -> builder.addFilters(
                new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true, true));
    }
}
