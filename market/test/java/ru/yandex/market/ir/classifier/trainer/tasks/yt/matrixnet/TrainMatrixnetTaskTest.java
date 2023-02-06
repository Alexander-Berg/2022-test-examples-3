package ru.yandex.market.ir.classifier.trainer.tasks.yt.matrixnet;

import org.junit.Test;
import ru.yandex.market.ir.classifier.ml.SimpleClassifier;
import ru.yandex.market.ir.classifier.trainer.tasks.yt.matrixnet.transform.ToConfidencePoolTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TrainMatrixnetTaskTest {
    @Test
    public void testPoolTransformer() {
        List<String> sourcePool = Arrays.asList(
            "0\t1.0\ta,1\t1.0\t0.0\t10.0",
            "0\t0.0\ta,2\t1.0\t0.0\t11.0",
            "1\t0.0\tb,1\t1.0\t0.0\t-10.0",
            "1\t1.0\tb,2\t1.0\t0.0\t5.0",
            "1\t0.0\tb,3\t1.0\t0.0\t3.0"
        );
        List<String> expectedPool = Arrays.asList(
            "0\t0.0\ta,2,a,1\t1.0\t0.0\t11.0\t0.0\t10.0",
            "1\t1.0\tb,2,b,3\t1.0\t0.0\t5.0\t0.0\t3.0"
        );

        List<String> resultPullAccumulator = new ArrayList<>();
        SimpleClassifier classifier = buildClassifier();

        ToConfidencePoolTransformer transformer =
            new ToConfidencePoolTransformer(
                classifier
            );
        transformer.transformPool(
            sourcePool.stream(),
            resultPullAccumulator::add
        );
        assertEquals(expectedPool, resultPullAccumulator);
    }

    @Test
    public void testCalculateConfidenceStatistics() {
        List<String> confidencePool = new ArrayList<>();
        int poolSize = 1500;
        Random rnd = new Random(435);
        for (int i = 0; i < poolSize; i++) {
            confidencePool.add(
                new PoolRow(
                    i,
                    rnd.nextDouble() + i / (poolSize + 0.0) * 0.5 > 0.5 ? 1 : 0,
                    i + "",
                    1.0,
                    "0.0" + "\t" + (i / (poolSize + 0.0) * 2 - 1)
                    ).toPoolRow()
            );
        }
        List<String> expectedStatistics = Arrays.asList(
            "-1.0\t1.0\t0.746\t1.0",
            "-0.5\t0.75\t0.8035555555555556\t0.8078641644325291",
            "0.0\t0.5\t0.8786666666666667\t0.5889186773905273",
            "0.5\t0.25\t0.9413333333333334\t0.3154602323503128"
        );

        List<String> resultPullAccumulator = new ArrayList<>();

        SimpleClassifier classifier = buildClassifier();
        final int percentilesNumber = 4;
        TwoStepFormulasTrainer.calculateConfidenceStatistics(
            classifier, percentilesNumber, confidencePool.stream(), resultPullAccumulator::add
        );
        assertEquals(percentilesNumber, resultPullAccumulator.size());
        assertEquals(expectedStatistics, resultPullAccumulator);
    }

    private static SimpleClassifier buildClassifier() {
        return new SimpleClassifier() {
            @Override
            public double[] classifyBatch(float[][] featuresMatrix) {
                double[] res = new double[featuresMatrix.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = featuresMatrix[i][1];
                }
                return res;
            }

            @Override
            public void close() throws IOException {
                // nothing
            }
        };
    }
}
