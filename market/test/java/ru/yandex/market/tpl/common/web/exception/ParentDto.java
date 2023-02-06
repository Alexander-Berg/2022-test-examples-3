package ru.yandex.market.tpl.common.web.exception;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Past;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel(description = "Пример родиетльской DTO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentDto {

    @ApiModelProperty("два плюс два равно пять")
    @AssertFalse
    private Boolean falseField;

    @ApiModelProperty("дважды два - четыре")
    @AssertTrue
    private Boolean trueField;

    @ApiModelProperty("Число пальцев на руке")
    @DecimalMax("10")
    private Integer fingersCount;

    @ApiModelProperty("Зарплата")
    @DecimalMin("0")
    private BigDecimal salary;

    @ApiModelProperty("Цена")
    @Digits(integer = 4, fraction = 2)
    private String cost;

    @ApiModelProperty("Электронная почта")
    @Email
    private String email;

    @ApiModelProperty("Дата конца света")
    @Future
    private LocalDate endOfTheWorldDate;

    @ApiModelProperty("Сегодняшняя дата")
    @FutureOrPresent
    private LocalDate today;

    @ApiModelProperty("Оценка")
    @Max(5)
    private Long mark;

    @ApiModelProperty("Скорость")
    @Min(0)
    private Double speed;

    @ApiModelProperty("Зрение")
    @Negative
    private Integer vision;

    @ApiModelProperty("Отклонение")
    @NegativeOrZero
    private Short deviation;

    @ApiModelProperty("Имя")
    @NotBlank
    private String name;

    @ApiModelProperty("Навыки")
    @NotEmpty
    private List<String> skills;

    @ApiModelProperty("День недели")
    @NotNull
    private DayOfWeek dayOfWeek;

    @ApiModelProperty("Месяц")
    @Null
    private Month month;

    @ApiModelProperty("День распада СССР")
    @Past
    private LocalDate sovietUnionLastDay;

    @ApiModelProperty("День рождения Яндекса")
    @PastOrPresent
    private LocalDate yandexBirthday;

    @ApiModelProperty("Номер мобильного телефона")
    @Pattern(regexp = "^\\d{11}$")
    private String phoneNumber;

    @ApiModelProperty("Рост")
    @Positive
    private Short height;

    @ApiModelProperty("Градусы")
    @PositiveOrZero
    private Short degrees;

    @ApiModelProperty("Спутники Земли")
    @Size(max = 1)
    private List<String> earthSatellites;

    @ApiModelProperty("Дети")
    @Valid
    private List<ChildDto> children;

    @ApiModelProperty("Внебрачный ребенок")
    @Valid
    private ChildDto child;

    @ApiModelProperty("Друзья")
    @Valid
    private Map<String, ChildDto> friends;
}
