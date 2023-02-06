package ru.yandex.direct.core.entity.client.repository;

import org.junit.Test;

import ru.yandex.direct.core.entity.client.model.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.client.repository.ClientOptionsMapping.CLIENTS_OPTION_FLAGS;
import static ru.yandex.direct.core.entity.client.repository.ClientOptionsMapping.extractOptionFlags;

public class ClientOptionsMappingExtractOptionFlagsTest {

    private final Client client = new Client();

    @Test
    public void noFlagsTests() {
        assertThat(extractOptionFlags(client)).isEqualTo("");
    }


    @Test
    public void oneEnabledFlagTest() {
        client.setNoDisplayHref(true);
        assertThat(extractOptionFlags(client)).isEqualTo("no_display_hrefs");
    }

    @Test
    public void twoEnabledFlagsTest() {
        client.setNoDisplayHref(true);
        client.setCanCopyCtr(true);
        assertThat(extractOptionFlags(client)).isEqualTo("no_display_hrefs,can_copy_ctr");
    }

    @Test
    public void twoEnabledFlagsDifferentOrderTest() {
        client.setCanCopyCtr(true);
        client.setNoDisplayHref(true);
        assertThat(extractOptionFlags(client)).isEqualTo("no_display_hrefs,can_copy_ctr");
    }

    @Test
    public void oneDisabledFlagTest() {
        client.setCanPayBeforeModeration(false);
        assertThat(extractOptionFlags(client)).isEqualTo("");
    }

    @Test
    public void enabledAndDisabledFlagsTest() {
        client.setCanCopyCtr(false);
        client.setAutoVideo(true);
        assertThat(extractOptionFlags(client)).isEqualTo("auto_video");
    }

    @Test
    public void allFlagsEnabledTest() {
        CLIENTS_OPTION_FLAGS.forEach(prop -> prop.set(client, true));

        assertThat(extractOptionFlags(client)).isEqualTo("no_text_autocorrection"
                + ",no_display_hrefs"
                + ",not_agreed_on_creatives_autogeneration"
                + ",can_copy_ctr"
                + ",not_convert_to_currency"
                + ",auto_video"
                + ",suspend_video"
                + ",feature_access_auto_video"
                + ",create_without_wallet"
                + ",feature_context_relevance_match_allowed"
                + ",feature_context_relevance_match_interface_only"
                + ",cant_unblock"
                + ",feature_payment_before_moderation"
                + ",as_soon_as_possible"
                + ",auto_overdraft_notified"
                + ",is_pro_strategy_view_enabled"
                + ",videohints_enabled"
                + ",is_touch"
                + ",is_conversion_multipliers_popup_disabled"
        );
    }
}
