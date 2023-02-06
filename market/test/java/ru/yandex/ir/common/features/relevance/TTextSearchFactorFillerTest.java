package ru.yandex.ir.common.features.relevance;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.ir.common.features.relevance.base.TDocumentHitsBuf;
import ru.yandex.ir.common.features.relevance.base.TFullPosition;
import ru.yandex.ir.common.features.relevance.base.TFullPositionEx;
import ru.yandex.ir.common.features.relevance.base.TSignedPosting;
import ru.yandex.ir.common.features.relevance.base.TWeightOfWords;

import java.util.HashMap;
import java.util.Map;

class TTextSearchFactorFillerTest {

    @Test
    public void test_iphone_11_pro() {
        float[] wordWeights = {2.04031f, 1.2401f, 1.63476f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4400, 4400, 0),
                buildFullPositionEx(4464, 4464, 1),
                buildFullPositionEx(4528, 4528, 2),
                buildFullPositionEx(8464, 8464, 0),
                buildFullPositionEx(8528, 8528, 1),
                buildFullPositionEx(8592, 8592, 2)
        };
        int[] textSenLengths = {0, 11, 31};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.997506f);
        values.put("BM15_W", 0.997506f);
        values.put("BM25", 0.492264f);
        values.put("BM25_EX", 0.492264f);
        values.put("BM25_SY", 0.492264f);
        values.put("BM25_W1", 0.492264f);
        values.put("BM25_EX_W1", 0.492264f);
        values.put("BM25_SY_W1", 0.492264f);
        values.put("BM25_BREAK", 0.5f);
        values.put("BM25_BREAK_EX", 0.5f);
        values.put("BM25_BREAK_SY", 0.5f);
        values.put("BM25_PAIR", 0.292448f);
        values.put("BM25_PAIR_EX", 0.292448f);
        values.put("BM25_PAIR_SY", 0.292448f);
        values.put("BM25_HI_REL", 0.5f);
        values.put("BM25_HI_REL_EX", 0.5f);
        values.put("BM25_HI_REL_SY", 0.5f);
        values.put("BOCM", 0.923077f);
        values.put("BOCM_W", 0.923077f);
        values.put("BCLM", 0.949564f);
        values.put("SWBM25", 0.830565f);
        values.put("TOCM", 0.761905f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.909091f);
        values.put("BM15_ATTEN_W1_K4", 0.668651f);
        values.put("BM15_V0_W1_K4", 0.5f);
        values.put("BCLM_HARD_W1_K1", 0.999992f);
        values.put("BCLM_HARD_W1_K2", 0.999917f);
        values.put("BCLM_WEIGHTED_K4", 0.937522f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.999937f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.941423f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_iphone_case() {
        float[] wordWeights = {1.12764f, 1.15807e-05f, 13.3642f, 1.2401f, 4.63668f, 3.81711f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4272, 4272, 0),
                buildFullPositionEx(4336, 4336, 1),
                buildFullPositionEx(4464, 4464, 3),
                buildFullPositionEx(8336, 8336, 0),
                buildFullPositionEx(8400, 8400, 1),
                buildFullPositionEx(8528, 8528, 3),
                buildFullPositionEx(8976, 8976, 1),
                buildFullPositionEx(9104, 9105, 0),
                buildFullPositionEx(9168, 9169, 0),
                buildFullPositionEx(9232, 9233, 0),
                buildFullPositionEx(9296, 9296, 1),
                buildFullPositionEx(9552, 9552, 1)
        };
        int[] textSenLengths = {0, 8, 23};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.498753f);
        values.put("BM15_W", 0.0976543f);
        values.put("BM25", 0.0483961f);
        values.put("BM25_EX", 0.0483961f);
        values.put("BM25_SY", 0.0483961f);
        values.put("BM25_W1", 0.247175f);
        values.put("BM25_EX_W1", 0.247175f);
        values.put("BM25_SY_W1", 0.247175f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 2.39411e-07f);
        values.put("BM25_PAIR_EX", 2.39411e-07f);
        values.put("BM25_PAIR_SY", 2.39411e-07f);
        values.put("BM25_HI_REL", 0.0489492f);
        values.put("BM25_HI_REL_EX", 0.0489492f);
        values.put("BM25_HI_REL_SY", 0.0489492f);
        values.put("BOCM", 0.485437f);
        values.put("BOCM_W", 0.096013f);
        values.put("BCLM", 0.447887f);
        values.put("SWBM25", 0.0813109f);
        values.put("TOCM", 0.079917f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.454545f);
        values.put("BM15_ATTEN_W1_K4", 0.378205f);
        values.put("BM15_V0_W1_K4", 0.25f);
        values.put("BCLM_HARD_W1_K1", 0.33333f);
        values.put("BCLM_HARD_W1_K2", 0.3333f);
        values.put("BCLM_WEIGHTED_K4", 1.11969e-05f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.188642f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.155852f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_water() {
        //text=Вода Бабушкино лукошко Детская питьевая, 0.5 л
        //modelId=1723074676
        float[] wordWeights = {0.525293f, 6.26017f, 6.19904f, 0.882151f, 7.06133f, 13.3642f, 1.61796f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4208, 4208, 3),
                buildFullPositionEx(4272, 4272, 0),
                buildFullPositionEx(4336, 4336, 1),
                buildFullPositionEx(4400, 4400, 2),
                buildFullPositionEx(8592, 8592, 0),
                buildFullPositionEx(8720, 8721, 0),
                buildFullPositionEx(8784, 8784, 4),
                buildFullPositionEx(9424, 9424, 1),
                buildFullPositionEx(9488, 9488, 2),
                buildFullPositionEx(9552, 9552, 0),
                buildFullPositionEx(9616, 9616, 3),
                buildFullPositionEx(9680, 9680, 4),
                buildFullPositionEx(9744, 9744, 0),
                buildFullPositionEx(9808, 9808, 3),
                buildFullPositionEx(9872, 9872, 0),
                buildFullPositionEx(9936, 9936, 1),
                buildFullPositionEx(10000, 10000, 2),
                buildFullPositionEx(10064, 10064, 3),
                buildFullPositionEx(10128, 10128, 3),
                buildFullPositionEx(10192, 10192, 0),
                buildFullPositionEx(10256, 10256, 3),
                buildFullPositionEx(10320, 10320, 4),
                buildFullPositionEx(10384, 10384, 0),
                buildFullPositionEx(10448, 10448, 0),
                buildFullPositionEx(10512, 10512, 1),
                buildFullPositionEx(10576, 10576, 2)
        };
        int[] textSenLengths = {0, 6, 37};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.570004f);
        values.put("BM15_W", 0.385185f);
        values.put("BM25", 0.191433f);
        values.put("BM25_EX", 0.191433f);
        values.put("BM25_SY", 0.191433f);
        values.put("BM25_W1", 0.283286f);
        values.put("BM25_EX_W1", 0.283286f);
        values.put("BM25_SY_W1", 0.283286f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.173477f);
        values.put("BM25_PAIR_EX", 0.173477f);
        values.put("BM25_PAIR_SY", 0.173477f);
        values.put("BM25_HI_REL", 0.193074f);
        values.put("BM25_HI_REL_EX", 0.193074f);
        values.put("BM25_HI_REL_SY", 0.193074f);
        values.put("BOCM", 0.558854f);
        values.put("BOCM_W", 0.378004f);
        values.put("BCLM", 0.545383f);
        values.put("SWBM25", 0.320721f);
        values.put("TOCM", 0.32263f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.519481f);
        values.put("BM15_ATTEN_W1_K4", 0.460849f);
        values.put("BM15_V0_W1_K4", 0.285714f);
        values.put("BCLM_HARD_W1_K1", 0.428568f);
        values.put("BCLM_HARD_W1_K2", 0.428536f);
        values.put("BCLM_WEIGHTED_K4", 0.376752f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.571395f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.54227f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_aelita() {
        //text=Подвеска Солнечный зайчик вертикальная Аэлита 2С404
        //modelId=1731656552
        //br[1] = Подвесная игрушка Аэлита Солнечный зайчик (2С404)
        //br[2] = для автомобильного кресла на автокресло, для шезлонга на шезлонг для качели для качелей на качель, на коляску для колясок на бампер коляски в коляску для прогулочной коляски, на кроватку в кроватку над кроваткой на детскую кроватку для кроватки, для коврика для игрового коврика, из пластика пластиковая пластмассовая из пластмассы пластик на карабине с карабином подвес подвеска игрушка подвесная набор подвесов подвесные игрушки набор
        //br[3] = подвесок Подвески игрушки Подвески для малышей Aelita Аэлита 2С404
        float[] wordWeights = {1.31892f, 6.48198f, 6.53887f, 5.55387f, 6.88527f, 13.3642f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4336, 4336, 4),
                buildFullPositionEx(4400, 4400, 1),
                buildFullPositionEx(4464, 4464, 2),
                buildFullPositionEx(4528, 4656, 5),
                buildFullPositionEx(11792, 11792, 0),
                buildFullPositionEx(12368, 12369, 0),
                buildFullPositionEx(12432, 12433, 0),
                buildFullPositionEx(12560, 12561, 0),
                buildFullPositionEx(12816, 12816, 4),
                buildFullPositionEx(12880, 13008, 5)
        };
        int[] textSenLengths = {0, 8, 63, 14};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.665004f);
        values.put("BM15_W", 0.826726f);
        values.put("BM25", 0.409714f);
        values.put("BM25_EX", 0.409714f);
        values.put("BM25_SY", 0.409714f);
        values.put("BM25_W1", 0.329567f);
        values.put("BM25_EX_W1", 0.329567f);
        values.put("BM25_SY_W1", 0.329567f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.0814444f);
        values.put("BM25_PAIR_EX", 0.0814444f);
        values.put("BM25_PAIR_SY", 0.0814444f);
        values.put("BM25_HI_REL", 0.414397f);
        values.put("BM25_HI_REL_EX", 0.414397f);
        values.put("BM25_HI_REL_SY", 0.414397f);
        values.put("BOCM", 0.650546f);
        values.put("BOCM_W", 0.811083f);
        values.put("BCLM", 0.636281f);
        values.put("SWBM25", 0.688366f);
        values.put("TOCM", 0.718748f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.606061f);
        values.put("BM15_ATTEN_W1_K4", 0.462531f);
        values.put("BM15_V0_W1_K4", 0.333333f);
        values.put("BCLM_HARD_W1_K1", 0.33333f);
        values.put("BCLM_HARD_W1_K2", 0.3333f);
        values.put("BCLM_WEIGHTED_K4", 0.81916f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.666659f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.659556f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_uppababy() {
        //text=Коляска прогулочная UPPAbaby Cruz 2018 Taylor (Indigo)
        //modelId=133629096
        //br[1] = Прогулочная коляска  UppaBaby Cruz 2018
        //br[2] = прогулочная коляска Коляски Детские коляски Дитячі коляски UppaBaby Cruz 2018 прогулочная
        float[] wordWeights = {3.03458f, 4.29248f, 8.3899f, 5.40991f, 6.0763f, 5.21422f, 5.29095f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4208, 4208, 1),
                buildFullPositionEx(4272, 4272, 0),
                buildFullPositionEx(4336, 4336, 2),
                buildFullPositionEx(4400, 4400, 3),
                buildFullPositionEx(4464, 4464, 4),
                buildFullPositionEx(8272, 8272, 1),
                buildFullPositionEx(8336, 8336, 0),
                buildFullPositionEx(8400, 8401, 0),
                buildFullPositionEx(8528, 8529, 0),
                buildFullPositionEx(8656, 8657, 0),
                buildFullPositionEx(8720, 8720, 2),
                buildFullPositionEx(8784, 8784, 3),
                buildFullPositionEx(8848, 8848, 4),
                buildFullPositionEx(8912, 8912, 1),
                buildFullPositionEx(8976, 8976, 1),
                buildFullPositionEx(9040, 9040, 3),
                buildFullPositionEx(9104, 9104, 4),
                buildFullPositionEx(9168, 9168, 1),
                buildFullPositionEx(9232, 9232, 2),
                buildFullPositionEx(9296, 9296, 3),
                buildFullPositionEx(9360, 9360, 4),
                buildFullPositionEx(9424, 9424, 1),
                buildFullPositionEx(9488, 9488, 0),
                buildFullPositionEx(9552, 9552, 2),
                buildFullPositionEx(9616, 9616, 3),
                buildFullPositionEx(9680, 9680, 4)
        };
        int[] textSenLengths = {0, 5, 27};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.712504f);
        values.put("BM15_W", 0.719611f);
        values.put("BM25", 0.358147f);
        values.put("BM25_EX", 0.358147f);
        values.put("BM25_SY", 0.358147f);
        values.put("BM25_W1", 0.35461f);
        values.put("BM25_EX_W1", 0.35461f);
        values.put("BM25_SY_W1", 0.35461f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.152303f);
        values.put("BM25_PAIR_EX", 0.152303f);
        values.put("BM25_PAIR_SY", 0.152303f);
        values.put("BM25_HI_REL", 0.360705f);
        values.put("BM25_HI_REL_EX", 0.360705f);
        values.put("BM25_HI_REL_SY", 0.360705f);
        values.put("BOCM", 0.708073f);
        values.put("BOCM_W", 0.715563f);
        values.put("BCLM", 0.683811f);
        values.put("SWBM25", 0.599178f);
        values.put("TOCM", 0.674019f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.649351f);
        values.put("BM15_ATTEN_W1_K4", 0.556087f);
        values.put("BM15_V0_W1_K4", 0.357143f);
        values.put("BCLM_HARD_W1_K1", 0.428568f);
        values.put("BCLM_HARD_W1_K2", 0.428536f);
        values.put("BCLM_WEIGHTED_K4", 0.710635f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.714275f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.703365f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_intellectico() {
        //text=Набор Юный Физик Intellectico Магнитный лабиринт
        //modelId=1728842238
        float[] wordWeights = {1.28208f, 6.72603f, 9.00929f, 10.3577f, 4.98169f, 5.67685f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4208, 4208, 0),
                buildFullPositionEx(4272, 4272, 3),
                buildFullPositionEx(4336, 4336, 1),
                buildFullPositionEx(4400, 4400, 2),
                buildFullPositionEx(8304, 8304, 4),
                buildFullPositionEx(8368, 8368, 5),
                buildFullPositionEx(12432, 12433, 2),
                buildFullPositionEx(12752, 12753, 4),
                buildFullPositionEx(13072, 13073, 0),
                buildFullPositionEx(13328, 13329, 0),
                buildFullPositionEx(13712, 13712, 1),
                buildFullPositionEx(13776, 13776, 2),
                buildFullPositionEx(16464, 16464, 4),
                buildFullPositionEx(16528, 16528, 5),
                buildFullPositionEx(16656, 16656, 4),
                buildFullPositionEx(16720, 16720, 5)
        };
        int[] textSenLengths = {0, 4, 3, 23, 5};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.997506f);
        values.put("BM15_W", 0.997506f);
        values.put("BM25", 0.49505f);
        values.put("BM25_EX", 0.49505f);
        values.put("BM25_SY", 0.49505f);
        values.put("BM25_W1", 0.49505f);
        values.put("BM25_EX_W1", 0.49505f);
        values.put("BM25_SY_W1", 0.49505f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.193068f);
        values.put("BM25_PAIR_EX", 0.193068f);
        values.put("BM25_PAIR_SY", 0.193068f);
        values.put("BM25_HI_REL", 0.5f);
        values.put("BM25_HI_REL_EX", 0.5f);
        values.put("BM25_HI_REL_SY", 0.5f);
        values.put("BOCM", 0.964166f);
        values.put("BOCM_W", 0.966677f);
        values.put("BCLM", 0.948395f);
        values.put("SWBM25", 0.829794f);
        values.put("TOCM", 0.896883f);
        values.put("WCM_COVERAGE_MAX", 0.719759f);
        values.put("PCM_MAX", 0.232393f);
        values.put("BM15_STRICT_W1_K4", 0.909091f);
        values.put("BM15_ATTEN_W1_K4", 0.828061f);
        values.put("BM15_V0_W1_K4", 0.5f);
        values.put("BCLM_HARD_W1_K1", 0.66666f);
        values.put("BCLM_HARD_W1_K2", 0.6666f);
        values.put("BCLM_WEIGHTED_K4", 0.986736f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.999987f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.986888f);
        values.put("BOCM_DOUBLE_K7", 0.261208f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_brauberg() {
        //text=Доска пробковая BRAUBERG для объявлений, 90х120 см, алюминиевая рамка, 236445
        //modelId=498794010
        float[] wordWeights = {3.22874f, 7.94744f, 6.31652f, 1.15807e-05f, 9.4083f, 13.3642f, 0.585877f, 5.19859f,
                2.90502f, 13.3642f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4208, 4208, 0),
                buildFullPositionEx(4272, 4272, 1),
                buildFullPositionEx(4336, 4336, 2),
                buildFullPositionEx(4400, 4400, 9),
                buildFullPositionEx(4464, 4592, 5),
                buildFullPositionEx(4656, 4656, 6),
                buildFullPositionEx(8336, 8336, 0),
                buildFullPositionEx(8464, 8465, 0),
                buildFullPositionEx(8528, 8528, 1),
                buildFullPositionEx(9808, 9809, 0),
                buildFullPositionEx(9936, 9936, 6),
                buildFullPositionEx(10064, 10065, 0),
                buildFullPositionEx(10192, 10192, 6),
                buildFullPositionEx(10256, 10257, 0),
                buildFullPositionEx(10320, 10321, 0),
                buildFullPositionEx(10384, 10384, 3),
                buildFullPositionEx(10576, 10577, 0),
                buildFullPositionEx(10640, 10640, 2),
                buildFullPositionEx(10704, 10704, 9),
                buildFullPositionEx(10768, 10768, 0),
                buildFullPositionEx(10832, 10832, 1),
                buildFullPositionEx(10896, 10896, 3),
                buildFullPositionEx(10960, 10960, 4),
                buildFullPositionEx(11216, 11216, 6),
                buildFullPositionEx(11280, 11280, 9)
        };
        int[] textSenLengths = {0, 8, 48};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.598504f);
        values.put("BM15_W", 0.717202f);
        values.put("BM25", 0.355436f);
        values.put("BM25_EX", 0.355436f);
        values.put("BM25_SY", 0.355436f);
        values.put("BM25_W1", 0.29661f);
        values.put("BM25_EX_W1", 0.29661f);
        values.put("BM25_SY_W1", 0.29661f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.119144f);
        values.put("BM25_PAIR_EX", 0.119144f);
        values.put("BM25_PAIR_SY", 0.119144f);
        values.put("BM25_HI_REL", 0.359498f);
        values.put("BM25_HI_REL_EX", 0.359498f);
        values.put("BM25_HI_REL_SY", 0.359498f);
        values.put("BOCM", 0.590709f);
        values.put("BOCM_W", 0.70432f);
        values.put("BCLM", 0.575567f);
        values.put("SWBM25", 0.597172f);
        values.put("TOCM", 0.599636f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.545455f);
        values.put("BM15_ATTEN_W1_K4", 0.444816f);
        values.put("BM15_V0_W1_K4", 0.3f);
        values.put("BCLM_HARD_W1_K1", 0.499996f);
        values.put("BCLM_HARD_W1_K2", 0.499955f);
        values.put("BCLM_WEIGHTED_K4", 0.713874f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.599995f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.595294f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    @Test
    public void test_planeta_organica() {
        //text=Маска для волос Густая золотая аюрведическая маска 300мл (Planeta Organica)
        //modelId=1969955921
        float[] wordWeights = {3.46062f, 1.15807e-05f, 1.58215f, 13.3642f, 5.0428f, 10.7388f, 3.46062f, 13.3642f,
                12.6717f};
        TFullPositionEx[] positions = {
                buildFullPositionEx(4208, 4272, 8),
                buildFullPositionEx(4656, 4656, 3),
                buildFullPositionEx(4720, 4720, 4),
                buildFullPositionEx(4784, 4784, 5),
                buildFullPositionEx(4848, 4848, 0),
                buildFullPositionEx(4848, 4848, 6),
                buildFullPositionEx(8336, 8336, 0),
                buildFullPositionEx(8336, 8336, 6),
                buildFullPositionEx(9232, 9232, 1),
                buildFullPositionEx(10576, 10577, 0),
                buildFullPositionEx(10576, 10577, 6),
                buildFullPositionEx(10896, 10896, 3),
                buildFullPositionEx(10960, 10960, 4),
                buildFullPositionEx(11024, 11024, 5),
                buildFullPositionEx(11088, 11088, 0),
                buildFullPositionEx(11088, 11088, 6),
                buildFullPositionEx(11472, 11472, 3),
                buildFullPositionEx(11536, 11536, 4),
                buildFullPositionEx(11600, 11600, 5),
                buildFullPositionEx(11664, 11664, 0),
                buildFullPositionEx(11664, 11664, 6),
                buildFullPositionEx(11728, 11728, 5)
        };
        int[] textSenLengths = {0, 11, 56};

        Map<String, Float> values = new HashMap<>();
        values.put("BM15_P", 0.665004f);
        values.put("BM15_W", 0.763399f);
        values.put("BM25", 0.376734f);
        values.put("BM25_EX", 0.376734f);
        values.put("BM25_SY", 0.376734f);
        values.put("BM25_W1", 0.328176f);
        values.put("BM25_EX_W1", 0.328176f);
        values.put("BM25_SY_W1", 0.328176f);
        values.put("BM25_BREAK", 0f);
        values.put("BM25_BREAK_EX", 0f);
        values.put("BM25_BREAK_SY", 0f);
        values.put("BM25_PAIR", 0.151073f);
        values.put("BM25_PAIR_EX", 0.151073f);
        values.put("BM25_PAIR_SY", 0.151073f);
        values.put("BM25_HI_REL", 0.382654f);
        values.put("BM25_HI_REL_EX", 0.382654f);
        values.put("BM25_HI_REL_SY", 0.382654f);
        values.put("BOCM", 0.609294f);
        values.put("BOCM_W", 0.706494f);
        values.put("BCLM", 0.565652f);
        values.put("SWBM25", 0.635638f);
        values.put("TOCM", 0.410797f);
        values.put("WCM_COVERAGE_MAX", 0f);
        values.put("PCM_MAX", 0f);
        values.put("BM15_STRICT_W1_K4", 0.606061f);
        values.put("BM15_ATTEN_W1_K4", 0.382594f);
        values.put("BM15_V0_W1_K4", 0.333333f);
        values.put("BCLM_HARD_W1_K1", 0.333331f);
        values.put("BCLM_HARD_W1_K2", 0.333306f);
        values.put("BCLM_WEIGHTED_K4", 0.71507f);
        values.put("BCLM_WEIGHTED_W1_K1", 0.666629f);
        values.put("BCLM_WEIGHTED_W1_K4", 0.635417f);
        values.put("BOCM_DOUBLE_K7", 0f);

        testFactorsInternal(wordWeights, positions, textSenLengths, values);
    }

    private void testFactorsInternal(
            float[] wordWeights,
            TFullPositionEx[] positions,
            int[] textSenLengths,
            Map<String, Float> factorValues
    ) {
        TWeightOfWords weights = new TWeightOfWords(wordWeights);
        TTextSearchFactorFiller factorFiller = new TTextSearchFactorFiller(weights, false);

        Object2FloatMap<ETextFactors> factors = new Object2FloatOpenHashMap<>();
        TDocumentHitsBuf textHits = new TDocumentHitsBuf(positions);

        factorFiller.FillHitsFactors(factors, textHits, textSenLengths);

        MutableBoolean equals = new MutableBoolean(true);

        factorValues.forEach(
                (name, value) -> {
                    double expected = value;
                    double actual = factors.get(ETextFactors.valueOf(name));
                    double delta = 1e-6;
                    double error = Math.abs(expected - actual);
                    if (error > delta) {
                        System.err.println("Wrong value of " + name + " factor ==>");
                        System.err.println("Expected :" + expected);
                        System.err.println("Actual   :" + actual);
                        System.err.println("Error    :" + error);
                        System.err.println();
                        equals.setFalse();
                    } else {
                        System.out.println(name + " factor OK");
                    }
                }
        );

        Assertions.assertTrue(equals.booleanValue());
    }

    @NotNull
    private TFullPositionEx buildFullPositionEx(int beg, int end, int wordIdx) {
        return new TFullPositionEx(new TFullPosition(TSignedPosting.Unpack(beg), TSignedPosting.Unpack(end)), wordIdx);
    }
}
