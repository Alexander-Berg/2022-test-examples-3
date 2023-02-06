package ru.yandex.market.vmid.services;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.vmid.repositories.IdsRepository;
import ru.yandex.market.vmid.repositories.dto.Vmid;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdsServiceImplTest {

    private final IdsRepository repository = mock(IdsRepository.class);
    private final IdsServiceImpl idsService = new IdsServiceImpl(repository);

    @Test
    public void getId_existed() {
        when(repository.getId(anyString(), anyLong())).thenReturn(Optional.of(new Vmid("12", 1L, 5L, LocalDate.now())));
        Long id = idsService.getId("1", 15L);
        assertEquals(Long.valueOf(5), id);
    }

    @Test
    public void getId_non_existed() {
        when(repository.getId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(repository.createNewVmid(anyString(), anyLong())).thenReturn(new Vmid("12", 1, 10L, LocalDate.now()));
        Long id = idsService.getId("1", 15L);
        assertEquals(Long.valueOf(10), id);
    }
}
