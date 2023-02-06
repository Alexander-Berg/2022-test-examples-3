package ru.yandex.common.util.xml.parser.stackable;

public class Offer {
    public int id;
    public String shop;

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Offer)) return false;

        Offer offer = (Offer) o;

        if(id != offer.id) return false;
        if(shop != null ? !shop.equals(offer.shop) : offer.shop != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return "Offer{" +
            "id=" + id +
            ", shop='" + shop + '\'' +
            "} " + super.toString();
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (shop != null ? shop.hashCode() : 0);
        return result;
    }
}
