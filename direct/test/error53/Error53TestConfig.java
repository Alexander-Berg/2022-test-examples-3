package ru.yandex.autotests.directapi.test.error53;

import java.util.Objects;

import org.apache.commons.beanutils.BeanMap;

import ru.yandex.autotests.directapi.apiclient.version5.ServiceNames;
import ru.yandex.autotests.directapi.model.api5.Action;

/**
 * Конфигурация для запуска теста на ошибку 53
 */
public final class Error53TestConfig {
    /**
     * Сервис для тестирования
     */
    private final ServiceNames serviceName;
    /**
     * Get действие
     */
    private final Action getAction;
    /**
     * Bean который необходимо передать get-действию
     */
    private final BeanMap getBean;
    /**
     * Любое не-get действие (null если сервис содержит только get-действие)
     */
    private final Action notGetAction;
    /**
     * Bean который необходимо передать не-get-действию
     */
    private final BeanMap notGetBean;
    /**
     * Промежуток времени на который устанавливается блокировка в минутах
     */
    private int lockTimeout;

    private Error53TestConfig(
            ServiceNames serviceName,
            Action getAction,
            BeanMap getBean,
            Action notGetAction,
            BeanMap notGetBean)
    {
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(getAction, "getAction");
        Objects.requireNonNull(getBean, "getBean");
        if (notGetAction != null) {
            Objects.requireNonNull(notGetBean, "notGetBean");
        }

        this.serviceName = serviceName;
        this.getAction = getAction;
        this.getBean = getBean;
        this.notGetAction = notGetAction;
        this.notGetBean = notGetBean;
    }

    public ServiceNames getServiceName() {
        return serviceName;
    }

    public Action getGetAction() {
        return getAction;
    }

    public BeanMap getGetBean() {
        return getBean;
    }

    public Action getNotGetAction() {
        return notGetAction;
    }

    public BeanMap getNotGetBean() {
        return notGetBean;
    }

    public int getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(int lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public static final class Builder {
        private ServiceNames serviceName;
        private Action getAction;
        private BeanMap getBean;
        private Action notGetAction;
        private BeanMap notGetBean;

        public ServiceNames getServiceName() {
            return serviceName;
        }

        public Builder withServiceName(ServiceNames serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Action getGetAction() {
            return getAction;
        }

        public Builder withGetAction(Action getAction) {
            this.getAction = getAction;
            return this;
        }

        public BeanMap getGetBean() {
            return getBean;
        }

        public Builder withGetBean(BeanMap getBean) {
            this.getBean = getBean;
            return this;
        }

        public Action getNotGetAction() {
            return notGetAction;
        }

        public Builder withNotGetAction(Action notGetAction) {
            this.notGetAction = notGetAction;
            return this;
        }

        public BeanMap getNotGetBean() {
            return notGetBean;
        }

        public Builder withNotGetBean(BeanMap notGetBean) {
            this.notGetBean = notGetBean;
            return this;
        }

        public Error53TestConfig build() {
            return new Error53TestConfig(
                    serviceName, getAction, getBean, notGetAction, notGetBean);
        }
    }
}
