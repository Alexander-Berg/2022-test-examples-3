package ru.yandex.autotests.direct.cmd.data.commons.banner;

import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.banners.MobileBannerPrimaryAction;
import ru.yandex.autotests.direct.cmd.data.banners.UsedResources;
import ru.yandex.autotests.direct.cmd.data.commons.BannerErrors;
import ru.yandex.autotests.direct.cmd.data.commons.BeforeModeration;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.GroupMultiplierStats;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.interest.RelevanceMatch;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;

/**
 * Отправляется в формате json в ручки:
 * saveDynamicAdGroups
 */
public class Banner {

    @SerializedName("modelId")
    private String modelId;

    @SerializedName("imf_mds_group_id")
    private Long imfMdsGroupId;

    @SerializedName("bid")
    private Long bid;

    @SerializedName("cid")
    private Long cid;

    @SerializedName("pid")
    private String pid;

    @SerializedName("phoneflag")
    private String phoneFlag;

    @SerializedName("archive")
    private String archive;

    @SerializedName("can_delete_banner")
    private Boolean canDeleteBanner;

    @SerializedName("can_archive_banner")
    private Boolean canArchiveBanner;

    @SerializedName("splited_phrases")
    private String splitedPhrases;

    @SerializedName("group_banners_types")
    private List<BannerTypeCount> groupBannersTypes;

    @SerializedName("hash_flags")
    private HashFlags hashFlags;

    @SerializedName("flags")
    private String flags;

    @SerializedName("title")
    private String title;

    @SerializedName("title_extension")
    private String titleExtension;

    @SerializedName("body")
    private String body;

    @SerializedName("banner_type")
    private String bannerType;

    @SerializedName("href_model")
    private HrefModel hrefModel;

    @SerializedName("url_protocol")
    private String urlProtocol;

    @SerializedName("href")
    private String href;

    @SerializedName("domain_sign")
    private String domainSign;

    @SerializedName("domain_redir")
    private String domainRedir;

    @SerializedName("domain_redir_sign")
    private String domainRedirSign;

    @SerializedName("has_vcard")
    private Integer hasVcard;

    @SerializedName("vcard_id")
    private Long vcardId;

    @SerializedName("vcard")
    private ContactInfo contactInfo;

    @SerializedName("sitelinks")
    private List<SiteLink> siteLinks;

    @SerializedName("turbolanding")
    private TurboLanding turboLanding;

    @SerializedName("callouts")
    private List<Callout> callouts;

    @SerializedName("disclaimer")
    private String disclaimer;

    @SerializedName("statusShow")
    private String statusShow;

    @SerializedName("statusModerate")
    private String statusModerate;

    @SerializedName("statusPostModerate")
    private String statusPostModerate;

    @SerializedName("status_moderate")
    private String status_moderate;

    @SerializedName("status_post_moderate")
    private String status_post_moderate;

    @SerializedName("isNewBanner")
    private Boolean isNewBanner;

    @SerializedName("image_model")
    private ImageModel imageModel;

    @SerializedName("ad_type")
    private String adType;

    @SerializedName("image")
    private String image;

    @SerializedName("image_type")
    private String imageType;

    @SerializedName("image_name")
    private String imageName;

    @SerializedName("source_image")
    private String imageSource;

    @SerializedName("image_source_url")
    private String imageSourceUrl;

    @SerializedName("image_statusModerate")
    private String imageStatusModerate;

    @SerializedName("im_statusModerate")
    private String imAdStatusModerate;

    @SerializedName("preview_scale")
    private Double previewScale;

    @SerializedName("width")
    private Integer width;

    @SerializedName("height")
    private Integer height;
    @SerializedName("name")
    private String name;
    @SerializedName("scale")
    private String scale;
    @SerializedName("preview_url")
    private String previewUrl;
    @SerializedName("image_ad")
    private ImageAd imageAd;

    @SerializedName("has_href")
    private Double hasHref;

    @SerializedName("is_vcard_open")
    private Double isVcardOpen;

    @SerializedName("is_vcard_collapsed")
    private Boolean isVcardCollapsed;

    @SerializedName("geo_names")
    private String geoNames;

    @SerializedName("child_age")
    private Double childAge;

    @SerializedName("domain")
    private String domain;

    @SerializedName("loadVCardFromClient")
    private Boolean loadVCardFromClient;

    @SerializedName("autobudget")
    private String autobudget;

    @SerializedName("status")
    private String status;

    @SerializedName("statusActive")
    private String statusActive;

    //эта шняга для showCamp, потому что там в поле banner возвращается на самом деле группа вот с таким вот полем
    @SerializedName("current_status_bids")
    List<Long> groupBids;

    @SerializedName("statusAutobudgetShow")
    private String statusAutobudgetShow;

    @SerializedName("groupStatusModerate")
    private String groupStatusModerate;

    @SerializedName("is_edited_by_moderator")
    private String isEditedByModerator;

    @SerializedName("day_budget")
    private String dayBudget;

    @SerializedName("showModEditNotice")
    private String showModEditNotice;

    @SerializedName("group_name")
    private String groupName;

    @SerializedName("canToggleAge")
    private Boolean canToggleAge;

    public Long getImfMdsGroupId() {
        return imfMdsGroupId;
    }

    public Banner withImfMdsGroupId(Long imfMdsGroupId) {
        this.imfMdsGroupId = imfMdsGroupId;
        return this;
    }

    @SerializedName("hasCopyFromPrev")
    private Boolean hasCopyFromPrev;

    @SerializedName("statusBsSynced")
    private String statusBsSynced;

    @SerializedName("display_href")
    private String displayHref;

    @SerializedName("display_href_statusModerate")
    private String displayHrefStatusModerate;

    /**
     * Для перформанс баннера
     */
    @SerializedName("status_bs_synced")
    private String status_bs_synced;

    @SerializedName("creative")
    private CreativeBanner creativeBanner; // также и для конструктора креативов

    /**
     * Для мобильного баннера
     */
    @SerializedName("reflected_attrs")
    private List<String> reflectedAttrs;

    @SerializedName("bannerID")
    private Long bannerID;

    @SerializedName("errors")
    private BannerErrors errors;

    @SerializedName("adgroup_id")
    private Long adGroupId;

    @SerializedName("performance_filters")
    private List<PerformanceFilter> performanceFilters;

    @SerializedName("dynamic_conditions")
    private List<DynamicCondition> dynamicConditions;

    @SerializedName("hasStoppedBanner")
    private String hasStoppedBanner;

    @SerializedName("phrases")
    private List<Phrase> phrases;

    @SerializedName("retargetings")
    private List<Retargeting> retargetings;

    @SerializedName("does_phrase_exceed_max_length")
    private String phraseExceedMaxLength;

    @SerializedName("does_phrase_exceed_max_words")
    private String phraseExceedMaxWordsCount;

    @SerializedName("group_multiplier_stats")
    private GroupMultiplierStats groupMultiplierStats;

    @SerializedName("group_has_callouts")
    private String groupHasCallouts;

    @SerializedName("before_moderation")
    private BeforeModeration beforeModeration;

    @SerializedName("primary_action")
    private MobileBannerPrimaryAction primaryAction;

    @SerializedName("target_interests")
    private List<TargetInterests> targetInterests;

    @SerializedName("relevance_match")
    private List<RelevanceMatch> relevanceMatch;

    @SerializedName("used_resources")
    private List<UsedResources> usedResources;

    @SerializedName("is_bs_rarely_loaded")
    private Integer isBsRarelyLoaded;

    @SerializedName("video_resources")
    private VideoAddition videoResources;

    @SerializedName("declined_show")
    private Integer declinedShow;

    @SerializedName("disabled_geo")
    private String disabledGeo;

    @SerializedName("effective_geo")
    private String effectiveGeo;

    @SerializedName("minus_geo")
    private String minusGeo;

    //Для cpm баннера
    @SerializedName("pixels")
    private List<Pixel> pixels;

    public Integer getDeclinedShow() {
        return declinedShow;
    }

    public void setDeclinedShow(Integer declinedShow) {
        this.declinedShow = declinedShow;
    }

    public Banner withDeclinedShow(Integer declinedShow) {
        setDeclinedShow(declinedShow);
        return this;
    }

    public VideoAddition getVideoResources() {
        return videoResources;
    }

    public void setVideoResources(VideoAddition videoResources) {
        this.videoResources = videoResources;
    }

    public Banner withVideoResources(VideoAddition videoResources) {
        this.videoResources = videoResources;
        return this;
    }

    public String getDisabledGeo() {
        return disabledGeo;
    }

    public Banner withDisabledGeo(String disabledGeo) {
        this.disabledGeo = disabledGeo;
        return this;
    }

    public String getEffectiveGeo() {
        return effectiveGeo;
    }

    public Banner withEffectiveGeo(String effectiveGeo) {
        this.effectiveGeo = effectiveGeo;
        return this;
    }

    public String getMinusGeo() {
        return minusGeo;
    }

    public Banner withMinusGeo(String minusGeo) {
        this.minusGeo = minusGeo;
        return this;
    }

    public Integer getIsBsRarelyLoaded() {
        return isBsRarelyLoaded;
    }

    public Banner withIsBsRarelyLoaded(Integer isBsRarelyLoaded) {
        this.isBsRarelyLoaded = isBsRarelyLoaded;
        return this;
    }

    public List<TargetInterests> getTargetInterests() {
        return targetInterests;
    }

    public Banner withTargetInterests(List<TargetInterests> targetInterests) {
        this.targetInterests = targetInterests;
        return this;
    }

    public List<RelevanceMatch> getRelevanceMatch() {
        return relevanceMatch;
    }

    public Banner withRelevanceMatch(List<RelevanceMatch> relevanceMatch) {
        this.relevanceMatch = relevanceMatch;
        return this;
    }

    public List<UsedResources> getUsedResources() {
        return usedResources;
    }

    public Banner withUsedResources(List<UsedResources> usedResources) {
        this.usedResources = usedResources;
        return this;
    }

    public String getModelId() {
        return modelId;
    }

    public Banner withModelId(String modelId) {
        this.modelId = modelId;
        return this;
    }

    public Long getBid() {
        return bid;
    }

    public Banner withBid(Long bid) {
        this.bid = bid;
        return this;
    }

    public String getPid() {
        return pid;
    }

    public Banner withPid(String pid) {
        this.pid = pid;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public Banner withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public String getArchive() {
        return archive;
    }

    public Banner withArchive(String archive) {
        this.archive = archive;
        return this;
    }

    public String getFlags() {
        return flags;
    }

    public Banner withFlags(String flags) {
        this.flags = flags;
        return this;
    }

    public Boolean getCanDeleteBanner() {
        return canDeleteBanner;
    }

    public Banner withCanDeleteBanner(Boolean canDeleteBanner) {
        this.canDeleteBanner = canDeleteBanner;
        return this;
    }

    public Boolean getCanArchiveBanner() {
        return canArchiveBanner;
    }

    public Banner withCanArchiveBanner(Boolean canArchiveBanner) {
        this.canArchiveBanner = canArchiveBanner;
        return this;
    }

    public String getSplitedPhrases() {
        return splitedPhrases;
    }

    public Banner withSplitedPhrases(String splitedPhrases) {
        this.splitedPhrases = splitedPhrases;
        return this;
    }

    public List<BannerTypeCount> getGroupBannersTypes() {
        return groupBannersTypes;
    }

    public Banner withGroupBannersTypes(List<BannerTypeCount> groupBannersTypes) {
        this.groupBannersTypes = groupBannersTypes;
        return this;
    }

    public Banner withHashFlags(HashFlags hashFlags) {
        this.hashFlags = hashFlags;
        return this;
    }

    public HashFlags getHashFlags() {
        return hashFlags;
    }

    public void setHashFlags(HashFlags hashFlags) {
        this.hashFlags = hashFlags;
    }

    public String getTitle() {
        return title;
    }

    public Banner withTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitleExtension() {
        return titleExtension;
    }

    public Banner withTitleExtension(String titleExtension) {
        this.titleExtension = titleExtension;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Banner withBody(String body) {
        this.body = body;
        return this;
    }

    public String getBannerType() {
        return bannerType;
    }

    public Banner withBannerType(String bannerType) {
        this.bannerType = bannerType;
        return this;
    }

    public HrefModel getHrefModel() {
        return hrefModel;
    }

    public Banner withHrefModel(HrefModel hrefModel) {
        this.hrefModel = hrefModel;
        return this;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public Banner withUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
        return this;
    }

    public String getHref() {
        return href;
    }

    public Banner withHref(String href) {
        this.href = href;
        return this;
    }

    public String getDomainSign() {
        return domainSign;
    }

    public Banner withDomainSign(String domainSign) {
        this.domainSign = domainSign;
        return this;
    }

    public String getDomainRedir() {
        return domainRedir;
    }

    public Banner withDomainRedir(String domainRedir) {
        this.domainRedir = domainRedir;
        return this;
    }

    public String getDomainRedirSign() {
        return domainRedirSign;
    }

    public Banner withDomainRedirSign(String domainRedirSign) {
        this.domainRedirSign = domainRedirSign;
        return this;
    }

    public Integer getHasVcard() {
        return hasVcard;
    }

    public Banner withHasVcard(Integer hasVcard) {
        this.hasVcard = hasVcard;
        return this;
    }

    public Long getVcardId() {
        return vcardId;
    }

    public Banner withVcardId(Long vcardId) {
        this.vcardId = vcardId;
        return this;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public Banner withContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
        return this;
    }

    public List<Callout> getCallouts() {
        return callouts;
    }

    public void setCallouts(List<Callout> callouts) {
        this.callouts = callouts;
    }

    public void setVcardId(Long vcardId) {
        this.vcardId = vcardId;
    }

    public Banner withCallouts(Callout... callouts) {
        return withCallouts(Arrays.asList(callouts));
    }

    public Banner withCallouts(List<Callout> callouts) {
        this.callouts = callouts;
        return this;
    }

    public List<SiteLink> getSiteLinks() {
        return siteLinks;
    }

    public Banner withSiteLinks(List<SiteLink> siteLinks) {
        this.siteLinks = siteLinks;
        return this;
    }

    public Banner withSiteLinks(SiteLink... siteLinks) {
        this.siteLinks = Arrays.asList(siteLinks);
        return this;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public Banner withStatusShow(String statusShow) {
        this.statusShow = statusShow;
        return this;
    }

    public String getStatusModerate() {
        return statusModerate;
    }

    public Banner withStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
        return this;
    }

    public String getStatusPostModerate() {
        return statusPostModerate;
    }

    public Banner withStatusPostModerate(String statusPostModerate) {
        this.statusPostModerate = statusPostModerate;
        return this;
    }

    public Boolean getNewBanner() {
        return isNewBanner;
    }

    public Banner withNewBanner(Boolean newBanner) {
        isNewBanner = newBanner;
        return this;
    }

    public ImageModel getImageModel() {
        return imageModel;
    }

    public Banner withImageModel(ImageModel imageModel) {
        this.imageModel = imageModel;
        return this;
    }

    public String getImage() {
        return image;
    }

    public Banner withImage(String image) {
        this.image = image;
        return this;
    }

    public String getImageType() {
        return imageType;
    }

    public Banner withImageType(String imageType) {
        this.imageType = imageType;
        return this;
    }

    public String getImageName() {
        return imageName;
    }

    public Banner withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public String getImageSource() {
        return imageSource;
    }

    public Banner withImageSource(String imageSource) {
        this.imageSource = imageSource;
        return this;
    }

    public String getImageSourceUrl() {
        return imageSourceUrl;
    }

    public Banner withImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
        return this;
    }

    public String getImageStatusModerate() {
        return imageStatusModerate;
    }

    public Banner withImageStatusModerate(String imageStatusModerate) {
        this.imageStatusModerate = imageStatusModerate;
        return this;
    }

    public String getImageAdStatusModerate() {
        return imAdStatusModerate;
    }

    public Banner withImageAdStatusModerate(String imStatusModerate) {
        this.imAdStatusModerate = imStatusModerate;
        return this;
    }

    public Double getHasHref() {
        return hasHref;
    }

    public Banner withHasHref(Double hasHref) {
        this.hasHref = hasHref;
        return this;
    }

    public Double getIsVcardOpen() {
        return isVcardOpen;
    }

    public Banner withIsVcardOpen(Double isVcardOpen) {
        this.isVcardOpen = isVcardOpen;
        return this;
    }

    public Boolean getVcardCollapsed() {
        return isVcardCollapsed;
    }

    public Banner withVcardCollapsed(Boolean vcardCollapsed) {
        isVcardCollapsed = vcardCollapsed;
        return this;
    }

    public String getGeoNames() {
        return geoNames;
    }

    public Banner withGeoNames(String geoNames) {
        this.geoNames = geoNames;
        return this;
    }

    public Double getChildAge() {
        return childAge;
    }

    public Banner withChildAge(Double childAge) {
        this.childAge = childAge;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public Banner withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public Boolean getLoadVCardFromClient() {
        return loadVCardFromClient;
    }

    public Banner withLoadVCardFromClient(Boolean loadVCardFromClient) {
        this.loadVCardFromClient = loadVCardFromClient;
        return this;
    }

    public String getAutobudget() {
        return autobudget;
    }

    public Banner withAutobudget(String autobudget) {
        this.autobudget = autobudget;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Banner withStatus(String status) {
        this.status = status;
        return this;
    }

    public String getStatusAutobudgetShow() {
        return statusAutobudgetShow;
    }

    public Banner withStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
        return this;
    }

    public String getGroupStatusModerate() {
        return groupStatusModerate;
    }

    public Banner withGroupStatusModerate(String groupStatusModerate) {
        this.groupStatusModerate = groupStatusModerate;
        return this;
    }

    public String getIsEditedByModerator() {
        return isEditedByModerator;
    }

    public Banner withIsEditedByModerator(String isEditedByModerator) {
        this.isEditedByModerator = isEditedByModerator;
        return this;
    }

    public String getDayBudget() {
        return dayBudget;
    }

    public Banner withDayBudget(String dayBudget) {
        this.dayBudget = dayBudget;
        return this;
    }

    public String getShowModEditNotice() {
        return showModEditNotice;
    }

    public Banner withShowModEditNotice(String showModEditNotice) {
        this.showModEditNotice = showModEditNotice;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public Banner withGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public Boolean getCanToggleAge() {
        return canToggleAge;
    }

    public Banner withCanToggleAge(Boolean canToggleAge) {
        this.canToggleAge = canToggleAge;
        return this;
    }

    public Boolean getHasCopyFromPrev() {
        return hasCopyFromPrev;
    }

    public Banner withHasCopyFromPrev(Boolean hasCopyFromPrev) {
        this.hasCopyFromPrev = hasCopyFromPrev;
        return this;
    }

    public Boolean getIsNewBanner() {
        return isNewBanner;
    }

    public Boolean getIsVcardCollapsed() {
        return isVcardCollapsed;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public void setStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
    }

    public Banner withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }

    public String getDisplayHref() {
        return displayHref;
    }

    public void setDisplayHref(String displayHref) {
        this.displayHref = displayHref;
    }

    public Banner withDisplayHref(String displayHref) {
        this.displayHref = displayHref;
        return this;
    }

    public String getDisplayHrefStatusModerate() {
        return displayHrefStatusModerate;
    }

    public void setDisplayHrefStatusModerate(String displayHrefStatusModerate) {
        this.displayHrefStatusModerate = displayHrefStatusModerate;
    }

    public Banner withDisplayHrefStatusModerate(String displayHrefStatusModerate) {
        this.displayHrefStatusModerate = displayHrefStatusModerate;
        return this;
    }

    public CreativeBanner getCreativeBanner() {
        return creativeBanner;
    }

    public Banner withCreativeBanner(CreativeBanner creativeBanner) {
        this.creativeBanner = creativeBanner;
        return this;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public void setCanDeleteBanner(Boolean canDeleteBanner) {
        this.canDeleteBanner = canDeleteBanner;
    }

    public void setCanArchiveBanner(Boolean canArchiveBanner) {
        this.canArchiveBanner = canArchiveBanner;
    }

    public void setSplitedPhrases(String splitedPhrases) {
        this.splitedPhrases = splitedPhrases;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleExtension(String titleExtension) {
        this.titleExtension = titleExtension;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setBannerType(String bannerType) {
        this.bannerType = bannerType;
    }

    public void setHrefModel(HrefModel hrefModel) {
        this.hrefModel = hrefModel;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setDomainSign(String domainSign) {
        this.domainSign = domainSign;
    }

    public void setDomainRedir(String domainRedir) {
        this.domainRedir = domainRedir;
    }

    public void setDomainRedirSign(String domainRedirSign) {
        this.domainRedirSign = domainRedirSign;
    }

    public void setHasVcard(Integer hasVcard) {
        this.hasVcard = hasVcard;
    }

    public void setContactInfo(ContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setSiteLinks(List<SiteLink> siteLinks) {
        this.siteLinks = siteLinks;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public void setStatusModerate(String statusModerate) {
        this.statusModerate = statusModerate;
    }

    public void setNewBanner(Boolean newBanner) {
        isNewBanner = newBanner;
    }

    public void setImageModel(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public Banner withAdType(String adType) {
        this.adType = adType;
        return this;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setImageSource(String imageSource) {
        this.imageSource = imageSource;
    }

    public void setImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
    }

    public void setImageStatusModerate(String imageStatusModerate) {
        this.imageStatusModerate = imageStatusModerate;
    }

    public void setImageAdStatusModerate(String imStatusModerate) {
        this.imAdStatusModerate = imStatusModerate;
    }

    public void setHasHref(Double hasHref) {
        this.hasHref = hasHref;
    }

    public void setIsVcardOpen(Double isVcardOpen) {
        this.isVcardOpen = isVcardOpen;
    }

    public void setVcardCollapsed(Boolean vcardCollapsed) {
        isVcardCollapsed = vcardCollapsed;
    }

    public void setGeoNames(String geoNames) {
        this.geoNames = geoNames;
    }

    public void setChildAge(Double childAge) {
        this.childAge = childAge;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setLoadVCardFromClient(Boolean loadVCardFromClient) {
        this.loadVCardFromClient = loadVCardFromClient;
    }

    public void setAutobudget(String autobudget) {
        this.autobudget = autobudget;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusAutobudgetShow(String statusAutobudgetShow) {
        this.statusAutobudgetShow = statusAutobudgetShow;
    }

    public void setGroupStatusModerate(String groupStatusModerate) {
        this.groupStatusModerate = groupStatusModerate;
    }

    public void setIsEditedByModerator(String isEditedByModerator) {
        this.isEditedByModerator = isEditedByModerator;
    }

    public void setDayBudget(String dayBudget) {
        this.dayBudget = dayBudget;
    }

    public void setShowModEditNotice(String showModEditNotice) {
        this.showModEditNotice = showModEditNotice;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setCanToggleAge(Boolean canToggleAge) {
        this.canToggleAge = canToggleAge;
    }

    public void setHasCopyFromPrev(Boolean hasCopyFromPrev) {
        this.hasCopyFromPrev = hasCopyFromPrev;
    }

    public void setCreativeBanner(CreativeBanner creativeBanner) {
        this.creativeBanner = creativeBanner;
    }

    public void setReflectedAttrs(List<String> reflectedAttrs) {
        this.reflectedAttrs = reflectedAttrs;
    }

    public List<String> getReflectedAttrs() {
        return reflectedAttrs;
    }

    public Banner withReflectedAttrs(List<String> reflectedAttrs) {
        this.reflectedAttrs = reflectedAttrs;
        return this;
    }

    public String getStatus_moderate() {
        return status_moderate;
    }

    public Banner withStatus_moderate(String status_moderate) {
        this.status_moderate = status_moderate;
        return this;
    }

    public String getStatus_post_moderate() {
        return status_post_moderate;
    }

    public Banner withStatus_post_moderate(String status_post_moderate) {
        this.status_post_moderate = status_post_moderate;
        return this;
    }

    public String getStatus_bs_synced() {
        return status_bs_synced;
    }

    public Banner withStatus_bs_synced(String status_bs_synced) {
        this.status_bs_synced = status_bs_synced;
        return this;
    }

    public Double getPreviewScale() {
        return previewScale;
    }

    public Banner withPreviewScale(Double previewScale) {
        this.previewScale = previewScale;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public Banner withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public Integer getHeight() {
        return height;
    }

    public Banner withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public String getName() {
        return name;
    }

    public Banner withName(String name) {
        this.name = name;
        return this;
    }

    public String getScale() {
        return scale;
    }

    public Banner withScale(String scale) {
        this.scale = scale;
        return this;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public Banner withPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
        return this;
    }

    public ImageAd getImageAd() {
        return imageAd;
    }

    public Banner withImageAd(ImageAd imageAd) {
        this.imageAd = imageAd;
        return this;
    }

    public Long getBannerID() {
        return bannerID;
    }

    public Banner withBannerID(Long bannerID) {
        this.bannerID = bannerID;
        return this;
    }

    public BannerErrors getErrors() {
        return errors;
    }

    public Banner withErrors(BannerErrors errors) {
        this.errors = errors;
        return this;
    }

    public Long getAdGroupId() {
        return adGroupId;
    }

    public Banner withAdGroupId(Long adGroupId) {
        this.adGroupId = adGroupId;
        return this;
    }

    public List<PerformanceFilter> getPerformanceFilters() {
        return performanceFilters;
    }

    public Banner withPerformanceFilters(List<PerformanceFilter> performanceFilters) {
        this.performanceFilters = performanceFilters;
        return this;
    }

    public List<DynamicCondition> getDynamicConditions() {
        return dynamicConditions;
    }

    public Banner withDynamicConditions(List<DynamicCondition> dynamicConditions) {
        this.dynamicConditions = dynamicConditions;
        return this;
    }

    public String getHasStoppedBanner() {
        return hasStoppedBanner;
    }

    public Banner withHasStoppedBanner(String hasStoppedBanner) {
        this.hasStoppedBanner = hasStoppedBanner;
        return this;
    }

    public List<Phrase> getPhrases() {
        return phrases;
    }

    public Banner withPhrases(List<Phrase> phrases) {
        this.phrases = phrases;
        return this;
    }

    public List<Retargeting> getRetargetings() {
        return retargetings;
    }

    public Banner withRetargetings(List<Retargeting> retargetings) {
        this.retargetings = retargetings;
        return this;
    }

    public String getPhraseExceedMaxLength() {
        return phraseExceedMaxLength;
    }

    public Banner withPhraseExceedMaxLength(String phraseExceedMaxLength) {
        this.phraseExceedMaxLength = phraseExceedMaxLength;
        return this;
    }

    public String getPhraseExceedMaxWordsCount() {
        return phraseExceedMaxWordsCount;
    }

    public Banner withPhraseExceedMaxWordsCount(String phraseExceedMaxWordsCount) {
        this.phraseExceedMaxWordsCount = phraseExceedMaxWordsCount;
        return this;
    }

    public GroupMultiplierStats getGroupMultiplierStats() {
        return groupMultiplierStats;
    }

    public Banner withGroupMultiplierStats(GroupMultiplierStats groupMultiplierStats) {
        this.groupMultiplierStats = groupMultiplierStats;
        return this;
    }

    public String getGroupHasCallouts() {
        return groupHasCallouts;
    }

    public Banner withGroupHasCallouts(String groupHasCallouts) {
        this.groupHasCallouts = groupHasCallouts;
        return this;
    }

    public BeforeModeration getBeforeModeration() {
        return beforeModeration;
    }

    public Banner withBeforeModeration(BeforeModeration beforeModeration) {
        this.beforeModeration = beforeModeration;
        return this;
    }

    public List<Long> getGroupBids() {
        return groupBids;
    }

    public Banner withGroupBids(List<Long> groupBids) {
        this.groupBids = groupBids;
        return this;
    }

    public Banner withUploadedImageFromFile(AjaxResizeBannerImageResponse resizeResponse) {
        this.image = resizeResponse.getImage();
        this.imageName = resizeResponse.getName();
        this.imageSource = ImageSourceType.FILE.toString();
        this.imageSourceUrl = "";
        this.imageModel = new ImageModel().
                withImage(this.image).
                withImageName(this.imageName).
                withImageSource(this.imageSource).
                withImageSourceUrl(this.imageSourceUrl);
        return this;
    }

    public Banner withUploadedImageFromUrl(AjaxResizeBannerImageResponse response, String imageSourceUrl) {
        this.image = response.getImage();
        this.imageName = response.getName();
        this.imageSource = ImageSourceType.URL.toString();
        this.imageSourceUrl = imageSourceUrl;
        this.imageModel = new ImageModel().
                withImage(this.image).
                withImageName(this.imageName).
                withImageSource(this.imageSource).
                withImageSourceUrl(this.imageSourceUrl);
        return this;
    }

    public String getPhoneFlag() {
        return phoneFlag;
    }

    public void setPhoneFlag(String phoneFlag) {
        this.phoneFlag = phoneFlag;
    }

    public Banner withPhoneFlag(String phoneFlag) {
        this.phoneFlag = phoneFlag;
        return this;
    }

    public MobileBannerPrimaryAction getPrimaryAction() {
        return primaryAction;
    }

    public Banner withPrimaryAction(MobileBannerPrimaryAction primaryAction) {
        this.primaryAction = primaryAction;
        return this;
    }

    public void setPrimaryAction(MobileBannerPrimaryAction primaryAction) {
        this.primaryAction = primaryAction;
    }

    public String getStatusActive() {
        return statusActive;
    }

    public Banner withStatusActive(String statusActive) {
        this.statusActive = statusActive;
        return this;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public Banner withDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
        return this;
    }

    public Banner addDefaultVideoAddition(Long creative_id) {
        this.setVideoResources(
                VideoAddition.getDefaultVideoAddition(creative_id)
        );
        return this;
    }

    public Banner removeVideoAddition() {
        this.setVideoResources(
                new VideoAddition()
        );
        return this;
    }

    public TurboLanding getTurboLanding() {
        return this.turboLanding;
    }

    public void setTurboLanding(TurboLanding turboLanding) {
        this.turboLanding = turboLanding;
    }

    public Banner withTurboLanding(TurboLanding turboLanding) {
        this.turboLanding = turboLanding;
        return this;
    }

    public List<Pixel> getPixels() {
        return pixels;
    }

    public void setPixels(List<Pixel> pixels) {
        this.pixels = pixels;
    }

    public Banner withPixels(List<Pixel> pixels) {
        this.pixels = pixels;
        return this;
    }

}
