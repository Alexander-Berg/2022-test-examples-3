package ru.yandex.market.checkout.checkouter.yauslugi.model;

import java.util.function.Function;

/**
 * @author zagidullinri
 * @date 21.09.2021
 */
public abstract class AddressDtoProvider {

    public static final String DEFAULT_COUNTRY = "someCountry";
    public static final String DEFAULT_POSTCODE = "somePostcode";
    public static final String DEFAULT_CITY = "someCity";
    public static final String DEFAULT_DISTRICT = "someDistrict";
    public static final String DEFAULT_SUBWAY = "someSubway";
    public static final String DEFAULT_STREET = "someStreet";
    public static final String DEFAULT_HOUSE = "someHouse";
    public static final String DEFAULT_BLOCK = "someBlock";
    public static final String DEFAULT_ENTRANCE = "someEntrance";
    public static final String DEFAULT_ENTRY_PHONE = "someEntryPhone";
    public static final String DEFAULT_FLOOR = "someFloor";
    public static final String DEFAULT_RECIPIENT_FIRST_NAME = "someRecipientFirstName";
    public static final String DEFAULT_RECIPIENT_LAST_NAME = "someRecipientLastName";
    public static final String DEFAULT_APARTMENT = "someApartment";
    public static final String DEFAULT_RECIPIENT_EMAIL = "recipient@mail.com";
    public static final String DEFAULT_GPS = "37.605373,55.767909";

    public static AddressDto defaultAddressDto() {
        return builder()
                .configure(AddressDtoProvider::applyDefaults)
                .build();
    }

    private static AddressDtoBuilder applyDefaults(AddressDtoBuilder addressDtoBuilder) {
        return addressDtoBuilder
                .withCountry(DEFAULT_COUNTRY)
                .withPostcode(DEFAULT_POSTCODE)
                .withCity(DEFAULT_CITY)
                .withDistrict(DEFAULT_DISTRICT)
                .withSubway(DEFAULT_SUBWAY)
                .withStreet(DEFAULT_STREET)
                .withHouse(DEFAULT_HOUSE)
                .withBlock(DEFAULT_BLOCK)
                .withEntrance(DEFAULT_ENTRANCE)
                .withEntryPhone(DEFAULT_ENTRY_PHONE)
                .withFloor(DEFAULT_FLOOR)
                .withRecipientFirstName(DEFAULT_RECIPIENT_FIRST_NAME)
                .withRecipientLastName(DEFAULT_RECIPIENT_LAST_NAME)
                .withApartment(DEFAULT_APARTMENT)
                .withRecipientEmail(DEFAULT_RECIPIENT_EMAIL)
                .withGps(DEFAULT_GPS);
    }

    public static AddressDtoBuilder builder() {
        return new AddressDtoBuilder();
    }

    public static class AddressDtoBuilder {

        private String country;
        private String postcode;
        private String city;
        private String district;
        private String subway;
        private String street;
        private String house;
        private String block;
        private String entrance;
        private String entryPhone;
        private String floor;
        private String recipientFirstName;
        private String recipientLastName;
        private String apartment;
        private String recipientEmail;
        private String gps;

        private AddressDtoBuilder() {

        }

        public AddressDtoBuilder configure(Function<AddressDtoBuilder, AddressDtoBuilder> function) {
            return function.apply(this);
        }

        public AddressDtoBuilder withCountry(String country) {
            this.country = country;
            return this;
        }

        public AddressDtoBuilder withPostcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public AddressDtoBuilder withCity(String city) {
            this.city = city;
            return this;
        }

        public AddressDtoBuilder withDistrict(String district) {
            this.district = district;
            return this;
        }

        public AddressDtoBuilder withSubway(String subway) {
            this.subway = subway;
            return this;
        }

        public AddressDtoBuilder withStreet(String street) {
            this.street = street;
            return this;
        }

        public AddressDtoBuilder withHouse(String house) {
            this.house = house;
            return this;
        }

        public AddressDtoBuilder withBlock(String block) {
            this.block = block;
            return this;
        }

        public AddressDtoBuilder withEntrance(String entrance) {
            this.entrance = entrance;
            return this;
        }

        public AddressDtoBuilder withEntryPhone(String entryPhone) {
            this.entryPhone = entryPhone;
            return this;
        }

        public AddressDtoBuilder withFloor(String floor) {
            this.floor = floor;
            return this;
        }

        public AddressDtoBuilder withRecipientFirstName(String recipientFirstName) {
            this.recipientFirstName = recipientFirstName;
            return this;
        }

        public AddressDtoBuilder withRecipientLastName(String recipientLastName) {
            this.recipientLastName = recipientLastName;
            return this;
        }

        public AddressDtoBuilder withApartment(String apartment) {
            this.apartment = apartment;
            return this;
        }

        public AddressDtoBuilder withRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public AddressDtoBuilder withGps(String gps) {
            this.gps = gps;
            return this;
        }

        public AddressDto build() {
            AddressDto addressDto = new AddressDto();
            addressDto.setCountry(country);
            addressDto.setPostcode(postcode);
            addressDto.setCity(city);
            addressDto.setDistrict(district);
            addressDto.setSubway(subway);
            addressDto.setStreet(street);
            addressDto.setHouse(house);
            addressDto.setBlock(block);
            addressDto.setEntrance(entrance);
            addressDto.setEntryPhone(entryPhone);
            addressDto.setFloor(floor);
            addressDto.setRecipientFirstName(recipientFirstName);
            addressDto.setRecipientLastName(recipientLastName);
            addressDto.setApartment(apartment);
            addressDto.setRecipientEmail(recipientEmail);
            addressDto.setGps(gps);
            return addressDto;
        }
    }
}
