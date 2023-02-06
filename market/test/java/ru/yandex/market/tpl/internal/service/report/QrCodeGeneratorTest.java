package ru.yandex.market.tpl.internal.service.report;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeGeneratorTest {

    @InjectMocks
    private QrCodeGenerator subject;
    @Mock
    private SortingCenterRepository sortingCenterRepository;
    @Mock
    private SortingCenter sortingCenter;
    private Clock clock = Clock.fixed(
            LocalDateTime.of(2021, 9, 10, 10, 10, 10)
                    .toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
    );
    private Long scId = 1L;
    private String scName = "name";
    private String code = "1265";
    private LocalDate date = LocalDate.now(clock);

    @BeforeEach
    void beforeEach() {
        when(sortingCenter.getName()).thenReturn(scName);
        when(sortingCenter.getZoneOffset()).thenReturn(ZoneOffset.UTC);
        when(sortingCenterRepository.findByIdOrThrow(any())).thenReturn(sortingCenter);
    }

    @Test
    @SneakyThrows
    void buildPdfSuccessfully() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        subject.generateQrCodePdf(os, scId, date, code);

        PdfReader reader = new PdfReader(os.toByteArray());

        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).isNotNull();

        String[] lines = text.split("\n");
        assertThat(lines.length).isEqualTo(5);
        assertThat(lines[2]).isEqualTo(scName + " - " + date);
        assertThat(lines[4]).isEqualTo(code);
    }

}
