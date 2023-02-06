package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;

import ru.yandex.market.loyalty.api.model.OperationContextDto;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.core.model.OperationContext;

import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_IS_B2B_USER;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_USER_FULL_NAME_ID;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 14.06.17
 */
public class OperationContextFactory {
    public static final long DEFAULT_REGION = 213L;

    public static OperationContext uidOperationContext() {
        return withUidBuilder(UserDataFactory.DEFAULT_UID)
                .withCouponCode("coupon_code")
                .withOrderTotal(new BigDecimal("10.00"))
                .buildOperationContext();
    }

    public static OperationContextDto uidOperationContextDto() {
        return uidOperationContextDto(UserDataFactory.DEFAULT_UID);
    }

    public static OperationContextDto uidOperationContextDto(long uid) {
        return withUidBuilder(uid).buildOperationContextDto();
    }

    public static OperationContext yandexUidOperationContext() {
        return withYandexUidBuilder(UserDataFactory.DEFAULT_YANDEX_UID)
                .withCouponCode("coupon_code")
                .withOrderTotal(new BigDecimal("10.00"))
                .buildOperationContext();
    }

    public static Builder emptyBuilder() {
        return new Builder(new OperationContextDto());
    }

    public static Builder withUidBuilder(long uid) {
        OperationContextDto operationContextDto = new OperationContextDto();
        operationContextDto.setUid(uid);
        return new Builder(operationContextDto).withDefaults();
    }

    public static Builder withYandexUidBuilder(String yandexUid) {
        OperationContextDto operationContextDto = new OperationContextDto();
        operationContextDto.setYandexUid(yandexUid);
        return new Builder(operationContextDto).withDefaults();
    }

    public static class Builder {
        private final OperationContextDto operationContextDto;
        private String couponCode;
        private BigDecimal orderTotal;
        private DeviceInfoRequest deviceInfoRequest;

        Builder(OperationContextDto operationContextDto) {
            this.operationContextDto = operationContextDto;
        }

        Builder withDefaults() {
            this.operationContextDto.setIp("ip");
            this.operationContextDto.setUserAgent("user_agent");
            this.operationContextDto.setRegionId(DEFAULT_REGION);
            this.operationContextDto.setIpRegionId(DEFAULT_REGION);
            this.operationContextDto.setLastName("Kozlodoyev");
            this.operationContextDto.setFirstName("Spolzaet_po_kryshe");
            this.operationContextDto.setMiddleName("Starik");
            this.operationContextDto.setPersonalPhoneId(DEFAULT_PHONE_ID);
            this.operationContextDto.setEmail(DEFAULT_EMAIL);
            this.operationContextDto.setClientDeviceType(UsageClientDeviceType.DESKTOP);
            this.operationContextDto.setPersonalEmailId(DEFAULT_EMAIL_ID);
            this.operationContextDto.setPersonalFullNameId(DEFAULT_USER_FULL_NAME_ID);
            this.operationContextDto.setPersonalPhoneId(DEFAULT_PHONE_ID);
            this.operationContextDto.setIsB2B(DEFAULT_IS_B2B_USER);
            return this;
        }

        public OperationContextDto buildOperationContextDto() {
            return operationContextDto;
        }

        public Builder withCouponCode(String couponCode) {
            this.couponCode = couponCode;
            return this;
        }

        public Builder withOrderTotal(BigDecimal orderTotal) {
            this.orderTotal = orderTotal;
            return this;
        }

        public Builder withShopId(long shopId) {
            this.operationContextDto.setShopId(shopId);
            return this;
        }

        public Builder withPhone(String phone) {
            this.operationContextDto.setPhone(phone);
            return this;
        }

        public Builder withClientDevice(UsageClientDeviceType clientDeviceType) {
            this.operationContextDto.setClientDeviceType(clientDeviceType);
            return this;
        }

        public Builder withRegionId(long regionId) {
            this.operationContextDto.setRegionId(regionId);
            return this;
        }

        public Builder withDeviceInfoRequest(DeviceInfoRequest deviceInfoRequest) {
            this.deviceInfoRequest = deviceInfoRequest;
            return this;
        }

        public OperationContext buildOperationContext() {
            return new OperationContext(couponCode, orderTotal, operationContextDto, deviceInfoRequest);
        }
    }
}
