package ru.yandex.travel.hotels.common.partners.travelline;

import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInfo;
import ru.yandex.travel.hotels.common.partners.travelline.utils.RoomMatchingHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class RoomMatchingTest {
    private HotelInfo hotelInfo;

    @SuppressWarnings("UnstableApiUsage")
    @Before
    public void loadHotelInfo() throws IOException {
        String data = Resources.toString(Resources.getResource("travelline/hotelRoomsWithAddresses.json"),
                Charset.defaultCharset());
        hotelInfo = DefaultTravellineClient.createObjectMapper().readerFor(HotelInfo.class).readValue(data);
    }

    @Test
    public void testRoomWithSameAddress() {
        var hotel = hotelInfo.getHotel();
        var room = hotelInfo.getHotel().getRoomTypes().get(0);
        assertThat(RoomMatchingHelper.checkRoomMatchesHotel(room, hotel)).isTrue();
    }

    @Test
    public void testRoomWithDifferentAddress() {
        var hotel = hotelInfo.getHotel();
        var room = hotelInfo.getHotel().getRoomTypes().get(1);
        assertThat(RoomMatchingHelper.checkRoomMatchesHotel(room, hotel)).isFalse();
    }

    @Test
    public void testRoomWithoutAddress() {
        var hotel = hotelInfo.getHotel();
        var room = hotelInfo.getHotel().getRoomTypes().get(2);
        assertThat(RoomMatchingHelper.checkRoomMatchesHotel(room, hotel)).isTrue();
    }
}
