package ru.yandex.market.tpl.core.domain.test_sc;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.common.db.util.HasId;

/**
 * Заказ в Сортировочном Центре (СЦ).
 *
 * @author kukabara
 */
@Entity

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestScOrder implements HasId<String> {

    /**
     * Идентификатор заказа в Доставке.
     */
    protected String externalOrderId;
    /**
     * Идентификатор заказа в 3PL.
     */
    @Id
    private String yandexId;
    /**
     * Идентификатор в СЦ.
     */
    private String orderPartnerId;
    private long partnerId;

    @Enumerated(EnumType.STRING)
    private OrderStatusType status;

    private Instant deliveryDate;

    private String courier;

    @CreationTimestamp
    private Instant createdAt;

    @Version
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestScOrder)) {
            return false;
        }
        TestScOrder that = (TestScOrder) o;
        if (this.getRealClass().isInstance(that)) {
            return false;
        }
        return this.identityEquals(that);
    }

    @Override
    public int hashCode() {
        return identityHashCode();
    }

    @Override
    public String toString() {
        return identityString();
    }

    @Override
    public String getId() {
        return yandexId;
    }

    @JsonProperty("statusName")
    public String getStatusName() {
        return status.name();
    }

}
