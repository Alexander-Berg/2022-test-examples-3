package ru.yandex.market.pharmatestshop.domain.pharmacy;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    public List<Pharmacy> getAllPharmacies() {
        return pharmacyRepository.findAll();
    }


    @Transactional
    public void updatePharmacySettingsDate() {
        pharmacyRepository.findAll().forEach(x -> {
            x.setFromDateExpress(LocalDate.now());
            x.setFromDateDelivery(LocalDate.now());
            x.setFromDatePickup(LocalDate.now());

            //To date
            x.setToDateExpress(LocalDate.now());
            x.setToDateDelivery(LocalDate.now().plusDays(3));
            x.setToDatePickup(LocalDate.now().plusDays(4));

        });

    }


    public Pharmacy getPharmacy(Long shopId) {
        return pharmacyRepository.findByShopId(shopId);
    }

    public void saveOrUpdate(Pharmacy pharmacy) {
        pharmacyRepository.save(pharmacy);
    }


    public void delete(long shop_id) {
        pharmacyRepository.deleteById(shop_id);
    }
}
