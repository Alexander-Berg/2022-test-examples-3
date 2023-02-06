package ru.yandex.market.checkout.checkouter.delivery;

import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;

/**
 * @author msavelyev
 */
public class AddressBuilder extends BaseBuilder<AddressImpl, AddressBuilder> {

    public AddressBuilder() {
        super(new AddressImpl());

        object.setCountry("value_country");
        object.setPostcode("value_postcode");
        object.setCity("value_city");
        object.setDistrict("value_district");
        object.setSubway("value_subway");
        object.setStreet("value_street");
        object.setKm("value_km");
        object.setHouse("value_house");
        object.setBuilding("value_building");
        object.setEstate("value_estate");
        object.setBlock("value_block");
        object.setEntrance("value_entrance");
        object.setEntryPhone("value_entryphone");
        object.setFloor("value_floor");
        object.setApartment("value_apartment");
        object.setGps("value_gps");
        object.setNotes("value_notes");
        object.setRecipient("value_recipient");
        object.setRecipientEmail("value_recipient_email");
    }

    public AddressBuilder withCountry(String country) {
        return withField("country", country);
    }

    public AddressBuilder withPostcode(String postcode) {
        return withField("postcode", postcode);
    }

    public AddressBuilder withCity(String city) {
        return withField("city", city);
    }

    public AddressBuilder withDistrict(String district) {
        return withField("district", district);
    }

    public AddressBuilder withSubway(String subway) {
        return withField("subway", subway);
    }

    public AddressBuilder withStreet(String street) {
        return withField("street", street);
    }

    public AddressBuilder withHouse(String house) {
        return withField("house", house);
    }

    public AddressBuilder withBlock(String block) {
        return withField("block", block);
    }

    public AddressBuilder withEntrance(String entrance) {
        return withField("entrance", entrance);
    }

    public AddressBuilder withEntryphone(String entryphone) {
        return withField("entryphone", entryphone);
    }

    public AddressBuilder withFloor(String floor) {
        return withField("floor", floor);
    }

    public AddressBuilder withApartment(String apartment) {
        return withField("apartment", apartment);
    }

    public AddressBuilder withRecipientEmail(String email) {
        return withField("recipient_email", email);
    }

    public AddressBuilder fromAddress(Address other) {
        AddressBuilder that = this.copy();
        AddressImpl object = copy().object;
        object.setCountry(other.getCountry());
        object.setPostcode(other.getPostcode());
        object.setCity(other.getCity());
        object.setDistrict(other.getDistrict());
        object.setSubway(other.getSubway());
        object.setStreet(other.getStreet());
        object.setKm(other.getKm());
        object.setHouse(other.getHouse());
        object.setBuilding(other.getBuilding());
        object.setEstate(other.getEstate());
        object.setBlock(other.getBlock());
        object.setEntrance(other.getEntrance());
        object.setEntryPhone(other.getEntryPhone());
        object.setFloor(other.getFloor());
        object.setApartment(other.getApartment());
        object.setGps(other.getGps());
        object.setNotes(other.getNotes());
        object.setRecipient(other.getRecipient());
        object.setRecipientEmail(other.getRecipientEmail());
        return that;
    }

}
