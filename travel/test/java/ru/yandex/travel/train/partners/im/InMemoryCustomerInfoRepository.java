package ru.yandex.travel.train.partners.im;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.travel.train.model.CustomerInfo;

public class InMemoryCustomerInfoRepository implements CustomerInfoRepository {
    private Map<Integer, CustomerInfo> cache = new HashMap<>();
    private Map<Integer, List<CustomerInfo>> orderIdCache = new HashMap<>();
    private Map<Integer, List<CustomerInfo>> buyOperationIdCache = new HashMap<>();

    @Override
    public List<CustomerInfo> getByOrderId(Integer orderId) {
        return orderIdCache.get(orderId);
    }

    @Override
    public List<CustomerInfo> getByBuyOperationId(Integer orderItemId) {
        return buyOperationIdCache.get(orderItemId);
    }

    @Override
    public CustomerInfo getOne(Integer customerId) {
        return cache.get(customerId);
    }

    @Override
    public void save(List<CustomerInfo> customerInfoList) {
        for (var c: customerInfoList) {
            cache.put(c.getCustomerId(), c);
            orderIdCache.putIfAbsent(c.getOrderId(), new ArrayList<>());
            orderIdCache.get(c.getOrderId()).add(c);
            buyOperationIdCache.putIfAbsent(c.getBuyOperationId(), new ArrayList<>());
            buyOperationIdCache.get(c.getBuyOperationId()).add(c);
        }
    }
}
