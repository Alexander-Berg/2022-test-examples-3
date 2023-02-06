package ru.yandex.direct.test.mysql;

public class TestMysqlConfig {
    private final String includesPath;
    private final String binaryResourceName;
    private final String dataResourceName;

    private final String dockerImageFilename;
    private final String dockerImageArcadiaPath;

    public TestMysqlConfig(
            String includesPath, String binaryResourceName, String dataResourceName,
            String dockerImageFilename, String dockerImageArcadiaPath
    ) {
        this.includesPath = includesPath;
        this.binaryResourceName = binaryResourceName;
        this.dataResourceName = dataResourceName;
        this.dockerImageFilename = dockerImageFilename;
        this.dockerImageArcadiaPath = dockerImageArcadiaPath;
    }

    public static TestMysqlConfig directConfig() {
        return new TestMysqlConfig(
                "direct/libs/test-mysql", "mysql-server", "mysql-test-data",
                "/ru/yandex/direct/test/mysql/dbschema_docker_image.txt",
                "direct/libs/test-mysql/src/main/resources/" +
                        "/ru/yandex/direct/test/mysql/dbschema_docker_image.txt"
        );
    }

    public String getIncludesPath() {
        return includesPath;
    }

    public String getBinaryResourceName() {
        return binaryResourceName;
    }

    public String getDataResourceName() {
        return dataResourceName;
    }

    public String getDockerImageFilename() {
        return dockerImageFilename;
    }

    public String getDockerImageArcadiaPath() {
        return dockerImageArcadiaPath;
    }
}
