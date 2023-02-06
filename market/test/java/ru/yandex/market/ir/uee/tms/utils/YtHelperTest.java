package ru.yandex.market.ir.uee.tms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.uee.model.ResourceLink;
import ru.yandex.market.ir.uee.model.ResourceStatus;
import ru.yandex.market.ir.uee.model.UserRunDataType;
import ru.yandex.market.ir.uee.tms.pojos.ResourcePojo;
import ru.yandex.misc.test.Assert;

public class YtHelperTest {

    @Test
    public void getDirectoriesForDelete() {
        YtHelper ytHelper = new YtHelper(null, null, null, "market/development/ir/uee");

        List<ResourcePojo> resourcePojos = new ArrayList<>(3);
        ResourceLink resourceLink1 = new ResourceLink();
        resourceLink1.setYtPath("//home/market/development/ir/uee/1/ADD_ROW_NUM_TO_INPUT_EXTERNAL_TASK/1.1635212946");
        ResourcePojo resourcePojo1 = new ResourcePojo(1, 1,
                UserRunDataType.YT,
                ResourceStatus.UPLOADED,
                resourceLink1);
        resourcePojos.add(resourcePojo1);
        ResourceLink resourceLink2 = new ResourceLink();
        resourceLink2.setYtPath("//home/market/development/ir/uee/1/ADD_ROW_NUM_TO_INPUT_EXTERNAL_TASK/2.1635212946");
        ResourcePojo resourcePojo2 = new ResourcePojo(2, 1,
                UserRunDataType.YT,
                ResourceStatus.UPLOADED,
                resourceLink2);
        resourcePojos.add(resourcePojo2);
        ResourceLink resourceLink3 = new ResourceLink();
        resourceLink3.setYtPath("//home/market/development/ir/uee/2/ADD_ROW_NUM_TO_INPUT_EXTERNAL_TASK/3.1635212946");
        ResourcePojo resourcePojo3 = new ResourcePojo(3, 2,
                UserRunDataType.YT,
                ResourceStatus.UPLOADED,
                resourceLink3);
        resourcePojos.add(resourcePojo3);

        Map<Integer, YPath> directoriesForDelete = ytHelper.getDirectoriesForDelete(resourcePojos);
        Assert.equals(directoriesForDelete.size(), 2);
        Assert.equals(directoriesForDelete.get(1).toString(), "//home/market/development/ir/uee/1");
        Assert.equals(directoriesForDelete.get(2).toString(), "//home/market/development/ir/uee/2");
    }
}
