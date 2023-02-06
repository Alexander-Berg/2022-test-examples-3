package ru.yandex.travel.orders.workflows.orderitem.dolphin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.common.orders.Guest;
import ru.yandex.travel.hotels.common.partners.dolphin.model.GuestName;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseDolphinHandlerTest {
    private final BaseDolphinHandler handler = new BaseDolphinHandler(null, null, null) {
    };

    private List<ru.yandex.travel.hotels.common.partners.dolphin.model.Guest> dolphinGuests;

    private List<Guest> itineraryGuests;

    @Before
    public void setUp() {
        itineraryGuests = new ArrayList<>(List.of(
                itineraryGuest("first", "last"),
                itineraryGuest("first2", "last2"),
                itineraryGuest("first3", "last3")
        ));
        dolphinGuests = new ArrayList<>(List.of(
                dolphinGuest("first", "last"),
                dolphinGuest("first2", "last2"),
                dolphinGuest("first3", "last3")
        ));
    }

    @Test
    public void testCheckAndUpdateGuestsOnSimilarLists() {
        handler.checkAndUpdateGuests(dolphinGuests, itineraryGuests);
        List<Guest> copy = List.of(
                itineraryGuest("first", "last"),
                itineraryGuest("first2", "last2"),
                itineraryGuest("first3", "last3")
        );
        assertThat(itineraryGuests).isEqualTo(copy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckAndUpdateGuestsOnListDifferentSize() {
        itineraryGuests.remove(1);
        handler.checkAndUpdateGuests(dolphinGuests, itineraryGuests);
    }

    @Test
    public void testCheckAndUpdateGuestsWhen2ndGuestWanNotFilled() {
        var guest2 = itineraryGuests.get(1);
        guest2.setLastName(null);
        guest2.setFirstName(null);

        // we would not send the 2nd guest in the list and would not receive it in response
        dolphinGuests.remove(1);

        handler.checkAndUpdateGuests(dolphinGuests, itineraryGuests);
        assertThat(itineraryGuests).isEqualTo(List.of(
                itineraryGuest("first", "last"),
                itineraryGuest(null, null),
                itineraryGuest("first3", "last3")
        ));
    }

    @Test
    public void testCheckAndUpdateGuestsWhenNameMismatch() {

        dolphinGuests.set(1, dolphinGuest("Александр", "Пушкин"));

        handler.checkAndUpdateGuests(dolphinGuests, itineraryGuests);

        assertThat(itineraryGuests).isEqualTo(List.of(
                itineraryGuest("first", "last"),
                itineraryGuest("Александр", "Пушкин"),
                itineraryGuest("first3", "last3")
        ));
    }

    private ru.yandex.travel.hotels.common.partners.dolphin.model.Guest dolphinGuest(String firstName,
                                                                                     String lastName) {
        return ru.yandex.travel.hotels.common.partners.dolphin.model.Guest.builder()
                .cyrillic(GuestName.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .build())
                .build();
    }

    private Guest itineraryGuest(String firstName,
                                 String lastName) {
        Guest guest = new Guest();
        guest.setFirstName(firstName);
        guest.setLastName(lastName);
        return guest;
    }
}
