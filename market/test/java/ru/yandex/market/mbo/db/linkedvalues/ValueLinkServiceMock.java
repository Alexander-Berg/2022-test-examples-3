package ru.yandex.market.mbo.db.linkedvalues;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkRow;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinksFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 06.11.17
 */
public class ValueLinkServiceMock implements ValueLinkServiceInterface {
    private List<ValueLink> links = new ArrayList<>();
    private List<ValueLink> defaultLinks = new ArrayList<>();
    private long nextId = 1;

    private Multimap<Long, Long> optionIdsByParamId = ArrayListMultimap.create();

    public void setOptionIdsByParamId(Multimap<Long, Long> optionIdsByParamId) {
        this.optionIdsByParamId = optionIdsByParamId;
    }

    @Override
    public List<Long> saveValueLinks(List<ValueLink> valueLinks) {
        List<Long> savedIds = new ArrayList<>(valueLinks.size());
        for (ValueLink valueLink : valueLinks) {
            long id = saveValueLink(valueLink);
            savedIds.add(id);
        }
        return savedIds;
    }

    @Override
    public Long saveValueLink(ValueLink valueLink) {
        ValueLink copyLink = valueLink.copy();
        if (copyLink.getId() == null) {
            copyLink.setId(nextId++);
        } else {
            links.removeIf(link -> link.getId().equals(copyLink.getId()));
        }
        links.add(copyLink);

        valueLink.setId(copyLink.getId());

        return copyLink.getId();
    }

    @Override
    public Long saveCategoryDefaultValueLink(ValueLink link) {

        ValueLink defaultLink = new ValueLink();
        defaultLink.setId(link.getId());
        defaultLink.setSourceParamId(link.getSourceParamId());
        defaultLink.setSourceOptionId(ValueLink.CATEGORY_OPTION_ID);
        defaultLink.setTargetParamId(link.getTargetParamId());
        defaultLink.setTargetOptionId(link.getTargetOptionId());
        defaultLink.setLinkDirection(LinkDirection.DIRECT);
        defaultLink.setType(ValueLinkType.GENERAL);
        defaultLink.setCategoryHid(link.getCategoryHid());

        if (defaultLink.getId() == null) {
            defaultLink.setId(nextId++);
        } else {
            defaultLinks.removeIf(dLink -> dLink.getId().equals(defaultLink.getId()));
        }
        defaultLinks.add(defaultLink);

        link.setId(defaultLink.getId());

        return defaultLink.getId();
    }

    @Override
    public ValueLink getValueLink(Long id) {
        Optional<ValueLink> optionalLink = links.stream().filter(link -> link.getId().equals(id)).findFirst();
        if (optionalLink.isPresent()) {
            return optionalLink.get();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public List<ValueLink> findValueLinks(Long sourceOptionId, LinkDirection linkDirection, ValueLinkType type) {
        ValueLinkSearchCriteria criteria = new ValueLinkSearchCriteria();
        criteria.setLinkDirection(linkDirection);
        criteria.setSourceOptionIds(Collections.singletonList(sourceOptionId));
        criteria.setType(type);

        return findValueLinks(criteria);
    }

    @Override
    public List<Long> findValueLinkIds(Long sourceOptionId, LinkDirection linkDirection, ValueLinkType type) {
        return findValueLinks(sourceOptionId, linkDirection, type)
            .stream()
            .map(ValueLink::getId)
            .collect(Collectors.toList());
    }

    @Override
    public List<ValueLink> findValueLinks(ValueLinkSearchCriteria criteria) {
        return links.stream().filter(link -> match(criteria, link)).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("checkstyle:SimplifyBooleanReturn")
    public List<ValueLink> findConstraintLinksForOptionIds(Collection<Long> optionIds) {
        return links.stream().filter(link -> {
            if (link.getLinkDirection() == LinkDirection.DIRECT && optionIds.contains(link.getTargetOptionId())) {
                return true;
            } else if (link.getLinkDirection() == LinkDirection.REVERSE &&
                optionIds.contains(link.getSourceOptionId())) {
                return true;
            } else if (link.getLinkDirection() == LinkDirection.BIDIRECTIONAL &&
                (optionIds.contains(link.getSourceOptionId()) || optionIds.contains(link.getTargetOptionId()))) {
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public List<ValueLink> findConstraintLinksForParamOptions(Long paramId) {
        return findConstraintLinksForOptionIds(optionIdsByParamId.get(paramId));
    }

    @Override
    @SuppressWarnings("checkstyle:SimplifyBooleanReturn")
    public Set<Long> findSlaveOptionIds(Collection<Long> masterOptionIds, Long slaveParamId) {
        Set<Long> result = new HashSet<>();
        links.forEach(link -> {
            if ((link.getLinkDirection() == LinkDirection.DIRECT ||
                link.getLinkDirection() == LinkDirection.BIDIRECTIONAL)
                && masterOptionIds.contains(link.getSourceOptionId())
                && link.getTargetParamId().equals(slaveParamId)) {

                result.add(link.getTargetOptionId());
            } else if ((link.getLinkDirection() == LinkDirection.REVERSE ||
                link.getLinkDirection() == LinkDirection.BIDIRECTIONAL)
                && masterOptionIds.contains(link.getTargetOptionId())
                && link.getSourceParamId().equals(slaveParamId)) {

                result.add(link.getSourceOptionId());
            }
        });
        return result;
    }

    @Override
    public List<ValueLink> listLinksForVendor(Long vendorId) {
        ValueLinkSearchCriteria linkSearchCriteria = new ValueLinkSearchCriteria();
        linkSearchCriteria.setTargetOptionIds(Collections.singletonList(vendorId));
        linkSearchCriteria.setLinkDirection(LinkDirection.REVERSE);

        List<ValueLink> linksForVendor = findValueLinks(linkSearchCriteria);
        return linksForVendor;
    }

    @Override
    public void removeValueLink(Long id) {
        links.removeIf(link -> link.getId().equals(id));
    }

    @Override
    public void removeCategoryDefaultValueLink(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValueLinksForOption(Long optionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValueLinksForOption(Collection<Long> optionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValueLinksForParameter(Long parameterId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValueLinks(ValueLinkSearchCriteria criteria) {
        links.removeIf(link -> match(criteria, link));
    }

    @Override
    public List<ValueLinkRow> getValueLinks(ValueLinksFilter filter, ValueLinkRow.Field sortField, boolean ascending) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ValueLinkRow> getSourceValueLinks(long categoryHid, Long sourceParamId, Set<Long> sourceOptionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ValueLinkRow> getCategoryDefaultValueLinks(long categoryHid, Long sourceParamId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ValueLinkRow> getCategoryDefaultValueLinks(long categoryHid) {
        return defaultLinks.stream()
            .filter(link -> link.getCategoryHid().equals(categoryHid))
            .map(link -> {
                     ValueLinkRow row = new ValueLinkRow();
                     row.setValueLink(link);
                     row.setSourceParamName("source_parameter_" + link.getSourceParamId());
                     row.setSourceOptionName("source_option_" + link.getSourceOptionId());
                     row.setTargetParamName("target_parameter_" + link.getTargetParamId());
                     row.setTargetOptionName("target_option_" + link.getTargetOptionId());
                     return row;
                 }
            )
            .collect(Collectors.toList());
    }

    @Override
    public Integer getValueLinksCount(ValueLinksFilter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isValueLinkExist(Long sourceParamId, Long sourceOptionId, Long targetParamId, Long targetOptionId,
                                    LinkDirection linkDirection) {
        return true;
    }

    private static boolean match(ValueLinkSearchCriteria criteria, ValueLink link) {
        return match(criteria.getSourceParamIds(), link.getSourceParamId())
            && match(criteria.getSourceOptionIds(), link.getSourceOptionId())
            && match(criteria.getTargetParamIds(), link.getTargetParamId())
            && match(criteria.getTargetOptionIds(), link.getTargetOptionId())
            && match(criteria.getHids(), link.getCategoryHid());
    }

    private static <T> boolean match(Collection<T> expectedValues, T testedValue) {
        return expectedValues == null || expectedValues.contains(testedValue);
    }
}
