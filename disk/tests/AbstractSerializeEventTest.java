package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.eventloader.EventLoaderLogListener;
import ru.yandex.chemodan.disksearch.indexing.IndexDocument;
import ru.yandex.chemodan.disksearch.indexing.IndexRequest;
import ru.yandex.chemodan.eventlog.events.AbstractEvent;
import ru.yandex.chemodan.eventlog.events.CompoundResourceType;
import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.MediaType;
import ru.yandex.chemodan.eventlog.events.MpfsAddress;
import ru.yandex.chemodan.eventlog.events.MpfsPath;
import ru.yandex.chemodan.eventlog.events.Resource;
import ru.yandex.chemodan.eventlog.events.ResourceLocation;
import ru.yandex.chemodan.eventlog.events.UniverseInvite;
import ru.yandex.chemodan.eventlog.events.YandexCloudRequestId;
import ru.yandex.chemodan.eventlog.events.album.Album;
import ru.yandex.chemodan.eventlog.events.album.AlbumItem;
import ru.yandex.chemodan.eventlog.events.billing.Payment;
import ru.yandex.chemodan.eventlog.events.billing.Product;
import ru.yandex.chemodan.eventlog.events.comment.CommentRef;
import ru.yandex.chemodan.eventlog.events.comment.EntityRef;
import ru.yandex.chemodan.eventlog.events.comment.ParentCommentRef;
import ru.yandex.chemodan.eventlog.events.fs.StoreTypeSubtype;
import ru.yandex.chemodan.eventlog.events.sharing.ShareData;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRights;
import ru.yandex.chemodan.eventlog.events.sharing.ShareRightsChange;
import ru.yandex.chemodan.eventlog.events.sharing.ShareUserType;
import ru.yandex.chemodan.mpfs.MpfsUid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.enums.EnumUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author Dmitriy Amelin (lemeh)
 */
abstract class AbstractSerializeEventTest {
    protected static final MpfsUid UID = new MpfsUid(1L);

    static final EventMetadata METADATA =
            new EventMetadata(UID, Instant.now(), YandexCloudRequestId.parse("mac-1234567-localhost"));

    static class ExpectedJson {
        final Tuple2List<String, Object> values;

        final Option<String> groupKey;

        final Option<EventType> eventType;

        final Option<MpfsUid> checkUid;

        final int checkIndex;

        ExpectedJson() {
            this(Cf.Tuple2List.cons(), Option.empty(), Option.empty(), Option.empty(), 0);
        }

        private ExpectedJson(Tuple2List<String, Object> values, Option<String> groupKey, Option<EventType> eventType,
                Option<MpfsUid> checkUid, int checkIndex)
        {
            this.values = values;
            this.groupKey = groupKey;
            this.eventType = eventType;
            this.checkUid = checkUid;
            this.checkIndex = checkIndex;
        }

        ExpectedJson withGroupKey(Object... chunks) {
            return withGroupKey(StringUtils.join(chunks, '|'));
        }

        ExpectedJson withGroupKey(String groupKey) {
            return new ExpectedJson(this.values, Option.of(groupKey), this.eventType, this.checkUid, this.checkIndex);
        }

        ExpectedJson withEventType(EventType eventType) {
            return new ExpectedJson(this.values, this.groupKey, Option.of(eventType), this.checkUid, this.checkIndex);
        }

        private ExpectedJson withAddress(String keyPrefix, MpfsAddress address) {
            return this.withAddress(keyPrefix, address.path);
        }

        private ExpectedJson withAddress(String keyPrefix, MpfsPath path) {
            return this
                    .with(keyPrefix + "_folder", path.getParent().toString())
                    .with(keyPrefix + "_path", path.toString());
        }

        ExpectedJson withOwner(MpfsUid uid) {
            return this.with("owner_uid", uid);
        }

        ExpectedJson withUser(MpfsUid uid) {
            return this.with("user_uid", uid);
        }

        ExpectedJson withUserType(ShareUserType userType) {
            return this.withEnum("user_type", userType);
        }

        ExpectedJson withPerformer(MpfsUid uid) {
            return withUser(uid);
        }

        ExpectedJson withInvite(UniverseInvite invite) {
            if (invite.isEmpty()) {
                return this;
            }

            return this
                    .with("invite_login", invite.getLogin())
                    .with("invite_service", invite.getService());
        }

        ExpectedJson withSource(ResourceLocation source) {
            return withAddress("source", source.address);
        }

        ExpectedJson withTarget(ResourceLocation target) {
            return withTarget(target.address);
        }

        ExpectedJson withSource(MpfsAddress source) {
            return withAddress("source", source);
        }

        ExpectedJson withTarget(MpfsAddress target) {
            return withAddress("target", target);
        }

        ExpectedJson withResource(Resource resource) {
            return this
                    .withCompoundType(resource.compoundType)
                    .with("resource_file_id", resource.fileId)
                    .withO("resource_public_key", resource.publicKey)
                    .withO("resource_short_url", resource.shortUrl)
                    .withO("resource_overwritten", resource.overwritten)
                    .withOwner(resource.owner);
        }

        private ExpectedJson withCompoundType(CompoundResourceType compoundType) {
            return this
                    .withEnum("resource_type", compoundType.type)
                    .withMediaType(compoundType.mediaType);
        }

        private ExpectedJson withMediaType(Option<MediaType> type) {
            return type.isPresent() ? this.with("resource_media_type", type.get().value) : this;
        }

        ExpectedJson withTypeSubtype(StoreTypeSubtype types) {
            if (types == StoreTypeSubtype.NONE) {
                return this;
            }

            return this
                    .with("store_type", types.getType())
                    .with("store_subtype", types.getSubtype());

        }

        ExpectedJson withAlbum(Album album) {
            return this
                    .with("album_id", album.id)
                    .with("album_title", album.title);
        }

        ExpectedJson withAlbumItem(AlbumItem albumItem) {
            return this
                    .withAlbum(albumItem.album)
                    .with("item_id", albumItem.itemId);
        }

        ExpectedJson withProduct(Product product) {
            return this
                    .withProductId(product.id)
                    .withO("product_period", product.period)
                    .with(product.names.namesMap);
        }

        ExpectedJson withProductId(String productId) {
            return this.with("product_id", productId);
        }

        ExpectedJson withPayment(Payment payment) {
            return this
                    .with("currency", payment.currency)
                    .with("price", payment.price);
        }

        ExpectedJson withShareData(ShareData shareData) {
            return this
                    .withOwner(shareData.owner)
                    .withTarget(shareData.target);
        }

        ExpectedJson withShareRightsChange(ShareRightsChange rightsChange) {
            return this
                    .withShareRights("rights", rightsChange.rights.get())
                    .withShareRights("prev_rights", rightsChange.prevRights.get());
        }

        ExpectedJson withShareRights(String key, ShareRights rights) {
            return this.with(key, rights.value());
        }

        ExpectedJson withProviderId(String providerId) {
            return with("invite_service", providerId);
        }

        ExpectedJson withEntityRef(EntityRef entity) {
            return this
                    .with("entity_type", entity.type)
                    .with("entity_id", entity.id)
                    .withO("resource_name", entity.resourceName)
                    .withO("resource_type", entity.resourceType.map(EnumUtils::toXmlName));
        }

        ExpectedJson withCommentRef(CommentRef comment) {
            return this
                    .withO("comment_id", comment.commentId)
                    .withO("comment_author_uid", comment.commentAuthorUid)
                    .withO("comment_text", comment.commentText);
        }

        ExpectedJson withParentCommentRef(ParentCommentRef comment) {
            return this
                    .withO("parent_comment_id", comment.parentCommentId)
                    .withO("parent_author_uid", comment.parentAuthorUid);
        }

        private ExpectedJson withEnum(String key, Enum<?> value) {
            return this.with(key, EnumUtils.toXmlName(value));
        }

        ExpectedJson with(MapF<?, ?> map) {
            ExpectedJson result = this;
            for(MapF.Entry<?, ?> entry : map.entrySet()) {
                result = result.with(entry.getKey().toString(), entry.getValue());
            }
            return result;
        }

        ExpectedJson with(String key, Object value) {
            return new ExpectedJson(values.plus1(key, value), groupKey, eventType, checkUid, checkIndex);
        }

        ExpectedJson withO(String key, Option<?> value) {
            return value.isPresent() ? this.with(key, value.get()) : this;
        }

        ExpectedJson plus(ExpectedJson expectedJson) {
            return new ExpectedJson(this.values.plus(expectedJson.values), groupKey, eventType, checkUid, checkIndex);
        }

        ExpectedJson forUid(MpfsUid uid) {
            return new ExpectedJson(values, groupKey, eventType, Option.of(uid), checkIndex);
        }

        ExpectedJson atIndex(int index) {
            return new ExpectedJson(values, groupKey, eventType, checkUid, index);
        }

        IndexDocument toIndexDocument() {
            return new IndexDocument(values.toMap().mapValues(Object::toString));
        }

        void serializeAndCheck(AbstractEvent event) {
            serializeAndCheck(event, checkUid.getOrElse(event.getUid()), checkIndex);
        }

        private void serializeAndCheck(AbstractEvent baseEvent, MpfsUid uid, int eventIndex) {
            ListF<AbstractEvent> events = baseEvent.getEvents();
            MapF<PassportUid, ListF<AbstractEvent>> eventsGroupedByUid =
                    events.groupBy(AbstractEvent::getPassportUid);
            if (!eventsGroupedByUid.containsKeyTs(uid.getUid())) {
                failForUid("No event created", uid);
            }

            if (eventIndex >= eventsGroupedByUid.getTs(uid.getUid()).length()) {
                failForUidAndIndex("No event created", uid, eventIndex);
            }

            AbstractEvent event = eventsGroupedByUid.getTs(uid.getUid()).get(eventIndex);
            EventType eventType = this.eventType.getOrElse(event.getEventType());
            IndexDocument expectedDocument = new ExpectedJson()
                    .withEnum("event_type", eventType)
                    .withEnum("event_class", eventType.category)
                    .with("event_timestamp", event.metadata.commitTime.getMillis())
                    .with("version", event.metadata.version.getMillis())
                    .with("platform", "mac")
                    .with("id", event.keys.getId())
                    .with("group_key", groupKey.getOrElse(event.keys.getGroupKey()))
                    .plus(this)
                    .toIndexDocument()
                    ;

            MapF<MpfsUid, IndexRequest> requests = EventLoaderLogListener.indexRequestBuilder
                    .buildUnsafe(events)
                    .toMapMappingToKey(r -> new MpfsUid(r.uid));
            if (!requests.containsKeyTs(uid)) {
                failForUid("No data serialized", uid);
            }

            IndexRequest request = requests.getTs(uid);
            if (eventIndex >= request.documents.length()) {
                failForUidAndIndex("No data serialized", uid, eventIndex);
            }

            Assert.equals(expectedDocument.attributes, request.getDocumentAt(eventIndex).attributes);
        }

        private void failForUid(String messagePrefix, MpfsUid uid) {
            failForUidAndIndex(messagePrefix, uid, Option.empty());
        }

        private void failForUidAndIndex(String messagePrefix, MpfsUid uid, int index) {
            failForUidAndIndex(messagePrefix, uid, Option.of(index));
        }

        private void failForUidAndIndex(String messagePrefix, MpfsUid uid, Option<Integer> index) {
            Assert.fail(messagePrefix + " for UID=" + uid + (index.isPresent() ? " at index=" + index.get() : ""));
        }
    }
}
