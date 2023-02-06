package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithContactInfo;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.vcard.service.VcardHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@ParametersAreNonnullByDefault
public class CampaignWithContactInfoUpdateOperationSupportOnAppliedChangesTest {
    public static final Long UID = nextLong();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private VcardHelper vcardHelper;

    @InjectMocks
    private CampaignWithContactInfoUpdateOperationSupport updateOperationSupport;

    @Test
    public void testVcardHelperCallsOnChangesApplied_NoContactInfoChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(false), getChanges(false)));
        verify(vcardHelper, never()).fillVcardsWithGeocoderData(anyCollection());
        verify(vcardHelper, never()).fillVcardsWithRegionIds(anyCollection());
    }

    @Test
    public void testVcardHelperCallsOnChangesApplied_PartialContactInfoChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(false), getChanges(true)));
        verify(vcardHelper).fillVcardsWithGeocoderData(anyCollection());
        verify(vcardHelper).fillVcardsWithRegionIds(anyCollection());
    }

    @Test
    public void testVcardHelperCallsOnChangesApplied_FullContactInfoChanges() {
        updateOperationSupport.onChangesApplied(null, List.of(getChanges(true), getChanges(true)));
        verify(vcardHelper).fillVcardsWithGeocoderData(anyCollection());
        verify(vcardHelper).fillVcardsWithRegionIds(anyCollection());
    }

    private static AppliedChanges<CampaignWithContactInfo> getChanges(boolean withContactInfo) {
        Long cid = nextLong();
        CampaignWithContactInfo campaign = new TextCampaign()
                .withId(cid);

        ModelChanges<CampaignWithContactInfo> campaignModelChanges = new ModelChanges<>(cid,
                CampaignWithContactInfo.class);

        if (withContactInfo) {
            campaignModelChanges.process(fullVcard(cid, UID), CampaignWithContactInfo.CONTACT_INFO);
        }

        return campaignModelChanges.applyTo(campaign);
    }

}
