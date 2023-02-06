package ru.yandex.market.markup2.tasks.fill_param_values;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mockito.Mock;
import ru.yandex.market.markup2.utils.YqlDao;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.offer.Offer;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.utils.tovarTree.TovarTreeProvider;
import ru.yandex.market.markup2.tasks.fill_param_values.formalized.FormalizedTopValues;
import ru.yandex.market.markup2.tasks.fill_param_values.formalized.FormalizedValuesService;
import ru.yandex.market.markup2.utils.Markup2TestUtils;
import ru.yandex.market.markup2.workflow.general.IdGenerator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 15.06.2017
 */
public class FillParamValuesRequestGeneratorTestBase {

    protected static final int CATEGORY_ID = 311;

    @Mock
    protected ParamUtils paramUtils;

    @Mock
    protected YqlDao yqlDao;

    @Mock
    protected ModelStorageService modelStorageService;

    @Mock
    protected FormalizedValuesService formalizedValuesService;

    @Mock
    protected TovarTreeProvider tovarTreeProvider;

    protected IdGenerator idGenerator = Markup2TestUtils.mockIdGenerator();

    protected final Long2ObjectMap<String> vendorNames =
        new Long2ObjectOpenHashMap<>(ImmutableMap.of(ModelsData.VENDOR_OPTION_ID, "vendor1_name"));

    public void setup() {
        when(paramUtils.getVendors(anyInt())).thenReturn(vendorNames);
        when(formalizedValuesService.getFormalizedTopValues(anyInt(), anyCollection(), anyCollection()))
            .thenReturn(new FormalizedTopValues());
        when(tovarTreeProvider.getCategoryName(anyInt())).thenReturn("Cat");
    }

    protected static Set<Long> getParameterIds(Collection<MboParameters.Parameter> parameters) {
        return parameters.stream()
            .map(MboParameters.Parameter::getId)
            .collect(Collectors.toSet());
    }

    protected static Set<Long> getFilledParameterIds(ModelStorage.Model model) {
        return model.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getParamId)
            .collect(Collectors.toSet());
    }

    protected List<Offer> generateOffers(int count) {
        List<Offer> result = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            String offerId = String.valueOf(i);
            Offer offer = (Offer) Offer.newBuilder()
                .setUrl("http://offer_url" + i)
                .setOfferId(offerId)
                .build();
            result.add(offer);
        }
        return result;
    }

    protected static List<ModelStorage.Model> allModels(Iterable... models) {
        return Lists.newArrayList(Iterables.concat(models));
    }
}
