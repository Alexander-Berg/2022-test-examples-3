package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.permission;

import lombok.Data;

@Data
public class PermissionRequest {
    String userKey;
    String areaKey;
    String permissionType;
    int allowPiece = 0;
    int allowPallet = 0;
    int allowIps = 0;
    int allowCase = 0;
    final String description = "at_add_permission";

    public PermissionRequest(String userKey, String areaKey, String permissionType) {
        this.userKey = userKey;
        this.areaKey = areaKey;
        this.permissionType = permissionType;
    }

    public PermissionRequest(String userKey, String areaKey, String permissionType,
                             int allowPiece, int allowPallet, int allowIps, int allowCase) {
        this.userKey = userKey;
        this.areaKey = areaKey;
        this.permissionType = permissionType;
        this.allowPiece = allowPiece;
        this.allowPallet = allowPallet;
        this.allowIps = allowIps;
        this.allowCase = allowCase;
    }


}
