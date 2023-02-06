package ru.yandex.canvas.service.video;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import ru.yandex.canvas.TimeDelta;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.PresetTag;
import ru.yandex.canvas.service.video.presets.PresetTheme;
import ru.yandex.canvas.service.video.presets.VideoPreset;
import ru.yandex.canvas.service.video.presets.configs.BaseConfig;
import ru.yandex.canvas.service.video.presets.configs.ConfigType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VideoPresetsTest {

    /*
    {
  "theme_name": "caucasus",
  "disclaimer_present": true,
  "age_present": true,
  "skip_offset": 5,
  "title_background_color": "#000000",
  "age_selectable": false,
  "body_editable": false,
  "domain_present": true,
  "legal_present": true,
  "thumbnail": "preset_1_thumbnail",
  "title_bg_from_video": false,
  "tags": [
    "common"
  ],
  "preset_name": "VideoPreset caucasus name",
  "title_editable": false,
  "allow_stock_video": true,
  "body_background_color": "#000000",
  "title_present": true,
  "button_limited_color": true,
  "domain_editable": false,
  "body_present": true,
  "button_text_present": true,
  "body_text_color": "#ffffff",
  "preset_id": "1",
  "domain_color": "#70d1ff"
}
     */

    private PresetDescription makeDefaultDescription() {
        PresetDescription presetDescription = new PresetDescription();
        presetDescription.setPresetTheme(PresetTheme.CAUCASUS);
        presetDescription.setPresetName("Test1 name");
        presetDescription.setThumbnail("Test thumbnail");
        presetDescription.setSkipOffset(5L);
        presetDescription.setTags(Arrays.asList(PresetTag.COMMON, PresetTag.CPC));
        presetDescription.setPresetId(12L);

        return presetDescription;
    }

    private void makeDefaultAssertions(VideoPreset videoPreset) {
        assertEquals("presetId saved", videoPreset.getId(), Long.valueOf(12));
        assertEquals("presetThumbnail saved", videoPreset.getThumbnail(), "Test thumbnail");
        assertEquals("preset Bundle name saved", videoPreset.getBundleName(), "video-banner_theme_caucasus");
        assertEquals("presetTags", videoPreset.getTags(),
                new HashSet<>(Arrays.asList(PresetTag.COMMON, PresetTag.CPC)));

        assertEquals("skipOffset", videoPreset.getSkipOffset(), new TimeDelta(5));

        assertTrue("Config was built", videoPreset.getConfig() != null);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has addition record", config.containsKey(ConfigType.ADDITION));
        //assertTrue("Config has button record", config.containsKey(ConfigType.BUTTON));

        //TODO other elements..
        assertTrue("ElementList is not empty", videoPreset.getPresetElementList() != null);
        assertTrue("ElementList is not empty", videoPreset.getPresetElementList().size() != 0);
    }

    @Test
    public void presetWithoutAnythingTest() {
        PresetDescription presetDescription = makeDefaultDescription();

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 1);
    }

    @Test
    public void presetWithDisclaimerTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setDisclaimerPresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config has disclaimer record", config.containsKey(ConfigType.DISCLAIMER));
    }

    @Test
    public void presetWithAgeTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setAgePresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config has age record", config.containsKey(ConfigType.AGE));
    }

    @Test
    public void presetWithTitleTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setTitlePresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config has title record", config.containsKey(ConfigType.TITLE));
    }

    @Test
    public void presetWithBodyTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setBodyPresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config body record", config.containsKey(ConfigType.BODY));
    }

    @Test
    public void presetWithLegalTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setLegalPresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config body record", config.containsKey(ConfigType.LEGAL));
    }

    @Test
    public void presetWithDomainTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setDomainPresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config domain record", config.containsKey(ConfigType.DOMAIN));
    }

    @Test
    public void presetWithSubtitlesTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setSubtitlesPresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 2);
        assertTrue("Config subtitles record", config.containsKey(ConfigType.SUBTITLES));
    }

    @Test
    public void presetWithDomainTitleAndBodyTest() {
        PresetDescription presetDescription = makeDefaultDescription();
        presetDescription.setDomainPresent(true);
        presetDescription.setBodyPresent(true);
        presetDescription.setTitlePresent(true);

        VideoPreset videoPreset = new VideoPreset(presetDescription);

        makeDefaultAssertions(videoPreset);

        Map<ConfigType, BaseConfig> config = videoPreset.getConfig().getConfigs();

        assertTrue("Config has only 2 records", config.size() == 4);
        assertTrue("Config body record", config.containsKey(ConfigType.DOMAIN));
        assertTrue("Config body record", config.containsKey(ConfigType.BODY));
        assertTrue("Config body record", config.containsKey(ConfigType.TITLE));
    }

}
