package ru.yandex.market.tpl.core.domain.promo;

import java.util.List;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PromoTestHelper {

    private final PromoRepository promoRepository;
    private final EntityManager entityManager;

    public Promo createPromo(String promoId) {
        return createPromo(promoId, PromoStatus.PUBLISHED);
    }

    public Promo createPromo(String promoId, PromoStatus status) {
        return createPromo(promoId, status, 10L);
    }

    public Promo createPromo(String promoId, PromoStatus status, Long priority) {
        Promo promo = new Promo();
        promo.setId(promoId);
        promo.setStatus(status);
        promo.setScreens(List.of("screen1", "screen2"));
        promo.setPriority(priority);

        Promo saved = promoRepository.save(promo);
        entityManager.flush();
        return saved;
    }


}
