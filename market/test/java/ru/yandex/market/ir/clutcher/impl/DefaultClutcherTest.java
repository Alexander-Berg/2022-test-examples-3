package ru.yandex.market.ir.clutcher.impl;

import org.apache.log4j.Logger;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class DefaultClutcherTest {
    private static final Logger log = Logger.getLogger(DefaultClutcherTest.class);

//    public static final String[] EMPTY_PICS = new String[0];
//
//    private DefaultClutcher defaultClutcher;
//
//    @Before
//    public void setUp() {
//        BasicConfigurator.configure();
//        XmlCategoryDao categoryDao = new XmlCategoryDao();
////        categoryDao.setRootDirName("/home/shurk/projects/clutcher/xml-dumps/20121025");
////        categoryDao.setRootDirName("/home/shurk/projects/clutcher/xml-dumps/20131107_1510-umbrellas");
//        categoryDao.setClustersDumpsDir("/home/shurk/projects/clutcher/xml-dumps/20150224");
//        categoryDao.setGurulightDumpsDir("/home/shurk/projects/formalizer/xml-dumps/20150224/gurulight/");
//        defaultClutcher = new DefaultClutcher();
//        defaultClutcher.setCategoryDao(categoryDao);
////        defaultClutcher.reloadCategories();
//    }
//
//    /**
//     * MARKETCONTENT-49040
//     */
//    //@Test
//    public void testUmbrellas() {
//      /*  checkOfferIdClutching("2bbf081291ef9c453ee61a9c8a6e034e", 91267, 1096902830);
//
//        checkOfferAliasClutching(91267, 1008297397, 91267, "", "Ferrari 5000032100", "");
//        checkOfferAliasClutching(91267, 1008297397, 91267, "", "", "Ferrari 5000032100");
//
//        checkVendorAliasClutching(91267, 8340298, 91267, "", "Fulton", "");
//        checkVendorAliasClutching(91267, 8340298, 91267, "", "FULTON", "");
//        checkVendorAliasClutching(91267, 8340298, 91267, "", "Зонт FULTON L553-2415 Superslim-2 серый леопард, женский", "");*/
//    }
//
//    /*@Test
//    public void testGetLoadedCategoriesIds(){
//        int[] ids = defaultClutcher.getLoadedCategoriesIds();
//        for (int id : ids) {
//            System.out.println("id = " + id);
//        }
//    }*/
//
//    /**
//     * Test for <a href="https://jira.yandex-team.ru/browse/MBO-4504">MBO-4504</a>.
//     */
//    //@Test
//    public void testThrownOfferIdClutching() {
////        checkOfferIdClutching("071f2815576daf6120e22310edd35288", 7811898, 1004792482);
////        checkOfferIdClutching("f9e5093da26b797b7ca86e25b697dcea", 7811898, 1004792482);
////        checkOfferIdClutching("2a127945933f29047e72c054f09d74c1", 7811898, 1004792482);
//       /* checkOfferIdClutching("ab62f882799bc62877e36c0cb183e082", 7811898, 1004792482);
//        checkOfferIdClutching("d6a97625eae174e3b06816950ff006c2", 7811898, 1004792482);
//        checkOfferIdClutching("defbfab87748ff199bcd8ba8d182929c", 7811898, 1004792482);*/
//    }
//
//    //@Test
//    public void testOfferIdClutching() {
//        /*checkOfferIdClutching("5e6f715979b898c55e3d7d371a880f48", 12345, 2345);
//        checkOfferIdClutching("20be9af78be835705c0a47372b21af75", 12345, 2345);
//        checkOfferIdClutching("75365e5f9c7890177dce4be9a4bdbd0e", 12345, 2345);
//
//        checkOfferIdClutching("3a4b7e088b4ead5057cf6ebc7ac4123b", 12345, 2240);
//        checkOfferIdClutching("0df330d736edb63832ff5a0ec8e0ab7a", 12345, 2240);
//        checkOfferIdClutching("6ddc6790c5f5953ad5a6c3f57aeb304f", 12345, 2240);
//
//        checkOfferIdClutching("4d5abcb4f268afeb112feda1921b2ee7", 7812167, 1000004015);
//        checkOfferIdClutching("4295d3d32046f3d6d618bd42fb433729", 7812167, 1000003244);*/
//
////        checkOfferIdClutching("fdcb97ffc69752ced03cc70ef5daf411", 7812167, 1004792482);
//      /*  checkOfferIdClutching("627f7a12a2d03b6aa900fb384cfef69f", 7812167, 1003139092);
//        checkOfferIdClutching("687d8815938a88f33df01b88c8a4db36", 7812167, 1005852437);
//        checkOfferIdClutching("2f2c7a1d2bdce4f771bd5ef0df6269da", 7812167, 1005852437);
//        checkOfferIdClutching("02aa46a282ce8e7ff71101d6a2912f76", 7812167, 1003826922);*/
//    }
//
//    //@Test
//    public void testOfferIdClutching2() {
//        final int hid = 1234567;
//        final long vendorId = 5678;
//        final Vendor vendor = new Vendor(vendorId, "Adidas");
//        vendor.getClusters().add(buildCluster(vendor.getId(), 10050011, "", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
//        vendor.getClusters().add(buildCluster(vendor.getId(), 10050022, "", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
//        vendor.getClusters().add(buildCluster(vendor.getId(), 10050033, "", "cccccccccccccccccccccccccccccccc"));
//        vendor.getClusters().add(buildCluster(vendor.getId(), 10050044, "", "dddddddddddddddddddddddddddddddd"));
//        vendor.getClusters().add(buildCluster(vendor.getId(), 10050055, "", generaterandomStringIds(15000000)));
//
//        final DefaultClutcher clutcher = buildSynthClutcher(buildCategory(hid, vendor));
//        checkOfferIdClutching(hid, vendorId, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 10050011, clutcher);
//        checkOfferIdClutching(hid, vendorId, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", 10050022, clutcher);
//        checkOfferIdClutching(hid, vendorId, "cccccccccccccccccccccccccccccccc", 10050033, clutcher);
//        checkOfferIdClutching(hid, vendorId, "dddddddddddddddddddddddddddddddd", 10050044, clutcher);
//    }
//
//    private List<String> generaterandomStringIds(final int count) {
//        final Random random = new Random();
//        final List<String> result = new ArrayList<>(count);
//        for (int i = 0; i < count; ++i) {
//            result.add(String.format("%016x%016x", random.nextLong(), random.nextLong()));
//        }
//        return result;
//    }
//
//    private void checkOfferIdClutching(String offerId, int hid, int expectedClusterId) {
//        checkOfferIdClutching(offerId, hid, expectedClusterId, this.defaultClutcher);
//    }
//
//    private void checkOfferIdClutching(String offerId, int hid, int expectedClusterId, DefaultClutcher clutcher) {
//        final long vendorId = 0;
//        checkOfferIdClutching(hid, vendorId, offerId, expectedClusterId, clutcher);
//    }
//
//    private void checkOfferIdClutching(int hid, long vendorId, String offerId, int expectedClusterId,
//                                       DefaultClutcher clutcher) {
//        DefaultClutcher.ClutchResult clutch = clutcher.getClutch(hid, offerId, "bubu", "bobo", "", "", vendorId);
//        assertEquals(hid, clutch.getHid());
//        assertEquals(Clutcher.ClutchType.OFFER_ID_CLUTCH_OK_VALUE, clutch.getClutchType());
//
//        if (expectedClusterId != clutch.getClusterId()) {
//            System.out.println("expectedClusterId = " + expectedClusterId);
//            System.out.println("clutch.getClusterId() = " + clutch.getClusterId());
//            System.out.println();
//        }
//
//        assertEquals(expectedClusterId, clutch.getClusterId());
//    }
//
//    //@Test
//    public void testAliasClutching() {
//        /*checkAliasClutching("Adidas bubu SF3204E3RO5 bobo", 12345, 2345);
//        checkAliasClutching("Adidas bubu SF3204E3RO6 bobo", 12345, 2345);
//        checkAliasClutching("Adidas bubu SF3204E3RO7 bobo", 12345, 2345);
//
//        checkAliasClutching("Abibas bubu AF3204E3RO8 bobo", 12345, 2240);
//        checkAliasClutching("Abibas bubu AF3204E3RO9 bobo", 12345, 2240);
//        checkAliasClutching("Abibas bubu AF3204E3RO1 bobo", 12345, 2240);*/
//
////        checkAliasClutching("Шапка Canoe 3440857", 7812167, 1000003244);
////        checkAliasClutching("Quiksilver AW302", 7812167, 1000003137);
//
//        /*checkAliasClutching("IcePeak CY888", 7812167, 1004792791);
//        checkAliasClutching("IcePeak CY889", 7812167, 1004792790);
//        checkAliasClutching("Бейсболка Ducati A070612064", 7812167, 1003140082);
//        checkAliasClutching("Бейсболка Ducati A080612076", 7812167, 1003140084);*/
//    }
//
//    private void checkAliasClutching(String text, int hid, int expectedClusterId) {
//        checkOfferAliasClutching(hid, expectedClusterId, hid, "brbrbr", text, "bobo");
//        checkOfferAliasClutching(hid, expectedClusterId, hid, "brbrbr", "bobo", text);
//    }
//
//    private void checkOfferAliasClutching(int expectedHid, int expectedClusterId,
//                                          int hid, String offerId, String title, String description) {
//        final DefaultClutcher clutcher = this.defaultClutcher;
//        checkOfferAliasClutching(expectedHid, expectedClusterId, hid, offerId, title, description, clutcher);
//    }
//
//    private void checkOfferAliasClutching(int expectedHid, int expectedClusterId, int hid,
//                                          String offerId, String title, String description, DefaultClutcher clutcher) {
//        DefaultClutcher.ClutchResult clutch = clutcher.getClutch(hid, offerId, title, description, "", "", 0);
//        assertEquals(expectedHid, clutch.getHid());
//        assertEquals(Clutcher.ClutchType.ALIAS_CLUTCH_OK_VALUE, clutch.getClutchType());
//        assertEquals(expectedClusterId, clutch.getClusterId());
//    }
//
//    private void checkVendorAliasClutching(int expectedHid, long expectedVendorId,
//                                           int hid, String offerId, String title, String description) {
//        DefaultClutcher.ClutchResult clutch = defaultClutcher.getClutch(hid, offerId, title, description, "", "", 0);
//        assertEquals(expectedHid, clutch.getHid());
//        assertEquals(Clutcher.ClutchType.VENDOR_CLUTCH_VALUE, clutch.getClutchType());
//        assertEquals(expectedVendorId, clutch.getVendorId());
//    }
//
//    /*public static DefaultClutcher prepareClutcher() {
//        XmlCategoryDao categoryDao = new XmlCategoryDao();
//        categoryDao.setBooksSupported(booksSupported);
//        categoryDao.setOriginalAliases(originalAliases);
//        categoryDao.setDirPath(PATH_PREFIX);
//
//        DefaultMatcher matcher = new DefaultMatcher();
//        matcher.setCategoryDao(categoryDao);
//        try {
//            Field aliveField = DefaultMatcher.class.getDeclaredField("alive");
//            aliveField.setAccessible(true);
//            aliveField.setBoolean(matcher, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return matcher;
//    }*/
//
//    private ClutcherServiceStub startHttpServer() {
//        ClutcherServiceImpl impl = new ClutcherServiceImpl();
//        impl.setClutcher(defaultClutcher);
//        ServiceServer clutcherHttpServer = new ServiceServer();
//        clutcherHttpServer.setPort(8080);
//        clutcherHttpServer.setMaxIdleTimeout(30000);
//        clutcherHttpServer.setMaxThreads(10);
//        clutcherHttpServer.setHandler(new ClutcherServiceHandler(impl));
//        try {
//            clutcherHttpServer.start();
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
//
//        ClutcherServiceStub client = new ClutcherServiceStub();
//        client.setHost("http://localhost:8080/");
//        log.debug("ping = " + client.ping());
//        return client;
//    }
//
//    //@Test
//    public void testClutchingViaHttp() {
//        ClutcherServiceStub client = startHttpServer();
//        checkClutching(client, "627f7a12a2d03b6aa900fb384cfef69f", "", 7812167, 8336817, 1003139092);
//        checkClutching(client, "no-offer-id", "IcePeak CY889", 7812167, 8338781, 1004792790);
//    }
//
//    private void checkClutching(ClutcherServiceStub client, String offerId, String title, int hid,
//                                int expectedVendorId, int expectedClusterId) {
//        Clutcher.ResultsSequence results = client.clutchBatch(
//            Clutcher.OffersSequence.newBuilder().addOffer(
//                Clutcher.Offer.newBuilder()
//                    .setHid(hid)
//                    .setOfferId(offerId)
//                    .setOffer(title)
//                    .build()
//            ).build()
//        );
//        Clutcher.ClutchResult result = results.getResult(0);
//        assertEquals(expectedClusterId, result.getClusterId());
//        assertEquals(expectedVendorId, result.getVendorId());
//    }
//
//    private DefaultClutcher buildSynthClutcher(Category category) {
//        final int vendorParamId = 7893318;
//        final ClutcherKnowledge knowledge = new ClutcherKnowledge(category, vendorParamId, 0, "");
//        return buildSynthClutcher(knowledge);
//    }
//
//    private DefaultClutcher buildSynthClutcher(final ClutcherKnowledge knowledge) {
//        final DefaultClutcher clutcher = new DefaultClutcher();
//        clutcher.setCategoryDao(buildSynthDao(knowledge));
//        clutcher.reloadCategories();
//        return clutcher;
//    }
//
//    private CategoryDao buildSynthDao(final ClutcherKnowledge knowledge) {
//        return new CategoryDao() {
//            @Override
//            public IntList loadCategoryIds() {
//                return IntLists.singleton(knowledge.getHid());
//            }
//
//            @Override
//            public ClutcherKnowledge reloadCategory(ClutcherKnowledge oldCategory) {
//                return oldCategory;
//            }
//
//            @Override
//            public ClutcherKnowledge loadCategory(int hid) {
//                return knowledge;
//            }
//        };
//    }
//
//    private Category buildCategory(int hid, Vendor vendor) {
//        final Category category = new Category(hid, "Some synth category");
//        category.getVendors().add(vendor);
//        return category;
//    }
//
//    private Cluster buildCluster(long vendorId, long clusterId, String clusterName, String offerId) {
//        final Cluster clusterD = new Cluster(clusterId, clusterName, vendorId);
//        clusterD.offerIds = Collections.singleton(offerId);
//        return clusterD;
//    }
//
//    private Cluster buildCluster(long vendorId, long clusterId, String clusterName, List<String> idsList) {
//        final Cluster clusterD = new Cluster(clusterId, clusterName, vendorId);
//        clusterD.offerIds = new HashSet<>(idsList);
//        return clusterD;
//    }
}
