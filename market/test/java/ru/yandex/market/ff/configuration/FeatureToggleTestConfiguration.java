package ru.yandex.market.ff.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.FeatureToggleService;

@Configuration
public class FeatureToggleTestConfiguration {

    @Primary
    @Bean
    public FeatureToggleService getFeatureToggleService(FTConfig config) {
        return new FeatureToggleService() {
            @Override
            public boolean is5thRegistryTypeOn(ShopRequest request) {
                return config.getSupplyEnabled();
            }

            @Override
            public boolean is5thRegistryTypeAllowed(Long supplierId, Long serviceId,
                                                    RequestType requestType) {
                return config.getSupplyEnabled();
            }

        };
    }

    @Bean
    public FTConfig getFTConfig() {
        return new FTConfig() {
            private boolean isSupplyEnabled = false;

            @Override
            public void setSupplyEnabled(boolean isSupplyEnabled) {
                this.isSupplyEnabled = isSupplyEnabled;
            }

            @Override
            public boolean getSupplyEnabled() {
                return isSupplyEnabled;
            }

            @Override
            public void cleanUp() {
                this.isSupplyEnabled = false;
            }
        };
    }

    public interface FTConfig {
        void setSupplyEnabled(boolean isSupplyEnabled);

        boolean getSupplyEnabled();

        void cleanUp();
    }
}
