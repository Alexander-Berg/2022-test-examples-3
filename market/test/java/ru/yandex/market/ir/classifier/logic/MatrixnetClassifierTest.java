package ru.yandex.market.ir.classifier.logic;

//import ru.yandex.market.ir.classifier.features.ClassifierFeaturesCalculator;

/**
 * @author Evgeny Anisimoff <a href="mailto:anisimoff@yandex-team.ru"/>
 * @since {20:53}
 */
public class MatrixnetClassifierTest {
    // fixme: not compiled, missing classes
/*
    @Test
    public void testClassify()  {
        MatrixnetClassifier classifier = new MatrixnetClassifier();
        classifier.setConfigurationProvider(getConfigurationProviderForMatrixnet(simpleMatrixnetMock()));
        classifier.setRequestQualifier(requestQualifier());
        classifier.setRejectingProbabilityThreshold(0);

        List<ClassifiedOffer> classifiedOffers = classifier.classify(
                asList(requestForCategory(1), requestForCategory(2), requestForCategory(3)), 1);

        assertEquals(1, classifiedOffers.get(0).getCategoryId());
        assertEquals(2, classifiedOffers.get(1).getCategoryId());
        assertEquals(3, classifiedOffers.get(2).getCategoryId());
    }

    private PreparedRequest requestForCategory(int category) {
        PreparedRequest preparedRequest = mock(PreparedRequest.class);
        ClassificationRequest classificationRequest = mock(ClassificationRequest.class);
        when(classificationRequest.getCategoryId()).thenReturn(category);
        when(preparedRequest.getSource()).thenReturn(classificationRequest);
        when(preparedRequest.getAllowedCategories()).thenReturn(new IntOpenHashSet(asList(-1, 0, 1, 2, 3, 4, 5)));
        return preparedRequest;
    }

    private ConfigurationProvider getConfigurationProviderForMatrixnet(SimpleClassifier matrixnet) {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getWhiteFormula()).thenReturn(matrixnet);
        when(configuration.getDefaultCategory(false)).thenReturn(-1);
        ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
        when(configurationProvider.getConfiguration()).thenReturn(configuration);
        return configurationProvider;
    }

    private RequestQualifier requestQualifier() {
        RequestQualifier requestQualifier = new RequestQualifier();
        requestQualifier.setMaxCategoriesCount(10);
        requestQualifier.setCategoriesCalculators(asList(thisAndZeroProbableCategoryCalculator()));
        requestQualifier.setFeaturesBatchCalculator(featuresBatchCalculator());
        return requestQualifier;
    }

    private FeaturesBatchCalculator featuresBatchCalculator() {
        FeaturesBatchCalculator featuresBatchCalculator = new FeaturesBatchCalculator();
        featuresBatchCalculator.setFeaturesCalculators(asList(exactFeatureCalculator()));
        return featuresBatchCalculator;
    }

    @SuppressWarnings("unchecked")
    private SimpleClassifier simpleMatrixnetMock() {
        return new SimpleClassifier() {
            @Override
            public double[] classifyBatch(float[][] featuresMatrix) {
                double[] result = new double[featuresMatrix.length];
                for (int i = 0; i < featuresMatrix.length; i++) {
                    float[] featureList = featuresMatrix[i];
                    result[i] = featureList[0];
                }
                return result;
            }

            @Override
            public void close() throws IOException {

            }
        };
    }

    public ProbableCategoriesCalculator thisAndZeroProbableCategoryCalculator() {
        return new ProbableCategoriesCalculator() {
            @Override
            public ProbableCategories calcProbableCategories(PreparedRequest request) {
                ProbableCategories probableCategories = new ProbableCategories();
                probableCategories.addCategory(0, 0.5);
                probableCategories.addCategory(request.getSource().getCategoryId(), 0.5);
                return probableCategories;
            }
        };
    }

    public ClassifierFeaturesCalculator exactFeatureCalculator() {
        return new ClassifierFeaturesCalculator() {
            @Override
            public String[] getFeaturesNames() {
                return new String[]{"Coincidence"};
            }

            @Override
            public void calcFeatures(QualifiedRequest qualifiedRequest) {
                qualifiedRequest.addFeature(qualifiedRequest.getCategoryId() ==
                        qualifiedRequest.getPreparedRequest().getSource().getCategoryId() ? 1. : 0.);
            }
        };
    }

    @Test
    public void testCalculateFeatures() throws Exception {

    }
*/
}
