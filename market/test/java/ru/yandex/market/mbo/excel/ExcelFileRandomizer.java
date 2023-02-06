package ru.yandex.market.mbo.excel;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ExcelFileRandomizer implements Randomizer<ExcelFile> {

    private final EnhancedRandom random;

    public ExcelFileRandomizer(long seed) {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .seed(seed)
                .build();
    }

    @Override
    public ExcelFile getRandomValue() {
        ExcelFile.Builder result = new ExcelFile.Builder();

        int headerSize = 1 + random.nextInt(3);
        for (int i = 0; i < headerSize; i++) {
            result.setHeader(i, random.nextObject(String.class));
        }

        int lines = random.nextInt(5);
        for (int i = 0; i < lines; i++) {
            int lineSize = 1 + random.nextInt(3);
            for (int j = 0; j < lineSize; j++) {
                // каждый 5 и 6 элемент будут либо пустыми или null
                // равновероятные варианты для чисел и для строк
                int randomVal = random.nextInt(6);
                switch (randomVal) {
                    case 0:
                        result.setValue(i + 1, j, null);
                        break;
                    case 1:
                        result.setValue(i + 1, j, "");
                        break;
                    case 2:
                    case 3:
                        result.setValue(i + 1, j, random.nextInt());
                        break;
                    default:
                        result.setValue(i + 1, j, random.nextObject(String.class));
                        break;
                }
            }
        }

        return result.build();
    }
}
