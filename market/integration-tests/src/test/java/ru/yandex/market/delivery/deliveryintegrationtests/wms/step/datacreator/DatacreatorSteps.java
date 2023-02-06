package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

public final class DatacreatorSteps {

    private DatacreatorSteps() {
        throw new UnsupportedOperationException();
    }

    private static final Label label = new Label();
    private static final Location location = new Location();
    private static final Users users = new Users();
    private static final Permission permission = new Permission();
    private static final Items items = new Items();
    private static final Inventorization inventorization = new Inventorization();
    private static final Order order = new Order();
    private static final TtsNotifications ttsNotifications = new TtsNotifications();
    private static final Tasks tasks = new Tasks();


    public static Label Label() {
        return label;
    }
    public static Location Location() {
        return location;
    }
    public static Users Users() {
        return users;
    }
    public static Permission Permission() {
        return permission;
    }
    public static Items Items() {
        return items;
    }
    public static Inventorization Inventorization() {
        return inventorization;}
    public static Order Order() {
        return order;
    }
    public static TtsNotifications TtsNotifications() {
        return ttsNotifications;}

    public static Tasks Tasks() {
        return tasks;
    }
}
