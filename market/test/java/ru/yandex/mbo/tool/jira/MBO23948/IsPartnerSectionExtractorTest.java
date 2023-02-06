package ru.yandex.mbo.tool.jira.MBO23948;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dergachevfv
 * @since 3/5/20
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:methodlength", "checkstyle:lineLenght"})
public class IsPartnerSectionExtractorTest {

    private static final String CONTENT_CASE_ONE = getContentCaseOne();
    private static final String CONTENT_CASE_TWO = getContentCaseTwo();
    private static final String CONTENT_CASE_THREE = getContentCaseThree();

    @Test
    public void shouldExtractAllIsPartnerSectionsCaseOne() {
        List<String> isPartnerSections = IsPartnerSectionExtractor.extract(CONTENT_CASE_ONE);
        assertThat(isPartnerSections).hasSize(1);
        assertThat(isPartnerSections).containsExactly(
            "\n{type#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Тип]]></name>\n" +
                "  <value><![CDATA[{type}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "\n" +
                "{box_type#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Тип переноски]]></name>\n" +
                "  <value><![CDATA[{box_type}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n"
        );
    }

    @Test
    public void shouldExtractAllIsPartnerSectionsCaseTwo() {
        List<String> isPartnerSections = IsPartnerSectionExtractor.extract(CONTENT_CASE_TWO);
        assertThat(isPartnerSections).hasSize(3);
        assertThat(isPartnerSections).containsExactly(
            "\n{EngineType#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Тип двигателя]]></name>\n" +
                "  <value><![CDATA[{EngineType}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "{NumOfStrokes#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Количество тактов двигателя]]></name>\n" +
                "  <value><![CDATA[{NumOfStrokes}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "\n",
            "\n{NominalPowerKW#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Мощность двигателя]]></name>\n" +
                "  <value><![CDATA[{NominalPowerKW} кВт]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "{EngineHP#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Мощность двигателя, л.с.]]></name>\n" +
                "  <value><![CDATA[{EngineHP}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "{NominalPowerRPM#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Номинальная мощность, об/мин]]></name>\n" +
                "  <value><![CDATA[{NominalPowerRPM} об/мин]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n",
            "\n{EngineType#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "  <name><![CDATA[Тип двигателя]]></name>\n" +
                "  <value><![CDATA[{EngineType}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n"
        );
    }

    @Test
    public void shouldExtractAllIsPartnerSectionsCaseThree() {
        List<String> isPartnerSections = IsPartnerSectionExtractor.extract(CONTENT_CASE_THREE);
        assertThat(isPartnerSections).hasSize(3);
        assertThat(isPartnerSections).containsExactly(
            "\n" +
                "    \n" +
                "{construction#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "<name><![CDATA[первая строка]]></name>\n" +
                "<value><![CDATA[{construction} бассейн]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "\n",
            "\n" +
                "\n" +
                "{PoolBottom#ifnz}#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "<name><![CDATA[Название параметра]]></name>\n" +
                "<value><![CDATA[дно бассейна: {PoolBottom}]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "\n",
            "\n" +
                "\n" +
                "{weight#ifnz}\n" +
                "<spec_guru_modelcard>\n" +
                "<name><![CDATA[вес в упаковке]]></name>\n" +
                "<value><![CDATA[вес в упаковке: {weight} кг]]></value>\n" +
                "</spec_guru_modelcard>\n" +
                "{#endif}\n" +
                "\n"
        );
    }

    @SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLenght"})
    private static String getContentCaseOne() {
        return "<ya_guru_modelcard>\n" +
            "{XL-Picture#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX}\" SizeY=\"{XLPictureSizeY}\"><![CDATA[{XL-Picture}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "<additional_pictures>\n" +
            "{XL-Picture_2#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_2}\" SizeY=\"{XLPictureSizeY_2}\"><![CDATA[{XL-Picture_2}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_3#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_3}\" SizeY=\"{XLPictureSizeY_3}\"><![CDATA[{XL-Picture_3}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_4#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_4}\" SizeY=\"{XLPictureSizeY_4}\"><![CDATA[{XL-Picture_4}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_5#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_5}\" SizeY=\"{XLPictureSizeY_5}\"><![CDATA[{XL-Picture_5}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_6#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_6}\" SizeY=\"{XLPictureSizeY_6}\"><![CDATA[{XL-Picture_6}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_7#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_7}\" SizeY=\"{XLPictureSizeY_7}\"><![CDATA[{XL-Picture_7}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_8#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_8}\" SizeY=\"{XLPictureSizeY_8}\"><![CDATA[{XL-Picture_8}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_9#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_9}\" SizeY=\"{XLPictureSizeY_9}\"><![CDATA[{XL-Picture_9}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_10#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_10}\" SizeY=\"{XLPictureSizeY_10}\"><![CDATA[{XL-Picture_10}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_11#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_11}\" SizeY=\"{XLPictureSizeY_11}\"><![CDATA[{XL-Picture_11}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_12#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_12}\" SizeY=\"{XLPictureSizeY_12}\"><![CDATA[{XL-Picture_12}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_13#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_13}\" SizeY=\"{XLPictureSizeY_13}\"><![CDATA[{XL-Picture_13}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_14#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_14}\" SizeY=\"{XLPictureSizeY_14}\"><![CDATA[{XL-Picture_14}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_15#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_15}\" SizeY=\"{XLPictureSizeY_15}\"><![CDATA[{XL-Picture_15}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_16#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_16}\" SizeY=\"{XLPictureSizeY_16}\"><![CDATA[{XL-Picture_16}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_17#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_17}\" SizeY=\"{XLPictureSizeY_17}\"><![CDATA[{XL-Picture_17}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_18#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_18}\" SizeY=\"{XLPictureSizeY_18}\"><![CDATA[{XL-Picture_18}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_19#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_19}\" SizeY=\"{XLPictureSizeY_19}\"><![CDATA[{XL-Picture_19}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_20#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_20}\" SizeY=\"{XLPictureSizeY_20}\"><![CDATA[{XL-Picture_20}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_21#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_21}\" SizeY=\"{XLPictureSizeY_21}\"><![CDATA[{XL-Picture_21}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_22#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_22}\" SizeY=\"{XLPictureSizeY_22}\"><![CDATA[{XL-Picture_22}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_23#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_23}\" SizeY=\"{XLPictureSizeY_23}\"><![CDATA[{XL-Picture_23}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_24#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_24}\" SizeY=\"{XLPictureSizeY_24}\"><![CDATA[{XL-Picture_24}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_25#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_25}\" SizeY=\"{XLPictureSizeY_25}\"><![CDATA[{XL-Picture_25}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_26#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_26}\" SizeY=\"{XLPictureSizeY_26}\"><![CDATA[{XL-Picture_26}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_27#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_27}\" SizeY=\"{XLPictureSizeY_27}\"><![CDATA[{XL-Picture_27}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_28#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_28}\" SizeY=\"{XLPictureSizeY_28}\"><![CDATA[{XL-Picture_28}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_29#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_29}\" SizeY=\"{XLPictureSizeY_29}\"><![CDATA[{XL-Picture_29}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_30#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_30}\" SizeY=\"{XLPictureSizeY_30}\"><![CDATA[{XL-Picture_30}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_31#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_31}\" SizeY=\"{XLPictureSizeY_31}\"><![CDATA[{XL-Picture_31}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_32#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_32}\" SizeY=\"{XLPictureSizeY_32}\"><![CDATA[{XL-Picture_32}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_33#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_33}\" SizeY=\"{XLPictureSizeY_33}\"><![CDATA[{XL-Picture_33}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_34#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_34}\" SizeY=\"{XLPictureSizeY_34}\"><![CDATA[{XL-Picture_34}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_35#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_35}\" SizeY=\"{XLPictureSizeY_35}\"><![CDATA[{XL-Picture_35}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_36#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_36}\" SizeY=\"{XLPictureSizeY_36}\"><![CDATA[{XL-Picture_36}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_37#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_37}\" SizeY=\"{XLPictureSizeY_37}\"><![CDATA[{XL-Picture_37}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_38#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_38}\" SizeY=\"{XLPictureSizeY_38}\"><![CDATA[{XL-Picture_38}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_39#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_39}\" SizeY=\"{XLPictureSizeY_39}\"><![CDATA[{XL-Picture_39}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_40#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_40}\" SizeY=\"{XLPictureSizeY_40}\"><![CDATA[{XL-Picture_40}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_41#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_41}\" SizeY=\"{XLPictureSizeY_41}\"><![CDATA[{XL-Picture_41}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_42#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_42}\" SizeY=\"{XLPictureSizeY_42}\"><![CDATA[{XL-Picture_42}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_43#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_43}\" SizeY=\"{XLPictureSizeY_43}\"><![CDATA[{XL-Picture_43}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_44#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_44}\" SizeY=\"{XLPictureSizeY_44}\"><![CDATA[{XL-Picture_44}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_45#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_45}\" SizeY=\"{XLPictureSizeY_45}\"><![CDATA[{XL-Picture_45}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_46#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_46}\" SizeY=\"{XLPictureSizeY_46}\"><![CDATA[{XL-Picture_46}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_47#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_47}\" SizeY=\"{XLPictureSizeY_47}\"><![CDATA[{XL-Picture_47}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_48#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_48}\" SizeY=\"{XLPictureSizeY_48}\"><![CDATA[{XL-Picture_48}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_49#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_49}\" SizeY=\"{XLPictureSizeY_49}\"><![CDATA[{XL-Picture_49}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "{XL-Picture_50#ifnz}\n" +
            "<big_picture SizeX=\"{XLPictureSizeX_50}\" SizeY=\"{XLPictureSizeY_50}\"><![CDATA[{XL-Picture_50}]]>" +
            "</big_picture>\n" +
            "{#endif}\n" +
            "\n" +
            "</additional_pictures>\n" +
            "\n" +
            "<block name=\"Общие характеристики\">\n" +
            "{raw_vendor#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Бренд]]></name>\n" +
            "  <value><![CDATA[{raw_vendor}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{is_partner#ifnz}\n" +
            "{type#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Тип]]></name>\n" +
            "  <value><![CDATA[{type}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{box_type#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Тип переноски]]></name>\n" +
            "  <value><![CDATA[{box_type}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{#else}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Тип]]></name>\n" +
            "<value><![CDATA[{type#ifnz}{type}{#endif}{if ($type==\"переноска\") return \"-\";" +
            " else return \"\"; #exec}{box_type#ifnz}{box_type}{#endif}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{form#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Форма]]></name>\n" +
            "<value><![CDATA[{form}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{mission#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Вид животного]]></name>\n" +
            "<value><![CDATA[{mission}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{size_animal#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Размер животного]]></name>\n" +
            "<value><![CDATA[{size_animal}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{max_weight#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Предельная нагрузка]]></name>\n" +
            "<value><![CDATA[{max_weight} кг]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[{if (($width>0)&&($height>0)&&($length>0)) return \"Размеры (ШxВxД): \";\n" +
            "if (($width>0)&&($height>0)&&($length<=0)) return \"Размер (ШxВ): \";\n" +
            "if (($width>0)&&($height<=0)&&($length>0)) return \"Размер (ШxД): \";\n" +
            "if (($width<=0)&&($height>0)&&($length>0)) return \"Размер (ВxД): \";\n" +
            "if (($width>0)&&($height<=0)&&($length<=0)) return \"Ширина: \";\n" +
            "if (($width<=0)&&($height<=0)&&($length>0)) return \"Длина: \";\n" +
            "if (($width<=0)&&($height>0)&&($length<=0)) return \"Высота: \";\n" +
            "else return \"\"; #exec}]]></name>\n" +
            "<value><![CDATA[{string parse=\"\";string out=\"\"; \n" +
            "if ($width>0) out=(string)$width; \n" +
            "if (out) parse=\" х \"; \n" +
            "if ($height>0) out=out+parse+(string)$height;  \n" +
            "if (out) parse=\" х \"; \n" +
            "if ($length>0) out=out+parse+(string)$length;\n" +
            "else return out; #exec}{if (($width>0)||($height>0)||($length>0)) return \" см\"; else return\"\";" +
            "  #exec}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "{weight#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Вес]]></name>\n" +
            "<value><![CDATA[{weight} кг]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{material#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Материал]]></name>\n" +
            "<value><![CDATA[{material}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{specific#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Особенности конструкции]]></name>\n" +
            "<value><![CDATA[{specific}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{use#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Назначение]]></name>\n" +
            "<value><![CDATA[{use}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Comment#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Дополнительная информация]]></name>\n" +
            "<value><![CDATA[{Comment}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "\n" +
            "\n" +
            "</block>\n" +
            "<block name=\"Дополнительно\">\n" +
            "{ShelfService#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Срок службы]]></name>\n" +
            "<value><![CDATA[{ShelfService}{ShelfService_Unit#ifnz}{if ($ShelfService_Unit == \"часы\")" +
            " return \" ч\";if ($ShelfService_Unit == \"дни\") return \" дн.\";if ($ShelfService_Unit == \"недели\")" +
            " return \" нед.\";if ($ShelfService_Unit == \"месяцы\") return \" мес.\";" +
            "if ($ShelfService_Unit == \"годы\") return ($ShelfService>=11) && ($ShelfService<=19) ? \" лет\" :" +
            " ((($ShelfService % 10 >= 1) && ($ShelfService % 10 <= 4)) ? \" г.\" : \" лет\");return \"\";#exec}" +
            "{#else} дн.{#endif}{ShelfService_Comment#ifnz}, {ShelfService_Comment}{#endif}]]></value>" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "</block>\n" +
            "</ya_guru_modelcard>";
    }

    private static String getContentCaseTwo() {
        return "<ya_guru_modelcard>\n" +
            "{XL-Picture#ifnz}\n" +
            " <big_picture SizeX=\"{XLPictureSizeX}\" SizeY=\"{XLPictureSizeY}\"><![CDATA[{XL-Picture}]]>" +
            "</big_picture>\n" +
            "  {#endif} \n" +
            "<additional_pictures>\n" +
            "{XL-Picture_2#ifnz}\n" +
            " <big_picture SizeX=\"{XLPictureSizeX_2}\" SizeY=\"{XLPictureSizeY_2}\"><![CDATA[{XL-Picture_2}]]>" +
            "</big_picture>\n" +
            "  {#endif} \n" +
            "{XL-Picture_3#ifnz}\n" +
            " <big_picture SizeX=\"{XLPictureSizeX_3}\" SizeY=\"{XLPictureSizeY_3}\"><![CDATA[{XL-Picture_3}]]>" +
            "</big_picture>\n" +
            "  {#endif} \n" +
            "{XL-Picture_4#ifnz}\n" +
            " <big_picture SizeX=\"{XLPictureSizeX_4}\" SizeY=\"{XLPictureSizeY_4}\"><![CDATA[{XL-Picture_4}]]>" +
            "</big_picture>\n" +
            "  {#endif} \n" +
            "{XL-Picture_5#ifnz}\n" +
            " <big_picture SizeX=\"{XLPictureSizeX_5}\" SizeY=\"{XLPictureSizeY_5}\"><![CDATA[{XL-Picture_5}]]>" +
            "</big_picture>\n" +
            "  {#endif} \n" +
            "</additional_pictures>\n" +
            "\n" +
            "<block name=\"Система очистки снега\">\n" +
            "{raw_vendor#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Бренд]]></name>\n" +
            "  <value><![CDATA[{raw_vendor}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{SnowblowerType#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Тип системы очистки]]></name>\n" +
            "  <value><![CDATA[{SnowblowerType}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            " {#endif}\n" +
            "\n" +
            "{CleaningWidth#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Ширина захвата снега]]></name>\n" +
            "  <value><![CDATA[{CleaningWidth} см]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{CleaningHeight#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Высота захвата снега]]></name>\n" +
            "  <value><![CDATA[{CleaningHeight} см]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{ThrowingDistanceMax#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Дальность выброса снега]]></name>\n" +
            "  <value><![CDATA[{ThrowingDistanceMin#ifnz}{ThrowingDistanceMin}-{#endif}{ThrowingDistanceMax} м]]>" +
            "</value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{AugerDiam#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Диаметр шнека]]></name>\n" +
            "  <value><![CDATA[{AugerDiam} см]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{AugerMaterial#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Материал шнека]]></name>\n" +
            "  <value><![CDATA[{AugerMaterial}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{AugerForm#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Форма шнеков]]></name>\n" +
            "  <value><![CDATA[{AugerForm}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{ChuteRotationAngle#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Максимальный угол поворота желоба выброса снега]]></name>\n" +
            "  <value><![CDATA[{ChuteRotationAngle}°]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{ChuteMaterial#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Материал желоба выброса снега]]></name>\n" +
            "  <value><![CDATA[{ChuteMaterial}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{ChuteRotationType#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Регулировка положения желоба выброса снега]]></name>\n" +
            "  <value><![CDATA[{ChuteRotationType}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{ImpellerMaterial#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Материал крыльчатки]]></name>\n" +
            "  <value><![CDATA[{ImpellerMaterial}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{SnowblowingPerf#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Производительность уборки снега]]></name>\n" +
            "  <value><![CDATA[{SnowblowingPerf} тонн/ч]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            " {#endif}\n" +
            "\n" +
            "{IntakeHousing#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Регулировка положения снегозаборника]]></name>\n" +
            "  <value><![CDATA[{IntakeHousing}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "</block>\n" +
            "\n" +
            "\n" +
            "\n" +
            "<block name=\"Двигатель\">\n" +
            "{is_partner#ifnz}\n" +
            "{EngineType#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Тип двигателя]]></name>\n" +
            "  <value><![CDATA[{EngineType}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{NumOfStrokes#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Количество тактов двигателя]]></name>\n" +
            "  <value><![CDATA[{NumOfStrokes}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{#else}\n" +
            "<spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Тип двигателя]]></name>\n" +
            "  <value><![CDATA[{EngineType}{if ($NumOfStrokes==2) return \", двухтактный\"; if ($NumOfStrokes==4)" +
            " return \", четырехтактный\"; else return \"\"; #exec}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "\n" +
            "\n" +
            "{EngineVendor#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Производитель{EngineSeriesModel#ifnz} и модель{#endif} двигателя]]></name>\n" +
            "  <value><![CDATA[{EngineVendor}{EngineSeriesModel#ifnz} {EngineSeriesModel}{#endif}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{is_partner#ifnz}\n" +
            "{NominalPowerKW#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Мощность двигателя]]></name>\n" +
            "  <value><![CDATA[{NominalPowerKW} кВт]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{EngineHP#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Мощность двигателя, л.с.]]></name>\n" +
            "  <value><![CDATA[{EngineHP}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{NominalPowerRPM#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Номинальная мощность, об/мин]]></name>\n" +
            "  <value><![CDATA[{NominalPowerRPM} об/мин]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{#else}\n" +
            "<spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Мощность двигателя]]></name>\n" +
            "  <value><![CDATA[{if (($NominalPowerKW>0)&&($EngineHP>0)&&($NominalPowerRPM>0))" +
            " return $NominalPowerKW + \" кВт / \" + $EngineHP + \" л.с. при \" + $NominalPowerRPM + \" об/мин\";" +
            " if (($NominalPowerKW<0.1)&&($EngineHP>0)&&($NominalPowerRPM>0)) return $EngineHP + \" л.с. при \" +" +
            " $NominalPowerRPM + \" об/мин\";if (($NominalPowerKW>0)&&($EngineHP<0.1)&&($NominalPowerRPM>0))" +
            " return $NominalPowerKW + \" кВт при \" + $NominalPowerRPM + \" об/мин\";" +
            " if (($NominalPowerKW>0)&&($EngineHP>0)&&($NominalPowerRPM<1)) return $NominalPowerKW + \" кВт / \" +" +
            " $EngineHP + \" л.с.\"; if (($NominalPowerKW<0.1)&&($EngineHP>0)&&($NominalPowerRPM<1))" +
            " return $EngineHP + \" л.с.\"; if (($NominalPowerKW>0)&&($EngineHP<0.1)&&($NominalPowerRPM<1))" +
            " return $NominalPowerKW + \" кВт\"; else return \"\"; #exec}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "\n" +
            "\n" +
            "{Displacement#ifnz} <!-- НЕ УДАЛЯТЬ--> {#endif}\n" +
            "{is_partner#ifnz}\n" +
            "{EngineType#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Тип двигателя]]></name>\n" +
            "  <value><![CDATA[{EngineType}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "{#endif}\n" +
            "\n" +
            "{AutoRPM#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Автоматическая регулировка оборотов двигателя]]></name>\n" +
            "  <value><![CDATA[{AutoRPM}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{WorkingTime#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Время непрерывной работы]]></name>\n" +
            "  <value><![CDATA[{WorkingTime} ч]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{TankSize#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Объем топливного бака]]></name>\n" +
            "  <value><![CDATA[{TankSize} л]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Estarter#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Электростартер]]></name>\n" +
            "  <value><![CDATA[{Estarter}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "</block>\n" +
            "\n" +
            "\n" +
            "<block name=\"Привод\">\n" +
            "\n" +
            "{DriveType#ifnz} <!-- НЕ УДАЛЯТЬ--> {#endif}\n" +
            "\n" +
            "{SelfMoving#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Самоходный]]></name>\n" +
            "  <value><![CDATA[{SelfMoving}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            " {#endif}\n" +
            "\n" +
            "{TransmissionType#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Тип трансмиссии]]></name>\n" +
            "  <value><![CDATA[{TransmissionType}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{SpeedsForward#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Количество передач]]></name>\n" +
            "  <value><![CDATA[{SpeedsForward} вперед{SpeedsBackward#ifnz}, {SpeedsBackward} назад{#endif}]]>" +
            "</value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{MaxSpeedForward#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Максимальная скорость движения]]></name>\n" +
            "  <value><![CDATA[{MaxSpeedForward} м/мин вперед{MaxSpeedBackward#ifnz}," +
            " {MaxSpeedBackward} м/мин назад{#endif}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{DiffBlock#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Отключаемая блокировка дифференциала]]></name>\n" +
            "  <value><![CDATA[{DiffBlock}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "</block>\n" +
            "\n" +
            "\n" +
            "<block name=\"Особенности\">\n" +
            "    \n" +
            "  {accum_include#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Аккумулятор в комплекте]]></name>\n" +
            "  <value><![CDATA[{accum_include}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif} \n" +
            "    \n" +
            "  {accum_work#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Аккумуляторный]]></name>\n" +
            "  <value><![CDATA[{accum_work}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{FuelIndicator#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Датчик уровня топлива]]></name>\n" +
            "  <value><![CDATA[{FuelIndicator}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{SnowCutters#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Нож для подрезки сугробов]]></name>\n" +
            "  <value><![CDATA[{SnowCutters}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{HandleHeightAdjustment#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Регулировка рукоятки по высоте]]></name>\n" +
            "  <value><![CDATA[{HandleHeightAdjustment}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{HeatingHandles#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Рукоятки с подогревом]]></name>\n" +
            "  <value><![CDATA[{HeatingHandles}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{OneHandOperation#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Возможность управления одной рукой]]></name>\n" +
            "  <value><![CDATA[{OneHandOperation}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Light#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Фара]]></name>\n" +
            "  <value><![CDATA[{Light}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{WeightTransfer#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Система переноса веса]]></name>\n" +
            "  <value><![CDATA[{WeightTransfer}]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{NoiseLevel#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "  <name><![CDATA[Уровень шума]]></name>\n" +
            "  <value><![CDATA[{NoiseLevel} дБ]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Brush#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Наличие подметальной щетки]]></name>\n" +
            "  <value><![CDATA[да]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{SnowRobot#ifnz}\n" +
            " <spec_guru_modelcard main_property=\"1\">\n" +
            "  <name><![CDATA[Робот-снегоуборщик\t]]></name>\n" +
            "  <value><![CDATA[да]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "</block>\n" +
            "\n" +
            "\n" +
            "<block name=\"Общие характеристики\">\n" +
            "{WheelSize#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Диаметр колес]]></name>\n" +
            "  <value><![CDATA[{WheelSize}\"]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Length#ifnz}{Width#ifnz}{Height#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Габариты, ДхШхВ]]></name>\n" +
            "  <value><![CDATA[{Length}х{Width}х{Height} см]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}{#endif}{#endif}\n" +
            "\n" +
            "{Weight#ifnz}\n" +
            " <spec_guru_modelcard>\n" +
            "  <name><![CDATA[Масса]]></name>\n" +
            "  <value><![CDATA[{Weight} кг]]></value>\n" +
            " </spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{Comment#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Дополнительная информация]]></name>\n" +
            "<value><![CDATA[{Comment}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "</block>\n" +
            "<block name=\"Дополнительно\">\n" +
            "{ShelfService#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Срок службы]]></name>\n" +
            "<value><![CDATA[{ShelfService}{ShelfService_Unit#ifnz}{if ($ShelfService_Unit == \"часы\")" +
            " return \" ч\";if ($ShelfService_Unit == \"дни\") return \" дн.\";if ($ShelfService_Unit == \"недели\")" +
            " return \" нед.\";if ($ShelfService_Unit == \"месяцы\") return \" мес.\";" +
            "if ($ShelfService_Unit == \"годы\") return ($ShelfService>=11)" +
            " && ($ShelfService<=19) ? \" лет\" : ((($ShelfService % 10 >= 1)" +
            " && ($ShelfService % 10 <= 4)) ? \" г.\" : \" лет\");return \"\";#exec}" +
            "{#else} дн.{#endif}{ShelfService_Comment#ifnz}, {ShelfService_Comment}{#endif}]]>" +
            "</value></spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{WarrantyPeriod#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Гарантийный срок]]></name>\n" +
            "<value><![CDATA[{WarrantyPeriod}{WarrantyPeriod_Unit#ifnz}{if ($WarrantyPeriod_Unit == \"часы\")" +
            " return \" ч\";if ($WarrantyPeriod_Unit == \"дни\") return \" дн.\";" +
            "if ($WarrantyPeriod_Unit == \"недели\") return \" нед.\";" +
            "if ($WarrantyPeriod_Unit == \"месяцы\") return \" мес.\";" +
            "if ($WarrantyPeriod_Unit == \"годы\") return ($WarrantyPeriod>=11)" +
            " && ($WarrantyPeriod<=19) ? \" лет\" : ((($WarrantyPeriod % 10 >= 1)" +
            " && ($WarrantyPeriod % 10 <= 4)) ? \" г.\" : \" лет\");return \"\";#exec}" +
            "{#else} дн.{#endif}{WarrantyPeriod_Comment#ifnz}, {WarrantyPeriod_Comment}{#endif}]]>" +
            "</value></spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "</block>\n" +
            "</ya_guru_modelcard>";
    }

    private static String getContentCaseThree() {
        return "<ya_guru_modelcard>\n" +
            "<block name=\\\"дружба\\\">\n" +
            "    \n" +
            "    {is_partner#ifnz}\n" +
            "    \n" +
            "{construction#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[первая строка]]></name>\n" +
            "<value><![CDATA[{construction} бассейн]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{#else}\n" +
            "    \n" +
            "    <spec_guru_modelcard>\n" +
            "<name><![CDATA[первая строка]]></name>\n" +
            "<value><![CDATA[\n" +
            "\n" +
            "{PoolGameCenter#ifnz}игровой центр\n" +
            "  {dry#ifnz} с сухим бассейном\n" +
            "  {#else} с надувным бассейном\n" +
            "  {#endif}\n" +
            "{#else}\n" +
            "  {dry#ifnz}сухой бассейн\n" +
            "  {#else}\n" +
            "    {spa#ifnz}{construction} SPA-бассейн\n" +
            "    {#else}{construction} бассейн\n" +
            "    {#endif}\n" +
            "  {#endif}\n" +
            "{#endif}\n" +
            "\n" +
            "]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "{#endif}\n" +
            "\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[вторая строка]]></name>\n" +
            "<value><![CDATA[{\n" +
            "if (($PoolHeight==0)&&($PoolWidth==0)&&($Poollength==0)&&($PoolDia>0))" +
            " return \\\"диаметр \\\" + $PoolDia + \\\" см\\\";\n" +
            "if (($PoolHeight==0)&&($PoolWidth==0)&&($Poollength>0)&&($PoolDia==0))" +
            " return \\\"длина \\\" + $Poollength + \\\" см\\\";\n" +
            "if (($PoolHeight==0)&&($PoolWidth>0)&&($Poollength==0)&&($PoolDia==0))" +
            " return \\\"ширина \\\" + $PoolWidth + \\\" см\\\";\n" +
            "if (($PoolHeight>0)&&($PoolWidth==0)&&($Poollength==0)&&($PoolDia==0))" +
            " return \\\"глубина \\\" + $PoolHeight + \\\" см\\\";\n" +
            "if (($PoolHeight>0)&&($PoolWidth>0)&&($Poollength>0)) return \\\"ДхШ \\\"" +
            " + $Poollength + \\\"х\\\" + $PoolWidth + \\\" см, глубина \\\" + $PoolHeight+ \\\" см\\\";\n" +
            "if (($PoolHeight==0)&&($PoolWidth>0)&&($Poollength>0)&&($PoolDia==0))" +
            " return \\\"ДхШ \\\" + $Poollength + \\\"х\\\" + $PoolWidth+ \\\" см\\\";\n" +
            "if (($PoolHeight>0)&&($PoolWidth==0)&&($Poollength>0)&&($PoolDia==0))" +
            " return \\\"длина \\\" + $Poollength + \\\" см, глубина \\\" + $PoolHeight+ \\\" см\\\";\n" +
            "if (($PoolHeight>0)&&($PoolWidth>0)&&($Poollength==0)&&($PoolDia==0))" +
            " return \\\"ширина \\\" + $PoolWidth + \\\" см, глубина \\\" + $PoolHeight+ \\\" см\\\";\n" +
            "if (($PoolHeight>0)&&($PoolWidth==0)&&($Poollength==0)&&($PoolDia>0))" +
            " return \\\"диаметр \\\" + $PoolDia + \\\" см, глубина \\\" + $PoolHeight+ \\\" см\\\";\n" +
            "else return \\\"\\\";#exec}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[третья строка]]></name>\n" +
            "<value><![CDATA[{if ($PoolCapacity>0) return \\\"объём: \\\" + $PoolCapacity+ \\\" л\\\";\n" +
            "else return \\\"\\\";#exec}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "{is_partner#ifnz}\n" +
            "\n" +
            "{PoolBottom#ifnz}#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[Название параметра]]></name>\n" +
            "<value><![CDATA[дно бассейна: {PoolBottom}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{#else}\n" +
            "\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[четвёртая строка]]></name>\n" +
            "<value><![CDATA[{if (($PoolKids)&&($PoolBottom==\\\"жёсткое\\\")) return \\\"жёсткое дно\\\";" +
            " if (($PoolKids)&&($PoolBottom==\\\"мягкое надувное\\\")) return \\\"мягкое надувное дно\\\";\n" +
            "else return \\\"\\\";#exec}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "{#endif}\n" +
            "\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[пятая строка]]></name>\n" +
            "<value><![CDATA[\n" +
            "в комплекте: {*comma-on}{balloons:шарики}{stairs:лестница}{Tent:тент}{PoolPump:водяной насос}" +
            "{Mat:подстилка под бассейн}{fountain:разбрызгиватель}{skimmer:скиммер}{disinfection}{filtertype}" +
            "{*comma-off}{filtertype#ifnz} фильтр{#endif}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "\n" +
            "{is_partner#ifnz}\n" +
            "\n" +
            "{weight#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[вес в упаковке]]></name>\n" +
            "<value><![CDATA[вес в упаковке: {weight} кг]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{#else}\n" +
            "\n" +
            "{weight#ifnz}\n" +
            "<spec_guru_modelcard>\n" +
            "<name><![CDATA[шестая строка]]></name>\n" +
            "<value><![CDATA[{if ($weight>5) return \\\"вес в упаковке: \\\" + $weight+ \\\" кг\\\";\n" +
            "else return \\\"\\\";#exec}]]></value>\n" +
            "</spec_guru_modelcard>\n" +
            "{#endif}\n" +
            "\n" +
            "{#endif}\n" +
            "\n" +
            "</block>\n" +
            "\n" +
            "</ya_guru_modelcard>";
    }
}
