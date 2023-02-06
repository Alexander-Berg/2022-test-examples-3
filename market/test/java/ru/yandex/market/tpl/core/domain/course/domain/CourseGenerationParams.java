package ru.yandex.market.tpl.core.domain.course.domain;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;

/**
 * @author sekulebyakin
 */
@Getter
@Builder
public class CourseGenerationParams {

    @Builder.Default
    private String name = "test-course-" + UUID.randomUUID();

    @Builder.Default
    private String programId = "test-program-uuid-" + UUID.randomUUID();

    @Builder.Default
    private String imageUrl = "http://localhost:8080/test-course-url.png";

    @Builder.Default
    private UserStatusRequirement userStatusRequirement = UserStatusRequirement.ALL;

    @Builder.Default
    private String description = "test-course-description";

    @Builder.Default
    private Integer expectedDurationInMinutes = 15;

    @Builder.Default
    private boolean beta = false;

    @Builder.Default
    private boolean scFiltersEnabled = true;

    @Builder.Default
    private boolean hasExam = true;

    CourseCommand.Create toCreateCommand() {
        return CourseCommand.Create.builder()
                .name(name)
                .programId(programId)
                .imageUrl(imageUrl)
                .userStatusRequirement(userStatusRequirement)
                .description(description)
                .expectedDurationInMinutes(expectedDurationInMinutes)
                .beta(beta)
                .scFiltersEnabled(scFiltersEnabled)
                .hasExam(hasExam)
                .build();
    }
}
