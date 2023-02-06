package ru.yandex.market.global.index.domain.category;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.assertj.core.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.index.mapper.JsonMapper;
import ru.yandex.mj.generated.server.model.AttributeDto;
import ru.yandex.mj.generated.server.model.Locale;
import ru.yandex.mj.generated.server.model.LocalizedStringDto;
import ru.yandex.mj.generated.server.model.MarketCategoryDto;

import static ru.yandex.market.global.common.util.StringFormatter.sf;

@Disabled
public class CategoryGenerator {
    @Test
    @SneakyThrows
    public void generateCategories() {
        List<MarketCategoryDto> categories = createCategories();
        System.out.println(JsonMapper.ELASTIC_JSON_MAPPER.writeValueAsString(categories));
        categories.forEach(this::saveTemplate);
    }

    @SneakyThrows
    private void saveTemplate(MarketCategoryDto category) {
        try (FileOutputStream file = new FileOutputStream("/home/moskovkin/Work/templates/" + category.getCode() + ".csv", false)) {
            String attributes = category.getAttributes().stream()
                    .map(AttributeDto::getCode)
                    .collect(Collectors.joining(","));
            String template = "id,category,marketCategory,available," + attributes;
            file.write(template.getBytes());
        }
    }

    @NotNull
    private List<MarketCategoryDto> createCategories() throws IOException {
        Map<String, List<AttributeDto>> infomodels = JsonMapper.ELASTIC_JSON_MAPPER.readValue(
                CategoryGenerator.class.getResourceAsStream("/category/infomodels.json"),
                new TypeReference<Map<String, List<AttributeDto>>>() {
                }
        );

        //noinspection ConstantConditions
        InputStreamReader categoriesCsvReader = new InputStreamReader(
                CategoryGenerator.class.getResourceAsStream("/category/categories.csv")
        );

        List<MarketCategoryDto> categories = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(categoriesCsvReader)) {
            //Skip header
            csvReader.readNext();

            String[] line = csvReader.readNext();
            while (line != null) {
                Long id = Long.parseLong(line[1]);
                String nameEn = line[3];
                String nameHe = line[8];
                String code = line[5];
                Long parentId = Strings.isNullOrEmpty(line[10]) ? null : Long.parseLong(line[10]);
                List<AttributeDto> infomodel = infomodels.get(line[6]);
                if (infomodel == null) {
                    throw new RuntimeException(sf("{} infomodul not found", line[6]));
                }
                MarketCategoryDto category = new MarketCategoryDto()
                        .id(id)
                        .title(List.of(
                                new LocalizedStringDto().locale(Locale.EN).value(nameEn),
                                new LocalizedStringDto().locale(Locale.HE).value(nameHe)
                        ))
                        .code(code)
                        .parentId(parentId)
                        .attributes(infomodel);
                categories.add(category);
                line = csvReader.readNext();
            }
        }
        return categories;
    }
}
