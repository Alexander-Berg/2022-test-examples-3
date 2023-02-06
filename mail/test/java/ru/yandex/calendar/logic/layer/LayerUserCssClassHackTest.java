package ru.yandex.calendar.logic.layer;

import org.junit.Test;

import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.util.color.Color;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class LayerUserCssClassHackTest {

    @Test
    public void getBestMatchingCssClassByColor() {
        Tuple2List<String, Color> cssClassColors = Tuple2List.arrayList();

        cssClassColors.add("red", Color.parseRgba("#FF0000FF"));
        cssClassColors.add("green", Color.parseRgba("#00FF00FF"));
        cssClassColors.add("blue", Color.parseRgba("#00FFFFFF"));

        Assert.A.equals("red", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#FF0000FF"), cssClassColors));
        Assert.A.equals("red", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#F05050FF"), cssClassColors));

        Assert.A.equals("green", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#00FF00FF"), cssClassColors));
        Assert.A.equals("green", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#50F050FF"), cssClassColors));

        Assert.A.equals("blue", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#0000FFFF"), cssClassColors));
        Assert.A.equals("blue", LayerUserCssClassHack.getBestMatchingCssClassByColor(Color.parseRgba("#5050F0FF"), cssClassColors));
    }

}

