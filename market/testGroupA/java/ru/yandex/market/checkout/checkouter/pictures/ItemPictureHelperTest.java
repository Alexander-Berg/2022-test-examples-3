package ru.yandex.market.checkout.checkouter.pictures;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * * template from report
 * *     <ava-template>//avatars.mds.yandex.net/get-marketpic/{{group_id}}/market_{{picture_id}}/{{thumb_name}}</ava
 * *     -template>
 * *     <ava-universal-template>//avatars.mds.yandex.net/get-{{namespace}}/{{group_id}}/{{imagename}}/{{thumb_name}}
 * *     </ava-universal-template>
 */
public class ItemPictureHelperTest {

    private ItemPictureHelper itemPictureHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{
                        "//avatars.mds.yandex.net/get-marketpic/group_id/market_picture_id/50x50",
                        "//avatars.mds.yandex.net/get-marketpic/group_id/market_picture_id/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-namespace/group_id/imagename/50x50",
                        "//avatars.mds.yandex.net/get-namespace/group_id/imagename/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-marketpic/245020/market_B8xg_McqNDBvduSTrzqMKA/50x50",
                        "//avatars.mds.yandex.net/get-marketpic/245020/market_B8xg_McqNDBvduSTrzqMKA/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-marketpic/226983/market_bMZ9QzOMhjjNMEZXsrUo5w/74x100",
                        "//avatars.mds.yandex.net/get-marketpic/226983/market_bMZ9QzOMhjjNMEZXsrUo5w/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-mpic/1565610/img_id8085021731343731495.jpeg/50x50",
                        "//avatars.mds.yandex.net/get-mpic/1565610/img_id8085021731343731495.jpeg/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-mpic/1056698/img_id1709403873505071862.jpeg/orig",
                        "//avatars.mds.yandex.net/get-mpic/1056698/img_id1709403873505071862.jpeg/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-marketpic/1401054/market_uQTz2x0LHT-0tlAEtb7W2A/orig",
                        "//avatars.mds.yandex.net/get-marketpic/1401054/market_uQTz2x0LHT-0tlAEtb7W2A/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-mpic/331398/img_id1511219078302981698.jpeg/orig",
                        "//avatars.mds.yandex.net/get-mpic/331398/img_id1511219078302981698.jpeg/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-mpic/1111879/img_id3409426112761675667.png/orig",
                        "//avatars.mds.yandex.net/get-mpic/1111879/img_id3409426112761675667.png/"
                },
                new Object[]{
                        "//avatars.mds.yandex.net/get-marketpic/219360/market_i8BDaysJmzygi62Qj86zPw/orig",
                        "//avatars.mds.yandex.net/get-marketpic/219360/market_i8BDaysJmzygi62Qj86zPw/"
                }
        ).map(Arguments::of);
    }

    @BeforeEach
    public void setUp() throws Exception {
        PicRobotClientConfig picRobotClientConfig = new PicRobotClientConfig(
                "avatars.mds.yandex.net"
        );


        itemPictureHelper = new ItemPictureHelper();
        itemPictureHelper.setPicRobotClientConfig(picRobotClientConfig);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testAvatarsUrl(String pictureUrl, String expectedPreparedPictureURL) {
        String prepared = itemPictureHelper.preparePictureURL(pictureUrl);
        Assertions.assertEquals(expectedPreparedPictureURL, prepared);
    }
}
