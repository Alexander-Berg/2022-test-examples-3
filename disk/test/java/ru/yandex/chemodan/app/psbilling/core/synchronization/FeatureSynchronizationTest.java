package ru.yandex.chemodan.app.psbilling.core.synchronization;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Repeat;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractGroupServicesTest;
import ru.yandex.chemodan.app.psbilling.core.dao.features.ServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.entities.features.FeatureWithOwner;
import ru.yandex.chemodan.app.psbilling.core.entities.features.IssuedFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.util.RequestTemplate;
import ru.yandex.chemodan.test.ConcurrentTestUtils;
import ru.yandex.inside.passport.tvm2.TvmHeaders;
import ru.yandex.misc.test.Assert;

public abstract class FeatureSynchronizationTest<
        TOwner,
        TIssuedFeature extends IssuedFeature<TOwner>,
        TInsertData extends ServiceFeatureDao.InsertData<TOwner, TIssuedFeature>,
        TFeatureWithOwner extends FeatureWithOwner<TOwner>,
        TServiceFeatureDao extends ServiceFeatureDao<TIssuedFeature, TOwner, TInsertData, TFeatureWithOwner>>
        extends AbstractGroupServicesTest {
    @Autowired
    protected ProductFeatureDao productFeatureDao;
    @Autowired
    protected ProductTemplateFeatureDao productTemplateFeatureDao;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected TServiceFeatureDao serviceFeatureDao;

    protected abstract TIssuedFeature createFeature(TOwner owner, FeatureEntity featureEntity);

    protected abstract TIssuedFeature createFeature(TOwner owner, FeatureEntity featureEntity, int amount);

    protected abstract TIssuedFeature createFeature(ProductFeatureEntity productFeatureEntity);

    protected abstract ProductFeatureEntity createProductFeature(FeatureEntity featureEntity, int amount);

    private TIssuedFeature createFeature(FeatureEntity featureEntity) {
        return createFeature(getOwner(), featureEntity);
    }

    private TIssuedFeature createFeature(FeatureEntity featureEntity, int amount) {
        return createFeature(getOwner(), featureEntity, amount);
    }

    protected abstract void synchronize(TOwner owner, FeatureEntity feature);

    protected abstract String getSentIdPattern();

    protected abstract String getSentIdValue(TIssuedFeature issuedFeature);

    protected abstract TIssuedFeature refresh(TIssuedFeature issuedFeature);

    protected abstract TOwner getOwner();

    protected abstract TOwner getSecondOwner();

    @Test
    public void shouldActivateFeature() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
        );
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));

        TIssuedFeature serviceFeature = createFeature(feature);
        synchronize(serviceFeature.getOwnerId(), feature);
        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature));

        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldSendBody() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern(),
                        "{'request': 'do it'}"))
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/amount/" + getSentIdPattern(),
                        "{'request': 'change it'}"))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivate/" + getSentIdPattern(),
                        "{'request': 'remove it'}"))
                .callSetAmountOnDeactivation(false)
        );
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/amount/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/deactivate/"));

        TIssuedFeature serviceFeature = createFeature(feature);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature),
                "{'request': 'do it'}");
        verifyExchangeForUrl("http://some.yandex.ru/amount/" + getSentIdValue(serviceFeature),
                "{'request': 'change it'}");

        serviceFeatureDao.setTargetState(Cf.list(serviceFeature.getId()), Target.DISABLED);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/deactivate/" + getSentIdValue(serviceFeature),
                "{'request': 'remove it'}");

        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldActivateFeatureForTwoOwners() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
        );
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));

        TIssuedFeature firstServiceFeature = createFeature(getOwner(), feature);
        TIssuedFeature secondServiceFeature = createFeature(getSecondOwner(), feature);

        synchronize(firstServiceFeature.getOwnerId(), feature);
        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(firstServiceFeature));
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
        assertIsActual(firstServiceFeature);
        assertNotActual(secondServiceFeature);

        synchronize(secondServiceFeature.getOwnerId(), feature);
        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(secondServiceFeature));
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldSetAmountInActivationTemplateOnActivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded(
                        "http://some.yandex.ru/activate/" + getSentIdPattern() + "/#{amount}"))
        );

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));

        TIssuedFeature serviceFeature = createFeature(feature, 20);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature) + "/20");
        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldNotSetAmountForFeatureOnActivationIfSpecified() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(
                        postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern() + "/#{amount}"))
                .setAmountRequestTemplate(
                        postUrlEncoded("http://some.yandex.ru/setAmount/" + getSentIdPattern() + "/#{amount}"))
                .callSetAmountOnActivation(false)
        );

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));

        TIssuedFeature serviceFeature = createFeature(feature, 20);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature) + "/20");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldSetAmountForFeatureOnActivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(
                        postUrlEncoded("http://some.yandex.ru/setAmount/" + getSentIdPattern() + "/#{amount}"))
        );

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));

        TIssuedFeature serviceFeature = createFeature(feature, 20);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/" + getSentIdValue(serviceFeature) + "/20");
        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldUseContextOnSetAmount() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
                .setAmountRequestTemplate(
                        postUrlEncoded("http://some.yandex.ru/setAmount/#{context['wrapper']['id']}/#{amount}"))
        );

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"), "{\"wrapper\": { \"id\": 1234567 } }");
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));

        TIssuedFeature serviceFeature = createFeature(feature, 20);
        synchronize(serviceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature));
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/1234567/20");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldSetDoubleAmountOnActivationOfSecondInstance() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/50");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldUseContextOnSecondSetAmount() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
                .setAmountRequestTemplate(
                        postUrlEncoded("http://some.yandex.ru/setAmount/#{context['wrapper']['id']}/#{amount}"))
        );
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));

        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        mockPostOk(Mockito.eq("http://some.yandex.ru/activate/" + getSentIdValue(firstServiceFeature)),
                "{\"wrapper\": { \"id\": 1234567 } }");
        synchronize(firstServiceFeature.getOwnerId(), feature);

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);
        synchronize(secondServiceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(secondServiceFeature));
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/1234567/20");
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/1234567/50");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
    }

    @Test
    public void shouldSetAmountIfSecondInstanceInDeactivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setTargetState(firstServiceFeature.getId(), Target.DISABLED);

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(firstServiceFeature.getOwnerId(), feature);
        synchronize(secondServiceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/30");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldSetAmountAfterDeactivationOfSecondInstance() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);
        serviceFeatureDao.setStatusActual(secondServiceFeature.getId(), secondServiceFeature.getTarget());
        serviceFeatureDao.setTargetState(secondServiceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(firstServiceFeature.getOwnerId(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/20");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldSetAmountAfterDeactivationOfSecondInstanceWithSameProduct() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        ProductFeatureEntity productFeature = createProductFeature(feature, 20);

        TIssuedFeature firstServiceFeature = createFeature(productFeature);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(productFeature);
        serviceFeatureDao.setStatusActual(secondServiceFeature.getId(), secondServiceFeature.getTarget());
        serviceFeatureDao.setTargetState(secondServiceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/20");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldNotCallDeactivationTemplateAfterDeactivationOfSecondInstance() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivation/"))
        );
        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);
        serviceFeatureDao.setStatusActual(secondServiceFeature.getId(), secondServiceFeature.getTarget());
        serviceFeatureDao.setTargetState(secondServiceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(getOwner(), feature);

        Mockito.verify(skipTransactionsExport, Mockito.never()).exchange(
                Mockito.eq("http://some.yandex.ru/deactivation/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        );

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldCallSetAmountWithZeroOnDeactivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        TIssuedFeature serviceFeature = createFeature(feature, 30);
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));

        synchronize(getOwner(), feature);
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/30");

        serviceFeatureDao.setTargetState(serviceFeature.getId(), Target.DISABLED);
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/setAmount/0");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldCallNotSetAmountWithZeroOnDeactivationIfSpecified() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/" + getSentIdPattern() +
                        "/#{amount}"))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivate/" + getSentIdPattern()))
                .callSetAmountOnDeactivation(false)
        );
        TIssuedFeature serviceFeature = createFeature(feature, 30);
        serviceFeatureDao.setStatusActual(serviceFeature.getId(), serviceFeature.getTarget());
        serviceFeatureDao.setTargetState(serviceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/deactivate/"));
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/deactivate/" + getSentIdValue(serviceFeature));
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldCallDeactivationTemplateOnDeactivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activation/" + getSentIdPattern()))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivation/#{context['id']}"))
        );
        TIssuedFeature serviceFeature = createFeature(feature, 30);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activation/" + getSentIdValue(serviceFeature)), "{ " +
                "\"id\": 1234567 }");
        synchronize(getOwner(), feature);

        serviceFeatureDao.setTargetState(serviceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/deactivation/"));
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activation/" + getSentIdValue(serviceFeature));
        verifyExchangeForUrl("http://some.yandex.ru/deactivation/1234567");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(serviceFeature);
    }

    @Test
    public void shouldNotCallActivationTemplateOnActivationOfSecondInstance() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate"))
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(feature, 20);

        mockPostOk(Mockito.eq("http://some.yandex.ru/activate"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount"));
        synchronize(getOwner(), feature);

        Mockito.verify(skipTransactionsExport, Mockito.never()).exchange(
                Mockito.eq("http://some.yandex.ru/activate"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        );

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldHandleTwoActivations() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate"))
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
        );
        mockPostOk(Mockito.eq("http://some.yandex.ru/activate"));

        createFeature(feature, 20);
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(getOwner(), feature);

        TIssuedFeature secondServiceFeature = createFeature(feature, 20);
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate");
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/20");
        verifyExchangeForUrl("http://some.yandex.ru/setAmount/40");
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);

        assertIsActual(secondServiceFeature);
    }

    @Test
    public void shouldUseSameUniqKeyOnActivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/#{uniq}"))
        );

        ListF<String> uniqKeys = Cf.arrayList();
        AtomicInteger activateCounts = new AtomicInteger(0);

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/activate/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            activateCounts.incrementAndGet();
            String url = invocation.getArgument(0);
            uniqKeys.add(url.substring("http://some.yandex.ru/activate/".length()));
            if (activateCounts.get() == 1) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>("", HttpStatus.OK);
        });

        createFeature(feature, 20);
        Assert.assertThrows(
                () -> synchronize(getOwner(), feature),
                HttpStatusCodeException.class
        );

        synchronize(getOwner(), feature);

        Assert.equals(2, activateCounts.get());
        Assert.equals(uniqKeys.get(0), uniqKeys.get(1));
    }

    @Test
    public void shouldUseSameUniqKeyOnActivationIfHasErrorOnSetAmount() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/#{uniq}"))
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{context['id']}"))
        );

        ListF<String> uniqKeys = Cf.arrayList();

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/activate/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            String url = invocation.getArgument(0);
            uniqKeys.add(url.substring("http://some.yandex.ru/activate/".length()));
            return new ResponseEntity<>("{\"id\": 123456}", HttpStatus.OK);
        });


        createFeature(feature, 20);

        mockPost(Mockito.eq("http://some.yandex.ru/setAmount/123456"), "", HttpStatus.INTERNAL_SERVER_ERROR);
        Assert.assertThrows(
                () -> synchronize(getOwner(), feature),
                HttpStatusCodeException.class
        );

        mockPostOk(Mockito.eq("http://some.yandex.ru/setAmount/123456"));
        synchronize(getOwner(), feature);

        Assert.equals(uniqKeys.get(0), uniqKeys.get(1));
    }

    @Test
    public void shouldUseAnotherUniqAfterDeactivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/#{uniq}"))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivation/#{context['id']}"))
        );

        ListF<String> uniqKeys = Cf.arrayList();

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/activate/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            String url = invocation.getArgument(0);
            uniqKeys.add(url.substring("http://some.yandex.ru/activate/".length()));
            return new ResponseEntity<>("{\"id\": 123456}", HttpStatus.OK);
        });


        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        synchronize(getOwner(), feature);

        serviceFeatureDao.setTargetState(firstServiceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.eq("http://some.yandex.ru/deactivation/123456"));
        synchronize(getOwner(), feature);

        createFeature(feature, 20);
        synchronize(getOwner(), feature);

        Assert.sizeIs(2, uniqKeys);
        Assert.notEquals(uniqKeys.get(0), uniqKeys.get(1));
    }

    @Test
    public void shouldSendTvmTicket() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/"))
                .systemTvmId(Option.of(123))
        );

        mockPostOk(Mockito.eq("http://some.yandex.ru/activate/"));

        createFeature(feature, 20);
        synchronize(getOwner(), feature);

        Mockito.verify(skipTransactionsExport).exchange(
                Mockito.eq("http://some.yandex.ru/activate/"), Mockito.eq(HttpMethod.POST),
                Mockito.argThat(request -> request.getHeaders().containsKey(TvmHeaders.SERVICE_TICKET)),
                Mockito.eq(String.class)
        );
    }

    @Test
    public void testTemplateRnd() {
        String template = "https://settings.mail.yandex.net/?uniq_id=#{rnd}";

        FeatureEntity feature = psBillingProductsFactory.createFeature(
                FeatureType.ADDITIVE,
                b -> b.activationRequestTemplate(postUrlEncoded(template))
        );
        createFeature(feature, 20);
        synchronize(getOwner(), feature);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(skipTransactionsExport).exchange(
                urlCaptor.capture(), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        );
        Assert.isTrue(urlCaptor.getValue().matches("[^?]+\\?uniq_id=[\\p{Alnum}]+"));
    }

    @Repeat(5)
    @Test
    public void testParallelDeactivationOfServiceAndSynchronization() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
                .systemTvmId(Option.of(123))
        );

        TIssuedFeature firstServiceFeature = createFeature(feature, 20);
        serviceFeatureDao.setStatusActual(firstServiceFeature.getId(), firstServiceFeature.getTarget());

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);

        ListF<BigDecimal> sendAmounts = Cf.arrayList();

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/setAmount/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            String url = invocation.getArgument(0);
            sendAmounts.add(new BigDecimal(url.substring("http://some.yandex.ru/setAmount/".length())));
            return new ResponseEntity<>("", HttpStatus.OK);
        });


        ConcurrentTestUtils.testConcurrency(Cf.list(
                () -> serviceFeatureDao.setTargetState(firstServiceFeature.getId(), Target.DISABLED),
                () -> synchronize(getOwner(), feature)
        ));

        synchronize(getOwner(), feature);

        assertIsActual(firstServiceFeature);
        assertTarget(firstServiceFeature, Target.DISABLED);
        Assert.isTrue(hasActualEnabledAt(firstServiceFeature));
        Assert.isTrue(hasActualDisabledAt(firstServiceFeature));
        assertIsActual(secondServiceFeature);
        assertTarget(secondServiceFeature, Target.ENABLED);
        Assert.isTrue(hasActualEnabledAt(secondServiceFeature));
        Assert.isFalse(hasActualDisabledAt(secondServiceFeature));

        Assert.isTrue(
                (sendAmounts.size() == 2
                        && sendAmounts.get(0).compareTo(BigDecimal.valueOf(50)) == 0
                        && sendAmounts.get(1).compareTo(BigDecimal.valueOf(30)) == 0
                ) || (sendAmounts.size() == 1
                        && sendAmounts.get(0).compareTo(BigDecimal.valueOf(30)) == 0
                ),
                sendAmounts.toString()
        );
    }

    @Repeat(5)
    @Test
    public void testParallelDeactivationOfNotActualServiceAndSynchronization() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
                .systemTvmId(Option.of(123))
        );

        TIssuedFeature firstServiceFeature = createFeature(feature, 20);

        TIssuedFeature secondServiceFeature = createFeature(feature, 30);

        ListF<BigDecimal> sendAmounts = Cf.arrayList();

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/setAmount/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            String url = invocation.getArgument(0);
            sendAmounts.add(new BigDecimal(url.substring("http://some.yandex.ru/setAmount/".length())));
            return new ResponseEntity<>("", HttpStatus.OK);
        });


        ConcurrentTestUtils.testConcurrency(Cf.list(
                () -> serviceFeatureDao.setTargetState(firstServiceFeature.getId(), Target.DISABLED),
                () -> synchronize(getOwner(), feature)
        ));

        synchronize(getOwner(), feature);

        assertIsActual(firstServiceFeature);
        assertTarget(firstServiceFeature, Target.DISABLED);
        Assert.isTrue(hasActualDisabledAt(firstServiceFeature));

        assertIsActual(secondServiceFeature);
        assertTarget(secondServiceFeature, Target.ENABLED);

        Assert.isTrue(
                (sendAmounts.size() == 2
                        && sendAmounts.get(0).compareTo(BigDecimal.valueOf(50)) == 0
                        && sendAmounts.get(1).compareTo(BigDecimal.valueOf(30)) == 0
                        && hasActualEnabledAt(firstServiceFeature)
                ) || (sendAmounts.size() == 1
                        && sendAmounts.get(0).compareTo(BigDecimal.valueOf(30)) == 0
                        && !hasActualEnabledAt(firstServiceFeature)
                ),
                sendAmounts.toString() + serviceFeatureDao.findById(firstServiceFeature.getId())
        );
    }

    @Repeat(5)
    @Test
    public void testParallelDeactivationAndSynchronization() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{amount}"))
                .systemTvmId(Option.of(123))
        );

        TIssuedFeature serviceFeature = createFeature(feature, 20);

        ListF<BigDecimal> sendAmounts = Cf.arrayList();

        Mockito.when(skipTransactionsExport.exchange(
                Mockito.startsWith("http://some.yandex.ru/setAmount/"), Mockito.eq(HttpMethod.POST),
                Mockito.any(), Mockito.eq(String.class)
        )).then(invocation -> {
            String url = invocation.getArgument(0);
            sendAmounts.add(new BigDecimal(url.substring("http://some.yandex.ru/setAmount/".length())));
            return new ResponseEntity<>("", HttpStatus.OK);
        });


        ConcurrentTestUtils.testConcurrency(Cf.list(
                () -> serviceFeatureDao.setTargetState(serviceFeature.getId(), Target.DISABLED),
                () -> synchronize(getOwner(), feature)
        ));

        synchronize(getOwner(), feature);

        assertIsActual(serviceFeature);
        assertTarget(serviceFeature, Target.DISABLED);
        Assert.isTrue(hasActualDisabledAt(serviceFeature));

        Assert.isTrue(
                (sendAmounts.size() == 2
                        && sendAmounts.get(0).compareTo(BigDecimal.valueOf(20)) == 0
                        && sendAmounts.get(1).compareTo(BigDecimal.valueOf(0)) == 0
                        && hasActualEnabledAt(serviceFeature)
                ) || (sendAmounts.size() == 0
                        && !hasActualEnabledAt(serviceFeature)
                ),
                sendAmounts.toString() + serviceFeatureDao.findById(serviceFeature.getId())
        );
    }

    @Test
    public void shouldHandleDeactivationBeforeActivation() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activation/" + getSentIdPattern()))
                .setAmountRequestTemplate(postUrlEncoded("http://some.yandex.ru/setAmount/#{context['id']}"))
                .deactivationRequestTemplate(postUrlEncoded("http://some.yandex.ru/deactivation/#{context['id']}"))
        );
        TIssuedFeature serviceFeature = createFeature(feature);
        serviceFeatureDao.setTargetState(serviceFeature.getId(), Target.DISABLED);

        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activation/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/setAmount/"));
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/deactivation/"));

        synchronize(getOwner(), feature);

        Mockito.verifyZeroInteractions(skipTransactionsExport);

        assertIsActual(serviceFeature);
    }

    @Test
    public void snoozingOnMpfsError() {
        Instant fixedNow = LocalDate.parse("2020-01-08").toDateTimeAtStartOfDay().toInstant();
        DateTimeUtils.setCurrentMillisFixed(fixedNow.getMillis());

        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
                .errorProcessorName("mpfs")
        );
        mockPost(Mockito.startsWith("http://some.yandex.ru/activate/"),
                "{\"message\":\"account has no password\",\"code\":113,\"response\":409}", HttpStatus.CONFLICT);

        TIssuedFeature serviceFeature = createFeature(feature, 20);

        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature));

        serviceFeature = serviceFeatureDao.findById(serviceFeature.getId());

        Assert.equals(SynchronizationStatus.SNOOZING, serviceFeature.getStatus());
        Assert.equals(fixedNow.plus(Duration.standardHours(4)), serviceFeature.getNextTry().get());
    }

    @Test
    public void shouldSetProductTemplateField() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/" + getSentIdPattern()))
        );
        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));

        ProductFeatureEntity productFeature = createProductFeature(feature, 20);
        UserProductEntity userProduct = userProductDao.findById(productFeature.getUserProductId());
        ProductTemplateEntity productTemplate =
                psBillingProductsFactory.createProductTemplate(userProduct.getCodeFamily());
        psBillingProductsFactory.createProductTemplateFeature(productTemplate.getId(), feature);

        TIssuedFeature serviceFeature = createFeature(productFeature);
        synchronize(getOwner(), feature);

        verifyExchangeForUrl("http://some.yandex.ru/activate/" + getSentIdValue(serviceFeature));
        Mockito.verifyNoMoreInteractions(skipTransactionsExport);
        assertIsActual(serviceFeature);
        assertHasTemplateFeature(serviceFeature);
    }

    private boolean hasActualEnabledAt(TIssuedFeature serviceFeature) {
        return serviceFeatureDao.findById(serviceFeature.getId()).getActualEnabledAt().isPresent();
    }

    private boolean hasActualDisabledAt(TIssuedFeature serviceFeature) {
        return serviceFeatureDao.findById(serviceFeature.getId()).getActualDisabledAt().isPresent();
    }

    protected void assertIsActual(TIssuedFeature serviceFeature) {
        Assert.equals(SynchronizationStatus.ACTUAL, refresh(serviceFeature).getStatus());
    }

    private void assertNotActual(TIssuedFeature serviceFeature) {
        Assert.notEquals(SynchronizationStatus.ACTUAL, refresh(serviceFeature).getStatus());
    }

    private void assertTarget(TIssuedFeature firstServiceFeature, Target target) {
        Assert.equals(
                target,
                serviceFeatureDao.findById(firstServiceFeature.getId()).getTarget()
        );
    }

    protected void assertHasTemplateFeature(TIssuedFeature serviceFeature) {
        Assert.notNull(serviceFeatureDao.findById(serviceFeature.getId())
                .getProductTemplateFeatureId().orElse((UUID) null)
        );
    }

    private void verifyExchangeForUrl(String s, String body) {
        Mockito.verify(skipTransactionsExport).exchange(
                Mockito.eq(s), Mockito.eq(HttpMethod.POST),
                Mockito.argThat(a -> body.equals(a.getBody())), Mockito.eq(String.class)
        );
    }

    @NotNull
    protected Option<RequestTemplate> postUrlEncoded(String template) {
        return Option.of(new RequestTemplate(HttpMethod.POST, template, Option.empty(),
                Option.of(MediaType.APPLICATION_FORM_URLENCODED)));
    }

    @NotNull
    private Option<RequestTemplate> postUrlEncoded(String template, String bodyTemplate) {
        return Option.of(new RequestTemplate(HttpMethod.POST, template, Option.of(bodyTemplate),
                Option.of(MediaType.APPLICATION_FORM_URLENCODED)));
    }
}
