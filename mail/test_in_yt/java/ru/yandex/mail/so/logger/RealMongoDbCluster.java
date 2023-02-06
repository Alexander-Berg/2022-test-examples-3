package ru.yandex.mail.so.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Feature;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.NumericVersion;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.process.ProcessOutput;
import de.flapdoodle.embed.process.config.store.DistributionDownloadPath;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.ImmutableDistribution;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.ExtractedFileSet;
import de.flapdoodle.embed.process.extract.ExtractedFileSets;
import de.flapdoodle.embed.process.extract.NoopTempNaming;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.StreamProcessor;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.Downloader;
import de.flapdoodle.embed.process.store.IArtifactStore;
import de.flapdoodle.embed.process.store.UrlConnectionDownloader;
import de.flapdoodle.os.ImmutablePlatform;
import de.flapdoodle.os.Platform;
import de.flapdoodle.os.Version;
import de.flapdoodle.os.common.DistinctPeculiarity;
import de.flapdoodle.os.common.HasPecularities;
import de.flapdoodle.os.common.Peculiarity;
import de.flapdoodle.os.common.attributes.Attribute;
import de.flapdoodle.os.common.matcher.Matchers;
import de.flapdoodle.os.common.types.OsReleaseFile;
import de.flapdoodle.os.common.types.OsReleaseFileConverter;
import de.flapdoodle.os.linux.OsReleaseFiles;
import de.flapdoodle.os.linux.UbuntuVersion;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.logger.PrefixedLogger;

public class RealMongoDbCluster implements Cluster {
    public static final String REPL_SET_NAME = "tempReplSet";
    public static final String DB_RULES = "rules";
    public static final String DB_ADMIN = "admin";
    public static final String DB_USER = "solog";
    public static final String DB_PASSWD = "solog";

    private static final Logger globallogger = LoggerFactory.getLogger(MockMongoDbCluster.class);
    private static final String sandboxResourcesRoot = ru.yandex.devtools.test.Paths.getSandboxResourcesRoot();
    private static final Platform defaultPlatform = Platform.detect();
    //private static final AtomicReference<MongodExecutable> mongoExecutable = new AtomicReference<>();
    //private static final AtomicReference<MongodConfig> mongoConfig = new AtomicReference<>();

    private final PrefixedLogger logger;
    private int port;
    private MongodStarter mongodStarter;
    private MongodExecutable mongodExecutable;
    private MongodProcess mongod;
    private SandboxResourceAwareArtifactStore artifactStore = null;
    private RuntimeConfig runtimeConfig;
    private Distribution distribution = null;
    private ExtractedFileSet extractedFileSet = null;
    private final Path tempDirPath;
    private final File tempDbDir;

    static {
        System.setProperty("EMBEDDED_MONGO_ARTIFACTS", sandboxResourcesRoot);
    }

    public enum OldUbuntuVersion implements Version {
        Ubuntu_14_04(osReleaseFileVersionMatches("14.04")),
        Ubuntu_16_04(osReleaseFileVersionMatches("16.04"));

        @SuppressWarnings("ImmutableEnumChecker")
        private final List<Peculiarity> peculiarities;

        OldUbuntuVersion(final DistinctPeculiarity<?>... peculiarities) {
            this.peculiarities  = HasPecularities.asList(peculiarities);
        }

        @Override
        public List<Peculiarity> pecularities() {
            return peculiarities;
        }

        public static Attribute<OsReleaseFile> osReleaseFile() {
            return releaseFile(OsReleaseFiles.RELEASE_FILE_NAME);
        }

        public static Attribute<OsReleaseFile> releaseFile(final String path) {
            return de.flapdoodle.os.common.attributes.Attributes.mappedTextFile(path, OsReleaseFileConverter.INSTANCE);
        }

        public static DistinctPeculiarity<OsReleaseFile> versionMatches(
            final Attribute<OsReleaseFile> osReleaseFile,
            final String version)
        {
            return DistinctPeculiarity.of(
                osReleaseFile,
                Matchers.osReleaseFileEntry("VERSION_ID", Pattern.quote(version) + ".*"));
        }

        public static DistinctPeculiarity<OsReleaseFile> osReleaseFileVersionMatches(final String version) {
            return versionMatches(osReleaseFile(), version);
        }
    }

    public enum MongoVersion implements IFeatureAwareVersion {
        V3_6_11("3.6.11", Feature.SYNC_DELAY, Feature.STORAGE_ENGINE, Feature.ONLY_64BIT, Feature.NO_CHUNKSIZE_ARG,
            Feature.NO_SOLARIS_SUPPORT, Feature.NO_HTTP_INTERFACE_ARG),
        V4_0_28("4.0.28", Feature.SYNC_DELAY, Feature.STORAGE_ENGINE, Feature.ONLY_64BIT, Feature.NO_CHUNKSIZE_ARG,
            Feature.NO_SOLARIS_SUPPORT, Feature.NO_HTTP_INTERFACE_ARG),
        V4_4_12("4.4.12", Feature.SYNC_DELAY, Feature.STORAGE_ENGINE, Feature.ONLY_64BIT, Feature.NO_CHUNKSIZE_ARG,
            Feature.NO_SOLARIS_SUPPORT, Feature.NO_HTTP_INTERFACE_ARG);

        public static final IFeatureAwareVersion DEFAULT = V4_0_28;
        public static final Map<Distribution, List<Pair<String, String>>> SANDBOX_MONGODB_RESOURCE_IDS;

        static {
            SANDBOX_MONGODB_RESOURCE_IDS = Map.of(
                ImmutableDistribution.builder().version(V3_6_11).platform(
                    ImmutablePlatform.copyOf(defaultPlatform).withVersion(OldUbuntuVersion.Ubuntu_14_04)).build(),
                // the 1st element is mandatory mongod executable, other - related libraries
                List.of(Pair.of("881247179", "mongodb-linux-x86_64-ubuntu1404-3.6.11.tgz"),
                    //Pair.of("2617307245", "openssl-1.0.0.tar.gz"),
                    Pair.of("2797808881", "curl.tar.gz")),
                ImmutableDistribution.builder().version(V4_0_28).platform(
                    ImmutablePlatform.copyOf(defaultPlatform).withVersion(OldUbuntuVersion.Ubuntu_16_04)).build(),
                List.of(Pair.of("2791671137", "mongodb-linux-x86_64-ubuntu1604-4.0.28.tgz"),
                    //Pair.of("2617307245", "openssl-1.0.0.tar.gz"),
                    Pair.of("2797808881", "curl.tar.gz")),
                ImmutableDistribution.builder().version(V4_0_28).platform(
                    ImmutablePlatform.copyOf(defaultPlatform).withVersion(UbuntuVersion.Ubuntu_18_04)).build(),
                    List.of(Pair.of("2791651469", "mongodb-linux-x86_64-ubuntu1804-4.0.28.tgz")),
                ImmutableDistribution.builder().version(V4_4_12).platform(
                    ImmutablePlatform.copyOf(defaultPlatform).withVersion(UbuntuVersion.Ubuntu_20_04)).build(),
                List.of(Pair.of("2791771331", "mongodb-linux-x86_64-ubuntu2004-4.4.12.tgz"),
                    //Pair.of("2617307245", "openssl-1.0.0.tar.gz"),
                    Pair.of("2797808881", "curl.tar.gz"))
            );
        }

        private final String version;
        @SuppressWarnings("ImmutableEnumChecker")
        private final EnumSet<Feature> features;

        MongoVersion(final String version, final Feature... features) {
            this.version = version;
            this.features = Feature.asSet(features);
        }

        @Override
        public String asInDownloadPath() {
            return version;
        }

        @Override
        public boolean enabled(final Feature feature) {
            return features.contains(feature);
        }

        @Override
        public EnumSet<Feature> getFeatures() {
            return EnumSet.copyOf(features);
        }

        @Override
        public String toString() {
            return "Version{" + version + '}';
        }

        @Override
        public NumericVersion numericVersion() {
            return NumericVersion.of(version);
        }
    }

    public RealMongoDbCluster(final PrefixedLogger logger) throws IOException {
        this.logger = logger;
        tempDirPath = Files.createTempDirectory("embeddedmongo-");
        tempDbDir = de.flapdoodle.embed.process.io.file.Files.createTempDir(tempDirPath.toFile(),"embedmongo-db");
    }

    @Override
    public void start() throws IOException {
        logger.info("Preparing of MongoDB's executable (1)...");
        port = Network.freeServerPort(Network.getLocalHost());
        artifactStore = new SandboxResourceAwareArtifactStore(logger, this);
        runtimeConfig = embeddedMongoRuntimeConfig(logger, artifactStore);
        mongodStarter = MongodStarter.getInstance(runtimeConfig);
        mongodExecutable = getDefaultExecutable(false);
        logger.info("Starting MongoDB (1)...");
        mongod = mongodExecutable.start();
        logger.info("MongoDB (1) started");
        setUpUsers();
        mongod.stop();
        logger.info("Preparing of MongoDB's executable (2)...");
        mongodExecutable = getDefaultExecutable(true);
        logger.info("Starting MongoDB (2)...");
        mongod = mongodExecutable.start();
        setUpReplication();
        logger.info("MongoDB (2) started");
    }

    @Override
    public void close() throws IOException {
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public PrefixedLogger logger() {
        return logger;
    }

    public MongodProcess mongod() {
        return mongod;
    }

    public Path tempDirPath() {
        return tempDirPath;
    }

    public Distribution distribution() {
        return distribution;
    }

    public void setDistribution(final Distribution distribution) {
        this.distribution = distribution;
    }

    public ExtractedFileSet extractedFileSet() {
        return extractedFileSet;
    }

    public void setExtractedFileSet(final ExtractedFileSet extractedFileSet) {
        this.extractedFileSet = extractedFileSet;
    }

    public static RuntimeConfig embeddedMongoRuntimeConfig(
        final PrefixedLogger logger,
        final IArtifactStore artifactStore)
    {
        var processOutput = ProcessOutput.builder()
            .output(new LogStreamProcessor(logger, Level.SEVERE))
            .error(new LogStreamProcessor(logger, Level.SEVERE))
            .commands(Processors.named("[console>]", new LogStreamProcessor(logger, Level.SEVERE)))
            .build();

        return Defaults.runtimeConfigFor(Command.MongoD, globallogger)
            .processOutput(processOutput)
            .artifactStore(artifactStore)
            .isDaemonProcess(false)
            .build();
    }

    private ImmutableMongodConfig mongodConfig(final IFeatureAwareVersion version, final boolean replicated)
        throws IOException
    {
        String localhost = InetAddress.getLoopbackAddress().getHostAddress();
        ImmutableMongodConfig.Builder mongodConfigBuilder = MongodConfig.builder()
            .version(version)
            .net(new Net(localhost, port, Network.localhostIsIPv6()))
            .cmdOptions(
                MongoCmdOptions.builder().useNoJournal(false).useNoPrealloc(false).useSmallFiles(false)
                    //.master(true)
                    .build());
        if (replicated) {
            mongodConfigBuilder.replication(new Storage(tempDbDir.getAbsolutePath(), REPL_SET_NAME, 128));
        } else {
            mongodConfigBuilder.replication(new Storage(tempDbDir.getAbsolutePath(), null, 0));
        }
        ImmutableMongodConfig mongodConfig = mongodConfigBuilder.build();
        logger.info("RealMongoDbCluster.getExecutable: mongodConfig = " + mongodConfig);
        return mongodConfig;
    }

    public MongodExecutable getExecutable(final IFeatureAwareVersion version, final boolean replicated) {
        ImmutableMongodConfig mongodConfig;
        try {
            logger.info("RealMongoDbCluster.getExecutable: version features = " + version.getFeatures()
                + ", tempDir = " + tempDirPath.toFile());
            mongodConfig = mongodConfig(version, replicated);
            if (distribution == null) {
                return mongodStarter.prepare(mongodConfig);
            } else {
                return new MongodExecutable(distribution, mongodConfig, runtimeConfig, extractedFileSet);
            }
        } catch (Exception e) {
            logger.info("RealMongoDbCluster.getExecutable: failed to obtain mongodStarter: " + e);
            return null;
        }
    }

    public MongoClient mongoClient(final String dbName, final String dbUser, final String dbPasswd) {
        StringBuilder sb = new StringBuilder("mongodb://");
        if (dbUser != null) {
            sb.append(dbUser);
            if (dbPasswd != null) {
                sb.append(':').append(dbPasswd);
            }
            sb.append('@');
        }
        sb.append("localhost:").append(port).append('/').append(dbName).append("?retryWrites=false");
        ConnectionString connString = new ConnectionString(sb.toString());
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
            .applyConnectionString(connString)
            .retryWrites(false);
        return MongoClients.create(settingsBuilder.build());
    }

    private void setUpUsers() {
        logger.info("MongoDB setting up of user '" + DB_ADMIN + "'");
        Document createUser;
        MongoDatabase database;
        MongoClient mongoClient;
        OperationSubscriber<Document> operationSubscriber =
            new OperationSubscriber<>("MongoDB set up user '" + DB_ADMIN + "'");
        mongoClient = mongoClient(DB_ADMIN, null, null);
        database = mongoClient.getDatabase(DB_ADMIN);
        createUser = new Document().append("createUser", DB_ADMIN).append("pwd", DB_PASSWD)
            .append("roles", List.of("userAdminAnyDatabase", "readWriteAnyDatabase"));
        database.runCommand(createUser).subscribe(operationSubscriber);
        operationSubscriber.await(60, TimeUnit.SECONDS);
        if (operationSubscriber.error() == null) {
            logger.info("MongoDB setting up of user '" + DB_ADMIN + "': successfully finished");
        } else {
            logger.info("MongoDB setting up of user '" + DB_ADMIN + "': " + operationSubscriber.error());
        }
        logger.info("MongoDB setting up of user '" + DB_USER + "'");
        database = mongoClient.getDatabase(DB_RULES);
        createUser = new Document().append("createUser", DB_USER).append("pwd", DB_PASSWD)
            .append("roles", List.of(Map.of("role", "dbOwner", "db", DB_RULES)));
        operationSubscriber = new OperationSubscriber<>("MongoDB set up user '" + DB_USER + "'");
        database.runCommand(createUser).subscribe(operationSubscriber);
        operationSubscriber.await(60, TimeUnit.SECONDS);
        if (operationSubscriber.error() == null) {
            logger.info("MongoDB setting up of user '" + DB_USER + "': successfully finished");
        } else {
            logger.info("MongoDB setting up of user '" + DB_USER + "': " + operationSubscriber.error());
        }
    }

    private void setUpReplication() {
        logger.info("MongoDB setting up of replication");
        MongoClient mongoClient = mongoClient(DB_ADMIN, DB_ADMIN, DB_PASSWD);
        MongoDatabase database = mongoClient.getDatabase(DB_ADMIN);
        Document replConfig = new Document().append("_id", REPL_SET_NAME).append("members", List.of(Map.of(
            "_id", 0, "host", "localhost:" + port
        )));
        OperationSubscriber<Document> operationSubscriber =
            new OperationSubscriber<>("MongoDB set up replication");
        database.runCommand(new Document().append("replSetInitiate", replConfig)).subscribe(operationSubscriber);
        operationSubscriber.await(60, TimeUnit.SECONDS);
        logger.info("MongoDB setting up of replication successfully finished");
    }

    public MongodExecutable getDefaultExecutable(final boolean replicated) {
        return getExecutable(MongoVersion.DEFAULT, replicated);
    }

    public static Distribution adjustDistribution(final Distribution distribution, final PrefixedLogger logger) {
        Distribution adjusted = Distribution.of(distribution.version(), defaultPlatform);
        if (!MongoVersion.SANDBOX_MONGODB_RESOURCE_IDS.containsKey(adjusted)) { // we suppose that only OS version not matched
            for (var someDistribution : MongoVersion.SANDBOX_MONGODB_RESOURCE_IDS.keySet()) {
                if (someDistribution.platform().operatingSystem().equals(distribution.platform().operatingSystem())) {
                    logger.warning("adjustDistribution: input distribution = " + distribution
                        + ", but we will see distribution = " + someDistribution);
                    if (distribution.platform().version().isPresent()
                            && someDistribution.platform().version().equals(distribution.platform().version()))
                    {
                        return someDistribution;
                    }
                }
            }
        }
        logger.warning("adjustDistribution: platform version not found for distribution = " + distribution);
        return Distribution.of(
            distribution.version(),
            ImmutablePlatform.copyOf(defaultPlatform).withVersion(UbuntuVersion.Ubuntu_18_04));
    }

    public static class SandboxDownloader implements Downloader {
        private static final Downloader defaultDownloader = Downloader.platformDefault();

        final PrefixedLogger logger;

        SandboxDownloader(final PrefixedLogger logger) {
            this.logger = logger;
        }

        @Override
        public String getDownloadUrl(DownloadConfig runtime, Distribution distribution) {
            return defaultDownloader.getDownloadUrl(runtime, distribution);
        }

        @Override
        public File download(DownloadConfig runtime, Distribution distribution) throws IOException {
            Distribution adjustedDistribution = adjustDistribution(distribution, logger);
            var url = getDownloadUrl(runtime, adjustedDistribution);
            var fileName = Paths.get(url).getFileName().toString();
            var localResource = localSandboxResource(fileName);
            if (localResource != null) {
                logger.log(Level.SEVERE, "Found local Sandbox resource " + localResource);
                return localResource.toFile();
            }
            // Falling back to the remote Sandbox resource.
            logger.log(Level.SEVERE, "SandboxDownloader.download: distribution=" + adjustedDistribution);
            for (var resourceInfo : MongoVersion.SANDBOX_MONGODB_RESOURCE_IDS.get(adjustedDistribution)) {
                if (resourceInfo != null) {
                    logger.log(Level.SEVERE, "Could not find the local Sandbox resource " + resourceInfo.getKey()
                        + " in the current directory. Make sure that the DATA tag in the makefile is set properly.");
                    try {
                        return new SandboxProxyDownloader(resourceInfo.getKey())
                                .download(runtime, adjustedDistribution);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "SandboxDownloader.download failed", e);
                        throw new IOException("SandboxDownloader.download failed", e);
                    }
                }
            }
            logger.log(Level.SEVERE, "Could not find the Sandbox resource for the distribution " + adjustedDistribution
                + ", falling back to the official MongoDB download site.");
            // Falling back to fastdl.mongodb.org.
            return defaultDownloader.download(runtime, adjustedDistribution);
        }

        private @Nullable Path localSandboxResource(String fileName) throws IOException {
            logger.info("SandboxDownloader.localSandboxResource: sandboxResourcesRoot=" + sandboxResourcesRoot);
            if (sandboxResourcesRoot == null) {
                return null;
            }
            var resourcePath = Paths.get(sandboxResourcesRoot, fileName);
            logger.info("SandboxDownloader.localSandboxResource: resourcePath=" + resourcePath
                + ", requested platform=" + defaultPlatform);
            if (!resourcePath.toFile().exists()) {
                logger.info("SandboxDownloader.localSandboxResource: file " + resourcePath.toFile()
                    + " does not exists!");
                return null;
            }
            var tempPath = Files.createTempFile(null, null);
            Files.copy(resourcePath, tempPath, StandardCopyOption.REPLACE_EXISTING);
            return tempPath;
        }
    }

    private static final class SandboxProxyDownloader extends UrlConnectionDownloader {
        private static final URI SANDBOX_PROXY_URI = SandboxDistributionUri.uri();

        private final String url;

        private SandboxProxyDownloader(String resourceId) {
            url = SANDBOX_PROXY_URI.resolve(resourceId).toASCIIString();
        }

        @Override
        public String getDownloadUrl(DownloadConfig runtime, Distribution distribution) {
            return url;
        }
    }

    private static class SandboxDistributionUri implements DistributionDownloadPath {
        public static final DistributionDownloadPath DEFAULT = new SandboxDistributionUri();
        public static final String SANDBOX_URI = "https://proxy.sandbox.yandex-team.ru/";

        @Override
        public String getPath(Distribution distribution) {
            return SANDBOX_URI;
        }

        public static URI uri() {
            return URI.create(SANDBOX_URI);
        }
    }

    public static class SandboxResourceAwareArtifactStore implements IArtifactStore {
        private final RealMongoDbCluster cluster;
        private final PrefixedLogger logger;
        private final Path tempDirPath;

        SandboxResourceAwareArtifactStore(final PrefixedLogger logger, final RealMongoDbCluster cluster) {
            this.cluster = cluster;
            this.logger = logger;
            this.tempDirPath = cluster.tempDirPath();
        }

        @SuppressWarnings("unused")
        public DownloadConfig downloadConfig() {
            return Defaults.downloadConfigFor(Command.MongoD).build();
        }

        @SuppressWarnings("unused")
        public Downloader downloader() {
            return new SandboxDownloader(logger);
        }

        public File tempDir() {
            return tempDirPath.toFile();
        }

        @SuppressWarnings("unused")
        public DirectoryAndExecutableNaming extraction() {
            return DirectoryAndExecutableNaming.of(new FixedPath(sandboxResourcesRoot), new NoopTempNaming());
        }

        public DirectoryAndExecutableNaming temp() {
            //return DirectoryAndExecutableNaming.of(new PropertyOrPlatformTempDir(), new UUIDTempNaming());
            return DirectoryAndExecutableNaming.of(new FixedPath(sandboxResourcesRoot), new NoopTempNaming());
        }

        @Override
        public Optional<ExtractedFileSet> extractFileSet(Distribution distribution) throws IOException {
            cluster.setDistribution(distribution);
            Distribution modifiedDistribution = adjustDistribution(distribution, logger);
            final var resources = MongoVersion.SANDBOX_MONGODB_RESOURCE_IDS.get(modifiedDistribution);
            if (resources == null) {
                logger.log(Level.SEVERE, "extractFileSet: No resource IDs found for distribution: " + distribution);
                return Optional.empty();
            }
            Optional<File> executable = Optional.empty();
            final Set<File> libraries = new HashSet<>();
            logger.info("extractFileSet: Path to extracted dir: " + tempDirPath);
            for (int i = 0; i < resources.size(); i++) {
                logger.info("extractFileSet: Resource id found: " + resources.get(i).getKey() + " for distribution "
                    + modifiedDistribution);
                final int finalI = i;
                var resourceInSandbox = Optional.of(sandboxResourcesRoot)
                    .map(sandboxResourcesRoot -> {
                        logger.info("extractFileSet: Looking in sandbox resources root (" + sandboxResourcesRoot
                            + "), will try to find resource file (" + resources.get(finalI).getValue() + ") in it");
                        return new File(sandboxResourcesRoot, resources.get(finalI).getValue());
                    })
                    .filter(File::exists)
                    .map(File::toPath);
                if (resourceInSandbox.isPresent()) {
                    final var archivePath = resourceInSandbox.get();
                    if (i == 0) {   // the 1st resource is mandatory resource with executable
                        executable = extractMongodBinary(archivePath.toFile(), tempDirPath);
                    } else {    // other resources are optional
                        libraries.addAll(extractLibrary(archivePath.toFile(), tempDirPath));
                    }
                } else {
                    logger.log(Level.SEVERE, "extractFileSet: Couldn't find sandbox resource "
                        + resources.get(i).getKey() + ", please ensure you add DATA(sbr://"
                        + resources.get(i).getValue() + ") to your test's ya.make");
                }
            }
            logger.info("extractFileSet: executable = " + executable.get());
            Optional<ExtractedFileSet> optionalExtractedFileSet =
                executable.map(file -> ExtractedFileSet.builder(tempDir())
                    .executable(file)
                    .addAllLibraryFiles(libraries)
                    .baseDirIsGenerated(true)
                    .build());
            cluster.setExtractedFileSet(optionalExtractedFileSet.get());
            return optionalExtractedFileSet;
        }

        protected Optional<File> extractMongodBinary(File archivePath, Path outDir) throws IOException {
            logger.info("extractMongodBinary: archive path = " + archivePath);
            try (
                var is = new FileInputStream(archivePath);
                var gcis = new GzipCompressorInputStream(is);
                var tais = new TarArchiveInputStream(gcis)
            ) {
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
                    logger.info("extractMongodBinary: Found in archive: " + entry.getName());
                    if (entry.getName().endsWith("/bin/mongod")) {
                        final File mongodFile = outDir.resolve("mongod").toFile();
                        logger.info("extractMongodBinary: start to unpacking to " + mongodFile + ", fileExists = "
                            + mongodFile.exists() + ", outDirExists = " + outDir.toFile().exists());
                        try (var os = new FileOutputStream(mongodFile)) {
                            IOUtils.copy(tais, os);
                        }
                        logger.info("extractMongodBinary: unpacked to " + mongodFile);
                        if (!mongodFile.setExecutable(true)) {
                            logger.log(Level.SEVERE, "extractMongodBinary: Couldn't make file executable: "
                                + mongodFile);
                            return Optional.empty();
                        }
                        return Optional.of(mongodFile);
                    }
                }
            }
            return Optional.empty();
        }

        protected Set<File> extractLibrary(File archivePath, Path outDir) throws IOException {
            Set<File> libFiles = new HashSet<>();
            try (
                var is = new FileInputStream(archivePath);
                var gcis = new GzipCompressorInputStream(is);
                var tais = new TarArchiveInputStream(gcis)
            ) {
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
                    logger.info("extractLibrary: Found in archive: " + entry.getName());
                    final File file = outDir.resolve(entry.getName()).toFile();
                    logger.info("extractLibrary: start to unpacking to " + file);
                    try (var os = new FileOutputStream(file)) {
                        IOUtils.copy(tais, os);
                    }
                    logger.info("extractLibrary: unpacked to " + file);
                    libFiles.add(file);
                }
            }
            return libFiles;
        }

        @Override
        public void removeFileSet(Distribution distribution, ExtractedFileSet files) {
            ExtractedFileSets.delete(files);
        }
    }

    public static class LogStreamProcessor implements StreamProcessor {

        private final PrefixedLogger logger;
        private final Level level;

        public LogStreamProcessor(PrefixedLogger logger, Level level) {
            this.logger = logger;
            this.level = level;
        }

        @Override
        public void process(String line) {
            logger.log(level, stripLineEndings(line));
        }

        @Override
        public void onProcessed() {
        }

        protected String stripLineEndings(String line) {
            // we still need to remove line endings that are passed on by StreamToLineProcessor...
            return line.replaceAll("[\n\r]+", "");
        }
    }
}
