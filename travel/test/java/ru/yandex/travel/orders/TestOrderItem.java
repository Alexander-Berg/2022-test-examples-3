package ru.yandex.travel.orders;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Setter;
import org.hibernate.annotations.Type;
import org.javamoney.moneta.Money;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.partners.BillingPartnerAgreement;

@Entity
@DiscriminatorValue("test")
public class TestOrderItem extends OrderItem {
    @Type(type = "jsonb-object")
    @Column(name = "payload")
    @Setter
    private Object payload;

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public EServiceType getPublicType() {
        return EServiceType.PT_EXPEDIA_HOTEL;
    }

    @Override
    public Enum getItemState() {
        return null;
    }

    @Override
    public LocalDateTime getServicedAt() {
        return LocalDateTime.now();
    }

    @Override
    public UUID getLogEntityId() {
        return null;
    }

    @Override
    public String getLogEntityType() {
        return null;
    }

    @Override
    public BillingPartnerAgreement getBillingPartnerAgreement() {
        return null;
    }

    @Override
    public Money preliminaryTotalCost() {
        return Money.zero(ProtoCurrencyUnit.RUB);
    }
}
