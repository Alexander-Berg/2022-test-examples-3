package ru.yandex.market.wms.pipelinebuilder.pipeline.deployment;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Наборы тестов.
 */
public class TestsConfigs {
    private Map<String, TestConfig> configs;

    @JsonIgnore
    public TestConfig getConfig(String id) {
        return configs.computeIfAbsent(id, k -> {
                throw new RuntimeException("Test config " + k + " not found");
        });
    }

    public void setConfigs(Map<String, TestConfig> configs) {
        this.configs = configs;
    }

    /**
     * Конфигурация тестовых наборов.
     */
    public static class TestConfig {
        /**
         * Имя тестового набора.
         */
        private String title;

        /**
         * Описание тестового набора.
         */
        private String desc;

        private String targets;

        /**
         * Фильтры тестовых классов.
         */
        private String filter;

        private String clientTags;

        private String envVars;

        private String jvmArgs;

        private Long containerResource;

        private boolean generateAllureReport = true;

        private boolean requiresManualLaunch;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTargets() {
            return targets;
        }

        public void setTargets(String targets) {
            this.targets = targets;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getClientTags() {
            return clientTags;
        }

        public void setClientTags(String clientTags) {
            this.clientTags = clientTags;
        }

        public String getEnvVars() {
            return envVars;
        }

        public void setEnvVars(String envVars) {
            this.envVars = envVars;
        }

        public String getJvmArgs() {
            return jvmArgs;
        }

        public void setJvmArgs(String jvmArgs) {
            this.jvmArgs = jvmArgs;
        }

        public Long getContainerResource() {
            return containerResource;
        }

        public void setContainerResource(Long containerResource) {
            this.containerResource = containerResource;
        }

        public boolean isGenerateAllureReport() {
            return generateAllureReport;
        }

        public void setGenerateAllureReport(boolean generateAllureReport) {
            this.generateAllureReport = generateAllureReport;
        }

        public boolean isRequiresManualLaunch() {
            return requiresManualLaunch;
        }

        public void setRequiresManualLaunch(boolean requiresManualLaunch) {
            this.requiresManualLaunch = requiresManualLaunch;
        }
    }
}
