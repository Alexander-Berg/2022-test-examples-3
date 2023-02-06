package ru.yandex.direct.core.testing.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.intapi.client.IntApiClient;
import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest;
import ru.yandex.direct.intapi.client.model.response.CampStatusModerate;
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse;

/**
 * Часто тест почему-то не компилируется, если этот класс не последний в списке Autowired.
 * В таком случае помогает передвинуть его на последнее место.
 */
public class IntApiClientStub extends IntApiClient {
    public IntApiClientStub() {
        super(null, (ParallelFetcherFactory) null, () -> null);
    }

    private List<TestNotification> notifications = new ArrayList<>();

    @Override
    public void addNotification(String notificationType, Map<String, Object> data, Map<String, Object> options) {
        notifications.add(new TestNotification(notificationType, data, options));
    }

    @Override
    public void unarcCampaign(Long uid, Long cid, Boolean force) {

    }

    @Override
    public void calculateCampaignStatusModerate(List<Long> cids) {
    }

    @Override
    public Map<Long, CampStatusModerate> calculateCampaignStatusModerateReadOnly(List<Long> cids) {
        return null;
    }

    @Override
    public CampaignStatisticsResponse getCampaignStatistics(CampaignStatisticsRequest request) {
        return null;
    }

    public List<TestNotification> getNotifications() {
        return notifications;
    }

    public void clear() {
        notifications.clear();
    }

    public static class TestNotification {
        private String notificationType;
        private Map<String, Object> data;
        private Map<String, Object> options;

        public TestNotification() {
        }

        public TestNotification(String notificationType,
                                Map<String, Object> data,
                                Map<String, Object> options) {
            this.notificationType = notificationType;
            this.data = data;
            this.options = options;
        }

        public void setNotificationType(String notificationType) {
            this.notificationType = notificationType;
        }

        public String getNotificationType() {
            return notificationType;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestNotification that = (TestNotification) o;
            return Objects.equals(notificationType, that.notificationType) && Objects.equals(data, that.data) && Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notificationType, data, options);
        }
    }

}
