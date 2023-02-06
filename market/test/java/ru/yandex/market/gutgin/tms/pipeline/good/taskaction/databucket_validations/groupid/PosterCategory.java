package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid;

import Market.DataCamp.DataCampOffer;
import ru.yandex.market.extractor.ExtractorConfig;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

interface PosterCategory {
    long CATEGORY_ID = 17L;

    enum Person implements TestParamFactory.EnumValue {
        TRAVOLTA(3, "John Travolta"), TILDA(5, "Tilda Swinton");

        final int code;
        final String text;

        Person(int code, String text) {
            this.code = code;
            this.text = text;
        }

        @Override
        public int getCode() {
            return code;
        }
    }

    enum Size {
        FIVE(5), TEN(10);

        final int num;

        Size(int i) {
            num = i;
        }
    }

    MboParameters.Parameter NAME = MboParameters.Parameter.newBuilder()
            .setId(ParameterValueComposer.NAME_ID)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("name")
            .addName(MboParameters.Word.newBuilder()
                .setName("name")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .build();

    MboParameters.Parameter POSTER_THEME = MboParameters.Parameter.newBuilder()
            .setId(1001L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("poster_theme")
            .addName(MboParameters.Word.newBuilder()
                .setName("poster_theme")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .build();

    MboParameters.Parameter POSTER_FORMAT = MboParameters.Parameter.newBuilder()
            .setId(1003L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_NONE)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("poster_format")
            .addName(MboParameters.Word.newBuilder()
                .setName("poster_format")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .build();

    MboParameters.Parameter PERSON = MboParameters.Parameter.newBuilder()
            .setId(1002L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
            .setValueType(MboParameters.ValueType.ENUM)
            .setXslName("person")
            .addName(MboParameters.Word.newBuilder()
                .setName("person")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .setMultivalue(true)
            .addAllOption(Arrays.stream(Person.values())
                .map(p -> MboParameters.Option.newBuilder()
                    .setId(p.getCode())
                    .addName(MboParameters.Word.newBuilder()
                        .setName(p.text)
                        .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
                        .build())
                    .addAlias(MboParameters.EnumAlias.newBuilder()
                        .setAlias(MboParameters.Word.newBuilder().setName(p.text.toLowerCase()))
                        .build())
                    .build())
                .collect(Collectors.toList())
            )
            .build();

    MboParameters.Parameter TAG_LINE = MboParameters.Parameter.newBuilder()
            .setId(1005L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
            .setValueType(MboParameters.ValueType.STRING)
            .setMultivalue(true)
            .addName(MboParameters.Word.newBuilder()
                .setName("poster_title")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .setXslName("poster_title")
            .build();

    MboParameters.Parameter SIZE = MboParameters.Parameter.newBuilder()
            .setId(1007L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
            .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
            .addName(MboParameters.Word.newBuilder()
                .setName("poster_title")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .setXslName("size")
            .addAllOption(Arrays.stream(Size.values())
                    .map(s -> MboParameters.Option.newBuilder().addName(MboParameters.Word.newBuilder().setName(s.num + "").build()).build())
                    .collect(Collectors.toList())
            )
            .build();

    MboParameters.Parameter YEAR = MboParameters.Parameter.newBuilder()
            .setId(1017L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
            .setValueType(MboParameters.ValueType.NUMERIC)
            .setMultivalue(true)
            .addName(MboParameters.Word.newBuilder()
                .setName("year")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .setXslName("year")
            .build();

    MboParameters.Parameter FLICK_NAME = MboParameters.Parameter.newBuilder()
            .setId(1018L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
            .setValueType(MboParameters.ValueType.STRING)
            .setXslName("flick_name")
            .addName(MboParameters.Word.newBuilder()
                .setName("flick_name")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .build();

    MboParameters.Parameter ADULT = MboParameters.Parameter.newBuilder()
            .setId(1019L)
            .setSkuMode(MboParameters.SKUParameterMode.SKU_DEFINING)
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .addName(MboParameters.Word.newBuilder()
                .setName("poster_theme")
                .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE))
            .setXslName("poster_theme")
            .build();

    List<MboParameters.Parameter> ALL_PARAMS = Arrays.asList(
            NAME, POSTER_THEME, POSTER_FORMAT, PERSON, TAG_LINE, SIZE, YEAR, FLICK_NAME, ADULT
    );

    class Model {
        String name;
        String theme;
        String format;

        Model(String name, String theme, String format) {
            this.name = name;
            this.theme = theme;
            this.format = format;
        }

        protected <OBJ> void fillParams(TestParamFactory<OBJ> paramFactory, OBJ obj) {
            paramFactory.createStringParam(NAME.getId(), name, obj);
            paramFactory.createStringParam(POSTER_THEME.getId(), theme, obj);
            paramFactory.createStringParam(POSTER_FORMAT.getId(), format, obj);
        }
    }

    class Sku {
        final Model model;

        List<Object> persons;
        List<String> tagLines;
        Object size;
        int year;
        String flickName;
        boolean adult;

        Sku(Model model, List<Object> person, List<String> tagLines, Object size, int year, String flickName, boolean adult) {
            this.model = model;
            this.persons = person;
            this.tagLines = tagLines;
            this.size = size;
            this.year = year;
            this.flickName = flickName;
            this.adult = adult;
        }

        protected <OBJ> void fillParams(TestParamFactory<OBJ> paramFactory,
                                        OBJ obj) {

            model.fillParams(paramFactory, obj);
            for (Object p : persons) {
                if (p instanceof Person) {
                    Person person = (Person) p;
                    paramFactory.createEnumParam(PERSON.getId(), person, obj);
                } else {
                    paramFactory.createHypothesisParam(PERSON.getId(), (String) p, obj);
                }
            }
            tagLines.forEach(tl -> paramFactory.createStringParam(TAG_LINE.getId(), tl, obj));
            if (size instanceof Size) {
                Size sizeEnum = (Size) size;
                paramFactory.createNumericEnumParam(SIZE.getId(), sizeEnum.num, obj);
            } else {
                Integer sizeInt = (Integer) size;
                paramFactory.createHypothesisParam(SIZE.getId(), "" + sizeInt, obj);
            }

            paramFactory.createNumericParam(YEAR.getId(), year, obj);
            paramFactory.createStringParam(FLICK_NAME.getId(), flickName, obj);
            paramFactory.createBooleanParam(ADULT.getId(), adult, obj);
        }


        ModelStorage.Model createModelSku(long id) {
            ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
            builder.setId(id);
            fillParams(TestParamFactory.MODEL_STORAGE_PARAM_FACTORY, builder);
            return builder.build();
        }

        GcSkuTicket createTicket(long ticketId, Long skuId) {
            return createTicket(ticketId, skuId, DcpGroupIdValidationCheckerTest.GROUP_ID);
        }

        GcSkuTicket createTicket(long ticketId, Long skuId, Integer groupId) {
            DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
            fillParams(TestParamFactory.TICKET_PARAM_FACTORY, offerBuilder);

            if (skuId != null) {
                offerBuilder.getContentBuilder().getBindingBuilder().getApprovedBuilder().setMarketSkuId(skuId);
            }

            offerBuilder.getContentBuilder().getPartnerBuilder().getOriginalBuilder().getGroupNameBuilder()
                .setValue(String.valueOf(groupId));

            GcSkuTicket gcSkuTicket = new GcSkuTicket();
            gcSkuTicket.setPartnerShopId(11);
            gcSkuTicket.setDcpGroupId(groupId);
            gcSkuTicket.setCategoryId(CATEGORY_ID);

            gcSkuTicket.setDatacampOffer(offerBuilder.build());
            gcSkuTicket.setId(ticketId);
            gcSkuTicket.setShopSku("shop_sku_" + ticketId);
            if (skuId != null) {
                gcSkuTicket.setExistingMboPskuId(skuId);
            }
            return gcSkuTicket;
        }
    }
}
