package ru.yandex.direct.core.testing.info;

import java.util.function.BiConsumer;
import java.util.function.Function;

import one.util.streamex.StreamEx;

import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsAvatarsHost;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsNamespace;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;

/**
 * На самом деле класс сейчас нигде не используется, он только пишется, но не читается.
 */
public class BannerImageFormat implements Model {
    public enum AvatarHost {
        PROD("avatars.mds.yandex.net"),
        TEST("avatars.mdst.yandex.net");

        AvatarHost(String host) {
            this.host = host;
        }

        private String host;

        public String getHost() {
            return host;
        }

        public static AvatarHost parseValue(String value) {
            return StreamEx.of(values())
                    .findFirst(t -> t.getHost().equals(value))
                    .orElseThrow(() -> new IllegalArgumentException("No enum constant " + value));
        }
    }

    public enum AvatarNamespace {
        DIRECT("direct"),
        DIRECT_PICTURE("direct-picture");

        private String value;

        AvatarNamespace(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AvatarNamespace parseValue(String value) {
            return StreamEx.of(values())
                    .filter(t -> t.getValue().equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No enum constant " + value));
        }
    }

    public enum ImageType {
        SMALL,
        REGULAR,
        WIDE,
        IMAGE_AD,
        LOGO
    }


    public static final ModelProperty<BannerImageFormat, String> IMAGE_HASH =
            prop("imageHash", BannerImageFormat::getImageHash, BannerImageFormat::setImageHash);
    public static final ModelProperty<BannerImageFormat, Long> MDS_GROUP_ID =
            prop("mdsGroupId", BannerImageFormat::getMdsGroupId, BannerImageFormat::setMdsGroupId);
    public static final ModelProperty<BannerImageFormat, AvatarNamespace> AVATAR_NAMESPACE =
            prop("avatarNamespace", BannerImageFormat::getAvatarNamespace, BannerImageFormat::setAvatarNamespace);
    public static final ModelProperty<BannerImageFormat, ImageType> IMAGE_TYPE =
            prop("imageType", BannerImageFormat::getImageType, BannerImageFormat::setImageType);
    public static final ModelProperty<BannerImageFormat, Long> WIDTH =
            prop("width", BannerImageFormat::getWidth, BannerImageFormat::setWidth);
    public static final ModelProperty<BannerImageFormat, Long> HEIGHT =
            prop("height", BannerImageFormat::getHeight, BannerImageFormat::setHeight);
    public static final ModelProperty<BannerImageFormat, String> FORMATS_JSON =
            prop("formatsJson", BannerImageFormat::getFormatsJson, BannerImageFormat::setFormatsJson);
    public static final ModelProperty<BannerImageFormat, String> MDS_META_JSON =
            prop("mdsMetaJson", BannerImageFormat::getMdsMetaJson, BannerImageFormat::setMdsMetaJson);
    public static final ModelProperty<BannerImageFormat, AvatarHost> AVATAR_HOST =
            prop("avatarHost", BannerImageFormat::getAvatarHost, BannerImageFormat::setAvatarHost);

    private String imageHash;
    private Long mdsGroupId;
    private AvatarNamespace avatarNamespace;
    private ImageType imageType;
    private Long width;
    private Long height;
    private String formatsJson;
    private String mdsMetaJson;
    private AvatarHost avatarHost;

    private static <V> ModelProperty<BannerImageFormat, V> prop(String name,
                                                                Function<BannerImageFormat, V> getter, BiConsumer<BannerImageFormat, V> setter) {
        return ModelProperty.create(BannerImageFormat.class, name, getter, setter);
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public Long getMdsGroupId() {
        return mdsGroupId;
    }

    public void setMdsGroupId(Long mdsGroupId) {
        this.mdsGroupId = mdsGroupId;
    }

    public AvatarNamespace getAvatarNamespace() {
        return avatarNamespace;
    }

    public void setAvatarNamespace(AvatarNamespace avatarNamespace) {
        this.avatarNamespace = avatarNamespace;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public Long getWidth() {
        return width;
    }

    public void setWidth(Long width) {
        this.width = width;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getFormatsJson() {
        return formatsJson;
    }

    public void setFormatsJson(String formatsJson) {
        this.formatsJson = formatsJson;
    }

    public String getMdsMetaJson() {
        return mdsMetaJson;
    }

    public void setMdsMetaJson(String mdsMetaJson) {
        this.mdsMetaJson = mdsMetaJson;
    }

    public AvatarHost getAvatarHost() {
        return avatarHost;
    }

    public void setAvatarHost(AvatarHost avatarHost) {
        this.avatarHost = avatarHost;
    }

    public BannerImageFormat withImageHash(String imageHash) {
        this.imageHash = imageHash;
        return this;
    }

    public BannerImageFormat withMdsGroupId(Long mdsGroupId) {
        this.mdsGroupId = mdsGroupId;
        return this;
    }

    public BannerImageFormat withAvatarNamespace(
            AvatarNamespace avatarNamespace) {
        this.avatarNamespace = avatarNamespace;
        return this;
    }

    public BannerImageFormat withImageType(ImageType imageType) {
        this.imageType = imageType;
        return this;
    }

    public BannerImageFormat withWidth(Long width) {
        this.width = width;
        return this;
    }

    public BannerImageFormat withHeight(Long height) {
        this.height = height;
        return this;
    }

    public BannerImageFormat withFormatsJson(String formatsJson) {
        this.formatsJson = formatsJson;
        return this;
    }

    public BannerImageFormat withMdsMetaJson(String mdsMetaJson) {
        this.mdsMetaJson = mdsMetaJson;
        return this;
    }

    public BannerImageFormat withAvatarHost(AvatarHost avatarHost) {
        this.avatarHost = avatarHost;
        return this;
    }


    public static AvatarHost avatarHostFromDb(BannerImagesFormatsAvatarsHost avatarsHost) {
        return avatarsHost == null ? null : AvatarHost.parseValue(avatarsHost.getLiteral());
    }

    public static BannerImagesFormatsAvatarsHost avatarHostToDb(AvatarHost avatarHost) {
        return avatarHost == null ? null
                : BannerImagesFormatsAvatarsHost.valueOf(avatarHost.getHost().replace(".", "_"));
    }

    public static AvatarNamespace avatarNamespaceFromDb(BannerImagesFormatsNamespace formatsNamespace) {
        return formatsNamespace == null ? null : AvatarNamespace.valueOf(formatsNamespace.getName().toUpperCase());
    }

    public static BannerImagesFormatsNamespace avatarNamespaceToDb(AvatarNamespace avatarNamespace) {
        return avatarNamespace == null ? null
                : BannerImagesFormatsNamespace.valueOf(avatarNamespace.name().toLowerCase());
    }

    public static ImageType imageTypeFromDb(BannerImagesFormatsImageType imageType) {
        return imageType == null ? null : ImageType.valueOf(imageType.name().toUpperCase());
    }

    public static BannerImagesFormatsImageType imageTypeToDb(ImageType imageType) {
        return imageType == null ? null : BannerImagesFormatsImageType.valueOf(imageType.name().toLowerCase());
    }
}
