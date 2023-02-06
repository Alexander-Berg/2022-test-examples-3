package ru.yandex.so.dssm.applier;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.number.OrderingComparison;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.jniwrapper.JniWrapper;
import ru.yandex.jniwrapper.JniWrapperConfigBuilder;
import ru.yandex.jniwrapper.ThreadSafeJniWrapper;
import ru.yandex.test.util.TestBase;

public class DssmApplierTest extends TestBase {
    private static final double EPSILON = 0.01d;
    private static final double MIN_DIFF = 0.05d;
    private static final double CLOSE_THRESHOLD = 0.75d;

    private final DssmApplier applier;
    private final DssmApplier applierUC;
    private final DssmApplier subjectPredictor;
    private final JniWrapper deobfuscator;

    public DssmApplierTest() throws Exception {
        super(false, 0L);

        applier = new DssmApplier(
            Paths.getSandboxResourcesRoot() + "/model.dssm",
            "query_embedding",
            true,
            true);
        applierUC = new DssmApplier(
            Paths.getSandboxResourcesRoot() + "/0_upload_files/model.dssm",
            "query_embedding",
            true,
            true);
        subjectPredictor = new DssmApplier(
            Paths.getSandboxResourcesRoot() + "/DssmAllCleanWithAttachments",
            "query_embedding",
            false,
            true);

        deobfuscator =
            new ThreadSafeJniWrapper(
                new JniWrapperConfigBuilder()
                    .libraryName(
                        Paths.getBuildPath(
                            "mail/so/libs/deobfuscator_jniwrapper/jniwrapper"
                            + "/libdeobfuscator-jniwrapper.so"))
                    .ctorName("JniWrapperCreateDeobfuscator")
                    .dtorName("JniWrapperDestroyDeobfuscator")
                    .freeName("JniWrapperFree")
                    .mainName("JniWrapperDeobfuscateText")
                    .config(
                        "<Deobfuscator>\n"
                        + "\tRemapPath: rus.dict/deobfuscator/remap.json\n"
                        + "\tTriePath: rus.dict/deobfuscator/dict.trie\n"
                        + "</Deobfuscator>\n"
                        + "<Deobfuscator>\n"
                        + "\tRemapPath: en.dict/deobfuscator/remap.json\n"
                        + "\tTriePath: en.dict/deobfuscator/dict.trie\n"
                        + "</Deobfuscator>\n")
                    .build());
    }

    private String deobfuscate(final String str) throws Exception {
        return deobfuscator.apply(str, null).process(null, 0, 0);
    }

    private String mkinput(
        final String subject,
        final String fromname,
        final String fromaddr)
        throws Exception
    {
        return "{\"subject\":\"" + subject
            + "\",\"fromname\":\"" + fromname
            + "\",\"fromaddr\":\"" + fromaddr
            + "\",\"raw_fromaddr\":\"" + fromaddr
            + "\",\"normalized_subject\":\"" + deobfuscate(subject)
            + "\"}";
    }

    private static double dotProduct(
        final float[] f1,
        final float[] f2)
    {
        Assert.assertEquals(f1.length, f2.length);
        double sum = 0;
        for (int i = 0; i < f1.length; ++i) {
            sum += ((double) f1[i]) * f2[i];
        }
        return sum;
    }

    private void testSelfProduct(final DssmApplier applier) throws Exception {
        float[] f1 = applier.apply(
            mkinput("Привет, мир", "Dmitry Potapov", "analizer@yandex.ru"));
        MatcherAssert.assertThat(
            dotProduct(f1, f1),
            OrderingComparison.greaterThan(1d - EPSILON));
        float[] f2 = applier.apply(
            mkinput("привет мир", "Dmitry Potapov", "analizer@yandex.ru"));
        MatcherAssert.assertThat(
            dotProduct(f1, f2),
            OrderingComparison.greaterThan(1d - EPSILON));
    }

    @Test
    public void testSelfProduct() throws Exception {
        testSelfProduct(applier);
        testSelfProduct(applierUC);
    }

    private void testCloseSubjects(final DssmApplier applier)
        throws Exception
    {
        List<float[]> f1 =
            applier.applyBatch(
                Arrays.asList(
                    mkinput(
                        "Привет, мир",
                        "Dmitry Potapov",
                        "analizer@yandex.ru"),
                    mkinput(
                        "Здравствуй, мир",
                        "Dmitry Potapov",
                        "analizer@yandex.ru")));
        Assert.assertEquals(2, f1.size());
        double dotProduct = dotProduct(f1.get(0), f1.get(1));
        logger.info("Dot product is: " + dotProduct);
        MatcherAssert.assertThat(
            dotProduct,
            OrderingComparison.greaterThan(CLOSE_THRESHOLD));
        MatcherAssert.assertThat(
            dotProduct,
            OrderingComparison.lessThan(1d - MIN_DIFF));

        List<float[]> f2 =
            applier.applyBatch(
                new StringReader(
                    mkinput(
                        "Привет, мир",
                        "Dmitry Potapov",
                        "analizer@yandex.ru")
                    + '\n'
                    + mkinput(
                        "Здравствуй, мир",
                        "Dmitry Potapov",
                        "analizer@yandex.ru")));
        Assert.assertEquals(2, f2.size());
        for (int i = 0; i < f1.size(); ++i) {
            Assert.assertArrayEquals(f1.get(i), f2.get(i), (float) EPSILON);
        }
    }

    @Test
    public void testCloseSubjects() throws Exception {
        testCloseSubjects(applier);
        testCloseSubjects(applierUC);
    }

    private void testFarSubjects(final DssmApplier applier) throws Exception {
        float[] f1 = applier.apply(
            mkinput("Привет, мир", "Dmitry Potapov", "analizer@yandex.ru"));
        float[] f2 = applier.apply(
            mkinput("Дивизия зомби", "Игорь Фёдорович", "igor@go.su"));
        double dotProduct = dotProduct(f1, f2);
        logger.info("Dot product is: " + dotProduct);
        logger.info("Layer size: " + f1.length);
        MatcherAssert.assertThat(
            dotProduct,
            OrderingComparison.lessThan(CLOSE_THRESHOLD));
    }

    @Test
    public void testFarSubjects() throws Exception {
        testFarSubjects(applier);
        testFarSubjects(applierUC);
    }

    private static String mkfullbody(
        final String subject,
        final String body,
        final String attachments)
    {
        return "{\"body\":\"" + body
            + "\",\"attachments\":\"" + attachments
            + "\",\"subject\":\"" + subject
            + "\",\"language\":\"\"}";
    }

    @Test
    public void testFullBody() throws Exception {
        float[] f1 =
            subjectPredictor.apply(
                mkfullbody(
                    "Fwd: Ордер поощрения по идентификатору TDEL_8334 одобрен",
                    "Здравствуйте!\\nВсвязи с интернет уязвимостью, у нашего "
                    + "агента не получилось продолжить перевод "
                    + "# UPWOFZZ102581.\\nСогласовать номер счета »",
                    ""));
        float[] f2 =
            subjectPredictor.apply(
                mkfullbody(
                    "Fwd: Запрос поощрения по номеру DKCC_8614 был проверен",
                    "Приветствуем!\\nВсвязи с неясной помехой, наш оператор не"
                    + " смог сделать выплату ID NFRLWZD_629919.\\n"
                    + "Указать номер счета",
                    ""));
        float[] f3 =
            subjectPredictor.apply(
                mkfullbody(
                    "Ваш заказ: №J-695768-5991 уже обрабатывается.",
                    "Здравствуйте, !\\n"
                    + "Прекрасный выбор! Ваш заказ №J-695768-5991 уже "
                    + "обрабатывается. Когда он поступит в магазин, "
                    + "вам придёт SMS.\\n"
                    + "Благодарим за покупку в нашем магазине!\\n"
                    + "Ваша карта уже активирована сегодня "
                    + "23.07.2020/9:38:45",
                    ""));

        double dotProduct = dotProduct(f1, f2);
        double dotProduct2 = dotProduct(f1, f3);
        logger.info("Vector size: " + f1.length);
        logger.info("Dot product spi-spi: " + dotProduct);
        logger.info("Dot product spi-flow: " + dotProduct2);
        MatcherAssert.assertThat(
            dotProduct,
            OrderingComparison.greaterThan(0.7));

        MatcherAssert.assertThat(
            dotProduct2,
            OrderingComparison.lessThan(0.5));
    }
}

