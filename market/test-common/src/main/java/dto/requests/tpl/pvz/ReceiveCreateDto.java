package dto.requests.tpl.pvz;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReceiveCreateDto {
    private List<ReceiveCreateItemDto> items;
}
