package ru.yandex.market.crm.campaign.domain.sending;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushDevice;

/**
 * Информация о тестовом устройстве для отправки тестового пуш сообщения
 */
public class TestPushDevice {

    /**
     * Тип идентификатора устройства
     */
    @JsonProperty(value = "id_type", required = true)
    private DeviceIdType idType;

    /**
     * Значение идентификатора устройства
     */
    @JsonProperty(value = "id_value", required = true)
    private String idValue;

    /**
     * Имя устройства
     */
    @JsonProperty("name")
    private String name;

    /**
     * Признак, того что устройство выбрано для отправки на него тестовых пушей
     */
    @JsonProperty(value = "selected", required = true)
    private boolean selected;

    @SuppressWarnings("unused")
    public TestPushDevice() {
    }

    public TestPushDevice(DeviceIdType idType, String idValue, String name, boolean selected) {
        this.idType = idType;
        this.idValue = idValue;
        this.name = name;
        this.selected = selected;
    }

    public TestPushDevice(DeviceIdType idType, String idValue, String name) {
        this(idType, idValue, name, false);
    }

    public PushDevice toPushDevice() {
        return new PushDevice(this.idType, this.idValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }
        TestPushDevice other = (TestPushDevice) obj;
        return this.idType == other.idType && Objects.equals(this.idValue, other.idValue);
    }

    public DeviceIdType getIdType() {
        return idType;
    }

    public TestPushDevice setIdType(DeviceIdType idType) {
        this.idType = idType;
        return this;
    }

    public String getIdValue() {
        return idValue;
    }

    public TestPushDevice setIdValue(String idValue) {
        this.idValue = idValue;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestPushDevice setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idType, idValue);
    }

    public boolean isSelected() {
        return selected;
    }

    public TestPushDevice setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public String toString() {
        return "TestPushDevice{ type=" + this.idType + ", id=" + this.idValue + ", selected=" + this.selected + "}";
    }
}
