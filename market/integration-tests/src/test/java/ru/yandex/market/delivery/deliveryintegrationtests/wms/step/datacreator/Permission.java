package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import io.qameta.allure.Step;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.permission.PermissionRequest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.permission.PermissionType;

public class Permission {
    private static final DatacreatorClient dataCreator = new DatacreatorClient();

    @Step("Выдаем пользователю {userKey} разрешение на отборы на участке {areaKey}")
    public String createPickingPermission(String userKey, String areaKey) {
        PermissionRequest reqBody = new PermissionRequest(
                userKey,
                areaKey,
                PermissionType.PICK.getValue(),
                1, 1, 1, 1);

        return dataCreator.createPermission(reqBody);
    }

    @Step("Выдаем пользователю {userKey} разрешение на сортировку на участке {areaKey}")
    public String createSortationPermission(String userKey, String areaKey) {
        return dataCreator.createPermission(
                new PermissionRequest(userKey, areaKey, PermissionType.SORTATION.getValue()));
    }

    @Step("Выдаем пользователю {userKey} разрешение на старую консолидацию на участке {areaKey}")
    public String createOldConsolidationPermission(String userKey, String areaKey) {
        return dataCreator.createPermission(
                new PermissionRequest(userKey, areaKey, PermissionType.OLD_CONSOLIDATION.getValue()));
    }

    @Step("Выдаем пользователю {userKey} разрешение консолидацию аномалий на участке {areaKey}")
    public String createAnomalyConsolidationPermission(String userKey, String areaKey) {
        return dataCreator.createPermission(
                new PermissionRequest(userKey, areaKey, PermissionType.ANOMALY_CONSOLIDATION.getValue()));
    }

    @Step("Выдаем пользователю {userKey} разрешение консолидацию аномалий на участке {areaKey}")
    public String createAnomalyPlacementPermission(String userKey, String areaKey) {
        return dataCreator.createPermission(
                new PermissionRequest(userKey, areaKey, PermissionType.ANOMALY_PLACEMENT.getValue()));
    }

    @Step("Удаляем разрешение у пользователя")
    public void deletePermission(String permissionSK) {
        if (permissionSK != null) dataCreator.deletePermission(permissionSK);
    }
}
