package dto.requests.lms;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActionDto {

    private Set<@NotNull Long> ids;
}
