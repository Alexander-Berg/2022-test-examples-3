package ru.yandex.canvas.service.direct;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.direct.CreativeUploadData;
import ru.yandex.canvas.model.video.Addition;
import ru.yandex.canvas.model.video.addition.AdditionData;
import ru.yandex.canvas.model.video.addition.options.AdditionElementOptions;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;


@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoAdditionDirectUploadHelperTest {
    @Autowired
    VideoAdditionDirectUploadHelper videoAdditionDirectUploadHelper;

    @Spy
    Addition additionSpy;

    @Mock
    AdditionElementOptions options;

    @Spy
    AdditionData data;

    public VideoAdditionDirectUploadHelperTest() {
        this.options = Mockito.spy(new AdditionElementOptions());
        this.data = new AdditionData();
        this.additionSpy = Mockito.spy(new Addition());
    }

    @Before
    public void beforeTests() {
        doReturn(true).when(additionSpy).isOverlayAddition();
        doReturn(options).when(additionSpy).findFilesOptions();
        doReturn((long) 5).when(additionSpy).getPresetId();
        doReturn(data).when(additionSpy).getData();
        doReturn(new ArrayList<>()).when(data).getElements();
        given(options.getVideoId()).willReturn(null);
    }

    @Test
    public void testSubstringNameIfItLongerThen255Symbols() {
        additionSpy.setName("a".repeat(300));
        CreativeUploadData creativeUploadData = videoAdditionDirectUploadHelper.toCreativeUploadData(additionSpy, 5);
        Assert.assertEquals(255,
                creativeUploadData.getCreativeName().length());
    }

    @Test
    public void testIfNameLessThen255Symbols() {
        additionSpy.setName("a".repeat(200));
        CreativeUploadData creativeUploadData = videoAdditionDirectUploadHelper.toCreativeUploadData(additionSpy, 5);
        Assert.assertEquals("a".repeat(200), creativeUploadData.getCreativeName());
    }
}
