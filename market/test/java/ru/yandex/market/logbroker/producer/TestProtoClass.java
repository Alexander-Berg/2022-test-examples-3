package ru.yandex.market.logbroker.producer;

final class TestProtoClass {
    private TestProtoClass() {}
    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions(
                (com.google.protobuf.ExtensionRegistryLite) registry);
    }
    public interface DynamicLogEntryOrBuilder extends
            // @@protoc_insertion_point(interface_extends:ru.yandex.market.dynamic.DynamicLogEntry)
            com.google.protobuf.MessageOrBuilder {

        /**
         * <pre>
         *Id поколения динамика, к которому относится событие.
         * </pre>
         *
         * <code>int64 generationId = 1;</code>
         */
        long getGenerationId();

        /**
         * <pre>
         *Тип сущности для динамика, см DynamicGenerationStatus
         * </pre>
         *
         * <code>string entityType = 2;</code>
         */
        java.lang.String getEntityType();
        /**
         * <pre>
         *Тип сущности для динамика, см DynamicGenerationStatus
         * </pre>
         *
         * <code>string entityType = 2;</code>
         */
        com.google.protobuf.ByteString
        getEntityTypeBytes();

        /**
         * <pre>
         *Id сущности для динамика.
         * </pre>
         *
         * <code>string entityId = 3;</code>
         */
        java.lang.String getEntityId();
        /**
         * <pre>
         *Id сущности для динамика.
         * </pre>
         *
         * <code>string entityId = 3;</code>
         */
        com.google.protobuf.ByteString
        getEntityIdBytes();
    }
    /**
     * <pre>
     **
     *Запись лога для динамика типа entityType
     * </pre>
     *
     * Protobuf type {@code ru.yandex.market.dynamic.DynamicLogEntry}
     */
    public  static final class DynamicLogEntry extends
            com.google.protobuf.GeneratedMessageV3 implements
            // @@protoc_insertion_point(message_implements:ru.yandex.market.dynamic.DynamicLogEntry)
            DynamicLogEntryOrBuilder {
        private static final long serialVersionUID = 0L;
        // Use DynamicLogEntry.newBuilder() to construct.
        private DynamicLogEntry(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
            super(builder);
        }
        private DynamicLogEntry() {
            generationId_ = 0L;
            entityType_ = "";
            entityId_ = "";
        }

        @java.lang.Override
        public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
            return this.unknownFields;
        }
        private DynamicLogEntry(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            this();
            int mutable_bitField0_ = 0;
            com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                    com.google.protobuf.UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        default: {
                            if (!parseUnknownFieldProto3(
                                    input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                        case 8: {

                            generationId_ = input.readInt64();
                            break;
                        }
                        case 18: {
                            java.lang.String s = input.readStringRequireUtf8();

                            entityType_ = s;
                            break;
                        }
                        case 26: {
                            java.lang.String s = input.readStringRequireUtf8();

                            entityId_ = s;
                            break;
                        }
                    }
                }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(
                        e).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }
        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return TestProtoClass.internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor;
        }

        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return TestProtoClass.internal_static_ru_yandex_market_dynamic_DynamicLogEntry_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            TestProtoClass.DynamicLogEntry.class, TestProtoClass.DynamicLogEntry.Builder.class);
        }

        public static final int GENERATIONID_FIELD_NUMBER = 1;
        private long generationId_;
        /**
         * <pre>
         *Id поколения динамика, к которому относится событие.
         * </pre>
         *
         * <code>int64 generationId = 1;</code>
         */
        public long getGenerationId() {
            return generationId_;
        }

        public static final int ENTITYTYPE_FIELD_NUMBER = 2;
        private volatile java.lang.Object entityType_;
        /**
         * <pre>
         *Тип сущности для динамика, см DynamicGenerationStatus
         * </pre>
         *
         * <code>string entityType = 2;</code>
         */
        public java.lang.String getEntityType() {
            java.lang.Object ref = entityType_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                entityType_ = s;
                return s;
            }
        }
        /**
         * <pre>
         *Тип сущности для динамика, см DynamicGenerationStatus
         * </pre>
         *
         * <code>string entityType = 2;</code>
         */
        public com.google.protobuf.ByteString
        getEntityTypeBytes() {
            java.lang.Object ref = entityType_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                entityType_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int ENTITYID_FIELD_NUMBER = 3;
        private volatile java.lang.Object entityId_;
        /**
         * <pre>
         *Id сущности для динамика.
         * </pre>
         *
         * <code>string entityId = 3;</code>
         */
        public java.lang.String getEntityId() {
            java.lang.Object ref = entityId_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                entityId_ = s;
                return s;
            }
        }
        /**
         * <pre>
         *Id сущности для динамика.
         * </pre>
         *
         * <code>string entityId = 3;</code>
         */
        public com.google.protobuf.ByteString
        getEntityIdBytes() {
            java.lang.Object ref = entityId_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                entityId_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        private byte memoizedIsInitialized = -1;
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized == 1) return true;
            if (isInitialized == 0) return false;

            memoizedIsInitialized = 1;
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                throws java.io.IOException {
            if (generationId_ != 0L) {
                output.writeInt64(1, generationId_);
            }
            if (!getEntityTypeBytes().isEmpty()) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 2, entityType_);
            }
            if (!getEntityIdBytes().isEmpty()) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 3, entityId_);
            }
            unknownFields.writeTo(output);
        }

        public int getSerializedSize() {
            int size = memoizedSize;
            if (size != -1) return size;

            size = 0;
            if (generationId_ != 0L) {
                size += com.google.protobuf.CodedOutputStream
                        .computeInt64Size(1, generationId_);
            }
            if (!getEntityTypeBytes().isEmpty()) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, entityType_);
            }
            if (!getEntityIdBytes().isEmpty()) {
                size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, entityId_);
            }
            size += unknownFields.getSerializedSize();
            memoizedSize = size;
            return size;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof TestProtoClass.DynamicLogEntry)) {
                return super.equals(obj);
            }
            TestProtoClass.DynamicLogEntry other = (TestProtoClass.DynamicLogEntry) obj;

            boolean result = true;
            result = result && (getGenerationId()
                    == other.getGenerationId());
            result = result && getEntityType()
                    .equals(other.getEntityType());
            result = result && getEntityId()
                    .equals(other.getEntityId());
            result = result && unknownFields.equals(other.unknownFields);
            return result;
        }

        @java.lang.Override
        public int hashCode() {
            if (memoizedHashCode != 0) {
                return memoizedHashCode;
            }
            int hash = 41;
            hash = (19 * hash) + getDescriptor().hashCode();
            hash = (37 * hash) + GENERATIONID_FIELD_NUMBER;
            hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
                    getGenerationId());
            hash = (37 * hash) + ENTITYTYPE_FIELD_NUMBER;
            hash = (53 * hash) + getEntityType().hashCode();
            hash = (37 * hash) + ENTITYID_FIELD_NUMBER;
            hash = (53 * hash) + getEntityId().hashCode();
            hash = (29 * hash) + unknownFields.hashCode();
            memoizedHashCode = hash;
            return hash;
        }

        public static TestProtoClass.DynamicLogEntry parseFrom(
                java.nio.ByteBuffer data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                java.nio.ByteBuffer data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                com.google.protobuf.ByteString data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                com.google.protobuf.ByteString data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }
        public static TestProtoClass.DynamicLogEntry parseDelimitedFrom(java.io.InputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input);
        }
        public static TestProtoClass.DynamicLogEntry parseDelimitedFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                com.google.protobuf.CodedInputStream input)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input);
        }
        public static TestProtoClass.DynamicLogEntry parseFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3
                    .parseWithIOException(PARSER, input, extensionRegistry);
        }

        public Builder newBuilderForType() { return newBuilder(); }
        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }
        public static Builder newBuilder(TestProtoClass.DynamicLogEntry prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }
        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE
                    ? new Builder() : new Builder().mergeFrom(this);
        }

        @java.lang.Override
        protected Builder newBuilderForType(
                com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }
        /**
         * <pre>
         **
         *Запись лога для динамика типа entityType
         * </pre>
         *
         * Protobuf type {@code ru.yandex.market.dynamic.DynamicLogEntry}
         */
        public static final class Builder extends
                com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                // @@protoc_insertion_point(builder_implements:ru.yandex.market.dynamic.DynamicLogEntry)
                TestProtoClass.DynamicLogEntryOrBuilder {
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return TestProtoClass.internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor;
            }

            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return TestProtoClass.internal_static_ru_yandex_market_dynamic_DynamicLogEntry_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                TestProtoClass.DynamicLogEntry.class, TestProtoClass.DynamicLogEntry.Builder.class);
            }

            // Construct using TestProtoClass.DynamicLogEntry.newBuilder()
            private Builder() {
                maybeForceBuilderInitialization();
            }

            private Builder(
                    com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }
            private void maybeForceBuilderInitialization() {
                if (com.google.protobuf.GeneratedMessageV3
                        .alwaysUseFieldBuilders) {
                }
            }
            public Builder clear() {
                super.clear();
                generationId_ = 0L;

                entityType_ = "";

                entityId_ = "";

                return this;
            }

            public com.google.protobuf.Descriptors.Descriptor
            getDescriptorForType() {
                return TestProtoClass.internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor;
            }

            public TestProtoClass.DynamicLogEntry getDefaultInstanceForType() {
                return TestProtoClass.DynamicLogEntry.getDefaultInstance();
            }

            public TestProtoClass.DynamicLogEntry build() {
                TestProtoClass.DynamicLogEntry result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            public TestProtoClass.DynamicLogEntry buildPartial() {
                TestProtoClass.DynamicLogEntry result = new TestProtoClass.DynamicLogEntry(this);
                result.generationId_ = generationId_;
                result.entityType_ = entityType_;
                result.entityId_ = entityId_;
                onBuilt();
                return result;
            }

            public Builder clone() {
                return (Builder) super.clone();
            }
            public Builder setField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    java.lang.Object value) {
                return (Builder) super.setField(field, value);
            }
            public Builder clearField(
                    com.google.protobuf.Descriptors.FieldDescriptor field) {
                return (Builder) super.clearField(field);
            }
            public Builder clearOneof(
                    com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                return (Builder) super.clearOneof(oneof);
            }
            public Builder setRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    int index, java.lang.Object value) {
                return (Builder) super.setRepeatedField(field, index, value);
            }
            public Builder addRepeatedField(
                    com.google.protobuf.Descriptors.FieldDescriptor field,
                    java.lang.Object value) {
                return (Builder) super.addRepeatedField(field, value);
            }
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof TestProtoClass.DynamicLogEntry) {
                    return mergeFrom((TestProtoClass.DynamicLogEntry)other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(TestProtoClass.DynamicLogEntry other) {
                if (other == TestProtoClass.DynamicLogEntry.getDefaultInstance()) return this;
                if (other.getGenerationId() != 0L) {
                    setGenerationId(other.getGenerationId());
                }
                if (!other.getEntityType().isEmpty()) {
                    entityType_ = other.entityType_;
                    onChanged();
                }
                if (!other.getEntityId().isEmpty()) {
                    entityId_ = other.entityId_;
                    onChanged();
                }
                this.mergeUnknownFields(other.unknownFields);
                onChanged();
                return this;
            }

            public final boolean isInitialized() {
                return true;
            }

            public Builder mergeFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                TestProtoClass.DynamicLogEntry parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    parsedMessage = (TestProtoClass.DynamicLogEntry) e.getUnfinishedMessage();
                    throw e.unwrapIOException();
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }

            private long generationId_ ;
            /**
             * <pre>
             *Id поколения динамика, к которому относится событие.
             * </pre>
             *
             * <code>int64 generationId = 1;</code>
             */
            public long getGenerationId() {
                return generationId_;
            }
            /**
             * <pre>
             *Id поколения динамика, к которому относится событие.
             * </pre>
             *
             * <code>int64 generationId = 1;</code>
             */
            public Builder setGenerationId(long value) {

                generationId_ = value;
                onChanged();
                return this;
            }
            /**
             * <pre>
             *Id поколения динамика, к которому относится событие.
             * </pre>
             *
             * <code>int64 generationId = 1;</code>
             */
            public Builder clearGenerationId() {

                generationId_ = 0L;
                onChanged();
                return this;
            }

            private java.lang.Object entityType_ = "";
            /**
             * <pre>
             *Тип сущности для динамика, см DynamicGenerationStatus
             * </pre>
             *
             * <code>string entityType = 2;</code>
             */
            public java.lang.String getEntityType() {
                java.lang.Object ref = entityType_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    entityType_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <pre>
             *Тип сущности для динамика, см DynamicGenerationStatus
             * </pre>
             *
             * <code>string entityType = 2;</code>
             */
            public com.google.protobuf.ByteString
            getEntityTypeBytes() {
                java.lang.Object ref = entityType_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    entityType_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <pre>
             *Тип сущности для динамика, см DynamicGenerationStatus
             * </pre>
             *
             * <code>string entityType = 2;</code>
             */
            public Builder setEntityType(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                entityType_ = value;
                onChanged();
                return this;
            }
            /**
             * <pre>
             *Тип сущности для динамика, см DynamicGenerationStatus
             * </pre>
             *
             * <code>string entityType = 2;</code>
             */
            public Builder clearEntityType() {

                entityType_ = getDefaultInstance().getEntityType();
                onChanged();
                return this;
            }
            /**
             * <pre>
             *Тип сущности для динамика, см DynamicGenerationStatus
             * </pre>
             *
             * <code>string entityType = 2;</code>
             */
            public Builder setEntityTypeBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                entityType_ = value;
                onChanged();
                return this;
            }

            private java.lang.Object entityId_ = "";
            /**
             * <pre>
             *Id сущности для динамика.
             * </pre>
             *
             * <code>string entityId = 3;</code>
             */
            public java.lang.String getEntityId() {
                java.lang.Object ref = entityId_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    entityId_ = s;
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <pre>
             *Id сущности для динамика.
             * </pre>
             *
             * <code>string entityId = 3;</code>
             */
            public com.google.protobuf.ByteString
            getEntityIdBytes() {
                java.lang.Object ref = entityId_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    entityId_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <pre>
             *Id сущности для динамика.
             * </pre>
             *
             * <code>string entityId = 3;</code>
             */
            public Builder setEntityId(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }

                entityId_ = value;
                onChanged();
                return this;
            }
            /**
             * <pre>
             *Id сущности для динамика.
             * </pre>
             *
             * <code>string entityId = 3;</code>
             */
            public Builder clearEntityId() {

                entityId_ = getDefaultInstance().getEntityId();
                onChanged();
                return this;
            }
            /**
             * <pre>
             *Id сущности для динамика.
             * </pre>
             *
             * <code>string entityId = 3;</code>
             */
            public Builder setEntityIdBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);

                entityId_ = value;
                onChanged();
                return this;
            }
            public final Builder setUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.setUnknownFieldsProto3(unknownFields);
            }

            public final Builder mergeUnknownFields(
                    final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.mergeUnknownFields(unknownFields);
            }


            // @@protoc_insertion_point(builder_scope:ru.yandex.market.dynamic.DynamicLogEntry)
        }

        // @@protoc_insertion_point(class_scope:ru.yandex.market.dynamic.DynamicLogEntry)
        private static final TestProtoClass.DynamicLogEntry DEFAULT_INSTANCE;
        static {
            DEFAULT_INSTANCE = new TestProtoClass.DynamicLogEntry();
        }

        public static TestProtoClass.DynamicLogEntry getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        private static final com.google.protobuf.Parser<DynamicLogEntry>
                PARSER = new com.google.protobuf.AbstractParser<DynamicLogEntry>() {
            public DynamicLogEntry parsePartialFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return new DynamicLogEntry(input, extensionRegistry);
            }
        };

        public static com.google.protobuf.Parser<DynamicLogEntry> parser() {
            return PARSER;
        }

        @java.lang.Override
        public com.google.protobuf.Parser<DynamicLogEntry> getParserForType() {
            return PARSER;
        }

        public TestProtoClass.DynamicLogEntry getDefaultInstanceForType() {
            return DEFAULT_INSTANCE;
        }

    }

    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor;
    private static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_ru_yandex_market_dynamic_DynamicLogEntry_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }
    private static  com.google.protobuf.Descriptors.FileDescriptor
            descriptor;
    static {
        java.lang.String[] descriptorData = {
                "\n&market/mbi/proto/DynamicLogEntry.proto" +
                        "\022\030ru.yandex.market.dynamic\"M\n\017DynamicLog" +
                        "Entry\022\024\n\014generationId\030\001 \001(\003\022\022\n\nentityTyp" +
                        "e\030\002 \001(\t\022\020\n\010entityId\030\003 \001(\tb\006proto3"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                            com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[] {
                        }, assigner);
        internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_ru_yandex_market_dynamic_DynamicLogEntry_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_ru_yandex_market_dynamic_DynamicLogEntry_descriptor,
                new java.lang.String[] { "GenerationId", "EntityType", "EntityId", });
    }

    // @@protoc_insertion_point(outer_class_scope)
}

