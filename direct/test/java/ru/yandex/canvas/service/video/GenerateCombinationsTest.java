package ru.yandex.canvas.service.video;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.repository.video.StockVideoAdditionsRepository;
import ru.yandex.canvas.repository.video.VideoAdditionsRepository;
import ru.yandex.canvas.service.video.files.StockMoviesService;
import ru.yandex.canvas.service.video.presets.PresetDescription;
import ru.yandex.canvas.service.video.presets.PresetTag;
import ru.yandex.canvas.service.video.presets.PresetTheme;
import ru.yandex.canvas.service.video.presets.VideoPreset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(SpringJUnit4ClassRunner.class)
public class GenerateCombinationsTest {

    @MockBean
    private StockMoviesService stockMoviesService;
    @MockBean
    private VideoAdditionsService videoAdditionsService;
    @MockBean
    private VideoAdditionsRepository videoAdditionsRepository;
    @MockBean
    private StockVideoAdditionsRepository stockVideoAdditionsRepository;
    @MockBean
    private VideoCreativesService videoCreativesService;
    @MockBean
    private VideoPresetsService videoPresetsService;

    CreativesGenerationService makeGenerator() {
        return new CreativesGenerationService(
                stockMoviesService,
                videoPresetsService,
                videoAdditionsService,
                videoAdditionsRepository,
                stockVideoAdditionsRepository,
                videoCreativesService);
    }

    static final long FIRST_PRESET_ID = 1L;
    static final long SECOND_PRESET_ID = 2L;
    static final String AUDIO_ID = "mocked_audio";

    @Test
    public void checkGeneration() throws IOException, URISyntaxException {

        PresetDescription description1 = new PresetDescription();
        description1.setPresetId(FIRST_PRESET_ID);
        description1.setPresetTheme(PresetTheme.CAUCASUS);
        description1.setSkipOffset(12L);
        description1.setTags(Arrays.asList(PresetTag.CPM));

        PresetDescription description2 = new PresetDescription();
        description2.setPresetId(SECOND_PRESET_ID);
        description2.setPresetTheme(PresetTheme.EMPTY);
        description2.setSkipOffset(12L);
        description2.setTags(Arrays.asList(PresetTag.CPM));

        VideoPreset videoPreset1 = new VideoPreset(description1);
        VideoPreset videoPreset2 = new VideoPreset(description2);

        CreativesGenerationService creativesGenerationService = makeGenerator();

        Iterator<CreativesGenerationService.ConditionsCombination> iterator = creativesGenerationService
                .generateCombinationsIterator(Arrays.asList(videoPreset1, videoPreset2),
                        Arrays.asList(AUDIO_ID),
                        Arrays.asList("fVideo", "sVideo"),
                        Arrays.asList("ru_RU"), 8,
                        false);

        List<CreativesGenerationService.ConditionsCombination> result = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            result.add(iterator.next());
        }

        assertThat("No more elements", result, Matchers.hasSize(8));
        assertThat("No more elements", iterator.hasNext(), is(false));

        //TODO
        //check content
    }

}
