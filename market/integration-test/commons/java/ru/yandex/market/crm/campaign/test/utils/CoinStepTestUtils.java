package ru.yandex.market.crm.campaign.test.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import ru.yandex.common.util.RandomUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.impl.ytree.object.YTreeSerializer;
import ru.yandex.inside.yt.kosher.impl.ytree.object.serializers.YTreeObjectSerializerFactory;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.external.loyalty.CoinsCreationResponse.IdentityWithCoin;
import ru.yandex.market.crm.external.loyalty.Identity;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.loyalty.Coin;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;

/**
 * @author apershukov
 */
public final class CoinStepTestUtils {

    public static StepOutputRow outputRowWithCoin(UidType idType, String idValue) {
        StepOutputRow row = outputRow(idType, idValue);

        Map<String, YTreeNode> vars = ImmutableMap.of(
                "COINS",
                YTree.listBuilder().value(issuedCoin()).buildList()
        );

        row.getData().setVars(vars);

        return row;
    }

    public static StepOutputRow outputRowWithCoin(String puid) {
        return outputRowWithCoin(UidType.PUID, puid);
    }

    private static YTreeNode issuedCoin() {
        Coin coin = new Coin();
        coin.setId(RandomUtils.nextIntInRange(0, 10_000));
        coin.setReason(EMAIL_COMPANY);
        coin.setTitle("Coin");
        coin.setSubtitle("Coin");
        coin.setCreationDate(LocalDateTime.now());

        coin.setEndDate(LocalDateTime.now().plusDays(1));
        coin.setFormattedEndDate(coin.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        coin.setImage("http://yandex.ru/image/");
        coin.setDefaultImage("http://yandex.ru/image/328x328");
        coin.setImages(Collections.emptyMap());
        coin.setBackgroundColor("#ffffff");
        coin.setStatus("ACTIVE");

        YTreeBuilder builder = YTree.builder();
        COIN_SERIALIZER.serialize(coin, builder);
        return builder.build();
    }

    public static YTreeMapNode issuedCoinRow(String puid) {
        return YTree.mapBuilder()
                .key("id_value").value(puid)
                .key("id_type").value(UidType.PUID.name())
                .key("coin").value(issuedCoin())
                .buildMap();
    }

    public static ru.yandex.market.crm.external.loyalty.Coin coin(IdentityWithCoin identityWithCoin) {
        return coin(identityWithCoin, UUID.randomUUID().toString());
    }

    public static ru.yandex.market.crm.external.loyalty.Coin coin(IdentityWithCoin identityWithCoin,
                                                                  String activationToken) {

        Identity<?> identity = identityWithCoin.getIdentity();

        boolean requireAuth;

        if (identity.getType() == Identity.Type.UID) {
            requireAuth = false;
            activationToken = null;
        } else {
            requireAuth = true;
        }

        return new ru.yandex.market.crm.external.loyalty.Coin(
                identityWithCoin.getCoinId(),
                0L,
                "Title",
                "subtitle",
                "FIXED",
                100500.,
                "",
                "",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "https://yandex.ru/mega-image",
                Collections.emptyMap(),
                "#FFFFFF",
                "ACTIVE",
                requireAuth,
                activationToken,
                EMAIL_COMPANY,
                null,
                null,
                null
        );
    }

    private static final YTreeSerializer<Coin> COIN_SERIALIZER = YTreeObjectSerializerFactory.forClass(Coin.class);

    private static final String EMAIL_COMPANY = "EMAIL_COMPANY";
}
