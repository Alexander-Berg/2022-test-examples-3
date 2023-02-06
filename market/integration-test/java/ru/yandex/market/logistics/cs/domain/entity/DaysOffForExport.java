package ru.yandex.market.logistics.cs.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(schema = "public", name = "service_to_daysoff")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class DaysOffForExport {
    @Id
    @Column(name = "service_id")
    private Long serviceId;

    @Type(type = "jsonb")
    @Column(name = "daysoff_dates", columnDefinition = "jsonb")
    private DaysoffDates daysOffDates;
}
