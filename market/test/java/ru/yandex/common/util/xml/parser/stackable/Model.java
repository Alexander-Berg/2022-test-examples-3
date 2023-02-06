package ru.yandex.common.util.xml.parser.stackable;

import java.util.List;

public class Model {
    public int id;
    public String name;
    public int price;
    public Details details;
    public List<Offer> offers;

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Model)) return false;

        Model model = (Model) o;

        if(id != model.id) return false;
        if(price != model.price) return false;
        if(details != null ? !details.equals(model.details) : model.details != null) return false;
        if(name != null ? !name.equals(model.name) : model.name != null) return false;
        if(offers != null ? !offers.equals(model.offers) : model.offers != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return "Model{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", price=" + price +
            ", details=" + details +
            ", offers=" + offers +
            "} " + super.toString();
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + price;
        result = 31 * result + (details != null ? details.hashCode() : 0);
        result = 31 * result + (offers != null ? offers.hashCode() : 0);
        return result;
    }
}
