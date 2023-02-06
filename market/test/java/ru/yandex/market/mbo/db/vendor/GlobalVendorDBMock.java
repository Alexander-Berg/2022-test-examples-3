package ru.yandex.market.mbo.db.vendor;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.support.TransactionCallback;
import ru.yandex.market.mbo.core.images.HyperImageDepotService;
import ru.yandex.market.mbo.gwt.models.vendor.Filter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.Logo;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 30.03.18
 */
public class GlobalVendorDBMock implements GlobalVendorDBInterface {
    Map<Long, GlobalVendor> vendors = new HashMap<>();
    long nextId = 1;

    @Override
    public void doInTransaction(TransactionCallback transactionCallback) {
        transactionCallback.doInTransaction(null);
    }

    @Override
    public GlobalVendor loadVendor(long vendorId) {
        return vendors.get(vendorId);
    }

    @Override
    public List<GlobalVendor> loadVendors(Collection<Long> ids) {
        return vendors.values().stream().filter(vendor -> ids.contains(vendor.getId())).collect(Collectors.toList());
    }

    @Override
    public List<GlobalVendor> loadVendorsByIgnoreCaseName(String ignoreCaseName) {
        return vendors.values().stream()
            .filter(vendor -> containsNameIgnoreCase(vendor, ignoreCaseName))
            .collect(Collectors.toList());
    }

    @Override
    public boolean exists(long vendorId) {
        return vendors.containsKey(vendorId);
    }

    @Override
    public List<GlobalVendor> getGlobalVendors(Filter filter, int offset, int size) {
        List<GlobalVendor> filteredVendors =
            vendors.values().stream().filter(vendor -> test(filter, vendor)).collect(Collectors.toList());
        if (filter.getOrderField() != null) {
            filteredVendors.sort(createComparator(filter.getOrderField(), filter.isOrderAscending()));
        }

        if (offset > filteredVendors.size() - 1) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(offset + size, filteredVendors.size());
        return filteredVendors.subList(offset, toIndex);
    }

    @Override
    public int getGlobalVendorsCount(Filter filter) {
        List<GlobalVendor> filteredVendors =
            vendors.values().stream().filter(vendor -> test(filter, vendor)).collect(Collectors.toList());
        return filteredVendors.size();
    }

    @Override
    public List<GlobalVendor> loadAllVendors() {
        return new ArrayList<>(vendors.values());
    }

    @Override
    public long createVendor(GlobalVendor vendor, long uid) {
        vendor.setId(nextId++);
        GlobalVendor savedVendor = vendor.copy();
        savedVendor.setLogos(new ArrayList<>());
        vendors.put(vendor.getId(), vendor);
        return vendor.getId();
    }

    @Override
    public void updateVendor(GlobalVendor v, long uid, boolean replaceNames, boolean replaceAliases,
                             boolean replaceRecommendedShops, boolean updateLogos) {
        GlobalVendor newVendor = v.copy();
        GlobalVendor oldVendor = vendors.get(v.getId());
        if (!replaceNames) {
            newVendor.setNames(oldVendor.getNames());
        }
        if (!replaceAliases) {
            newVendor.setAliases(oldVendor.getAliases());
        }
        if (!replaceRecommendedShops) {
            newVendor.setRecommendedShopPatterns(oldVendor.getRecommendedShopPatterns());
        }
        if (!updateLogos) {
            newVendor.setLogos(oldVendor.getLogos());
        }
        vendors.put(v.getId(), newVendor);
    }

    @Override
    public void removeVendor(long vendorId) {
        vendors.remove(vendorId);
    }

    @Override
    public void updateImage(long vendorId, HyperImageDepotService.ImageInfo imageUrl, List<Logo> logos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeImage(long vendorId, String imageUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateRecommendedShops(long vendorId, List<String> shopPatterns) {
        vendors.get(vendorId).setRecommendedShopPatterns(shopPatterns);
    }

    @Override
    public void loadManufacturer(GlobalVendor globalVendor) {
    }

    private static boolean containsNameIgnoreCase(GlobalVendor vendor, String ignoreCaseName) {
        for (Word word : vendor.getNames()) {
            if (ignoreCaseName.equalsIgnoreCase(word.getWord())) {
                return true;
            }
        }
        return false;
    }

    private static boolean test(Filter filter, GlobalVendor vendor) {
        if (filter.getLineName() != null) {
            throw new UnsupportedOperationException("GlobalVendorDBMock.test unsupported filter with lineName.");
        }
        if (filter.getId() != null && !Objects.equals(filter.getId(), vendor.getId())) {
            return false;
        }
        if (filter.getName() != null && !matchesWords(filter.getName(), vendor.getNames())) {
            return false;
        }
        if (filter.getPublished() != null && !Objects.equals(filter.getPublished(), vendor.isPublished())) {
            return false;
        }
        if (filter.getComment() != null && !filter.getComment().trim().equalsIgnoreCase(vendor.getComment())) {
            return false;
        }
        if (filter.getRecommendedShop() != null
            && !vendor.getRecommendedShopPatterns().contains(filter.getRecommendedShop())) {
            return false;
        }
        if (filter.hasSeoFields() && vendor.getSeoTitle() == null && vendor.getSeoDescription() == null) {
            return false;
        }
        return true;
    }

    private static boolean matches(String regex, String text) {
        String javaRegex = regex.toLowerCase().replaceAll("\\*+", ".*") + ".*";
        return Pattern.matches(javaRegex, text.toLowerCase());
    }

    private static boolean matchesWords(String regex, Collection<Word> words) {
        for (Word word : words) {
            if (matches(regex, word.getWord())) {
                return true;
            }
        }
        return false;
    }

    private static Comparator<GlobalVendor> createComparator(@NotNull  Filter.Field orderFiled,
                                                             boolean orderAscending) {
        Comparator<GlobalVendor> comparator;
        switch (orderFiled) {
            case ID: {
                comparator = Comparator.comparing(GlobalVendor::getId);
                break;
            }
            case NAME: {
                comparator = Comparator.comparing(vendor -> emptyWhenNull(vendor.getDefaultName()).toLowerCase());
                break;
            }
            case PUBLISHED: {
                comparator = Comparator.comparing(GlobalVendor::isPublished);
                break;
            }
            case COMMENT: {
                comparator = Comparator.comparing(vendor -> emptyWhenNull(vendor.getComment()).toLowerCase());
                break;
            }
            case SITE: {
                comparator = Comparator.comparing(vendor -> emptyWhenNull(vendor.getSite()).toLowerCase());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown ordered field " + orderFiled);
            }
        }
        if (!orderAscending) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static String emptyWhenNull(String str) {
        return str != null ? str : "";
    }
}
