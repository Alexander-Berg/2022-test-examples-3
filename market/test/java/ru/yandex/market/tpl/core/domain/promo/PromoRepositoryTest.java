package ru.yandex.market.tpl.core.domain.promo;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PromoRepositoryTest {

    private final PromoRepository promoRepository;

    private final EntityManager entityManager;


    @Test
    void save() {
        Promo promo = new Promo();
        String promoId = "test";
        List<String> screens = List.of("screen1", "screen2");
        promo.setId(promoId);
        promo.setStatus(PromoStatus.PUBLISHED);
        promo.setScreens(screens);

        promoRepository.save(promo);
        promoRepository.flush();

        Optional<Promo> saved = promoRepository.findById(promoId);
        assertThat(saved).isPresent();

        assertThat(saved.get().getScreens())
                .containsExactlyInAnyOrderElementsOf(screens);
    }


    @Test
    void update() {
        Promo promo = new Promo();
        String promoId = "test";
        promo.setId(promoId);
        promo.setStatus(PromoStatus.NEW);
        promo.setScreens(List.of("screen1"));

        promoRepository.save(promo);
        promoRepository.flush();

        Optional<Promo> saved = promoRepository.findById(promoId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(PromoStatus.NEW);

        List<String> screens = List.of("screen1", "screen2");
        Promo duplicatePromo = new Promo();
        duplicatePromo.setId(promoId);
        duplicatePromo.setStatus(PromoStatus.PUBLISHED);
        duplicatePromo.setScreens(screens);

        entityManager.clear();
        promoRepository.save(duplicatePromo);
        promoRepository.flush();

        Optional<Promo> updated = promoRepository.findById(promoId);
        assertThat(updated).isPresent();

        assertThat(updated.get().getScreens())
                .containsExactlyInAnyOrderElementsOf(screens);
        assertThat(updated.get().getStatus()).isEqualTo(PromoStatus.PUBLISHED);
    }
}
