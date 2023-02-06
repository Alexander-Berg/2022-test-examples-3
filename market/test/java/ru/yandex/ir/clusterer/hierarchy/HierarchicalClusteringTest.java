package ru.yandex.ir.clusterer.hierarchy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
/**
 * @author mkrasnoperov
 */
public class HierarchicalClusteringTest {

    //MARKETIR-2954
    @Test
    public void testSomeBigCases() {
        // 1. строим несколько множеств объектов, отмечаем их метками типа A1, A2,..., B1, B2...
        // 2, объединяем в общий массив, шаффлим
        // 3. строим матрицу
        // 4. кластеризуем
        // 5. вытаскиваем из дерева кластера, проверяем, лежат ли в них объекты с одной меткой
        Random random = new Random(42);
        IntList pseudoClusters = new IntArrayList();
        for (int i = 0; i < 100; i++) {
            pseudoClusters.add(1 + (int) (50 * random.nextDouble()));
        }
        List<String> elements = buildList(random, pseudoClusters.toIntArray());
        double[][] weights = buildBigMatrix(elements, random);

        HierarchicalClustering<Object> clustering = new HierarchicalClustering<>(
            new SummingUnionManager(weights)
        );
        List<int[]> clusters = clustering.clusterizeAndGetIndexes();

        int foundClusters = countSameTypeClusters(clusters, elements);
        assertEquals(pseudoClusters.size(), foundClusters);
    }

    private int countSameTypeClusters(List<int[]> clusters, List<String> elements) {
        int result = 0;
        for (int[] inClusterIndexes : clusters) {
            String commonPrefix = null;
            for (int elementsId : inClusterIndexes) {
                String element = elements.get(elementsId);
                String elementPrefix = element.substring(0, 1);
                if (null == commonPrefix) {
                    commonPrefix = elementPrefix;
                } else {
                    assertEquals(commonPrefix, elementPrefix);
                }
            }
            result += 1;
        }
        return result;
    }

    private double[][] buildBigMatrix(List<String> elements, Random random) {
        double[][] matrix = new double[elements.size()][elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            String eI = elements.get(i);
            final char letterI = eI.charAt(0);
            for (int j = i; j < elements.size(); j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    String eJ = elements.get(j);
                    if (letterI == eJ.charAt(0)) {
                        matrix[i][j] = matrix[j][i] = 0.1 + random.nextDouble() * 0.3 - 0.45;
                    } else {
                        matrix[i][j] = matrix[j][i] = 0.5 + random.nextDouble() * 1.5 - 0.45;
                    }
                }
            }
        }
        return matrix;
    }

    private List<String> buildList(Random random, int... lengths) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < lengths.length; i++) {
            int length = lengths[i];
            char letter = (char) ('A' + i);
            for (int j = 1; j <= length; j++) {
                list.add(letter + "-" + j);
            }
        }
        Collections.shuffle(list, random);
        return list;
    }

    @Test
    public void testForExceptions() {
        {
            double[][] matrix = generateSomeMatrix();
            for (int i = 0; i < 2000; i++) {
                HierarchicalClustering<Object> clustering = new HierarchicalClustering<>(
                    new SummingUnionManager(matrix)
                );
                List<int[]> clusters = clustering.clusterizeAndGetIndexes();
                assertNotNull(clusters);
            }
        }
        {
            double[][] weights = new double[][]{
                {0.0, 1.0, 3.0, 4.0, 5.0, 6.0},
                {1.0, 0.0, 2.0, 5.0, 4.0, 4.5},
                {3.0, 2.0, 0.0, 6.0, 4.5, 4.0},
                {4.0, 5.0, 6.0, 0.0, 2.0, 3.0},
                {5.0, 4.0, 4.5, 2.0, 0.0, 1.0},
                {6.0, 4.5, 4.0, 3.0, 1.0, 0.0},
            };
            HierarchicalClustering<Object> clustering = new HierarchicalClustering<>(
                new SummingUnionManager(weights)
            );
            List<int[]> clusters = clustering.clusterizeAndGetIndexes();
            assertNotNull(clusters);
        }
        {
            double[][] weights = new double[][]{
                {0.00, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15, 1.15},
                {1.15, 0.00, 0.46, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41},
                {1.15, 0.46, 0.00, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41, 1.41},
                {1.15, 1.41, 1.41, 0.00, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.00, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.00, 0.47, 0.47, 0.47, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.47, 0.00, 0.47, 0.47, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.47, 0.47, 0.00, 0.47, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.47, 0.47, 0.47, 0.00, 0.47, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47, 0.00, 0.47},
                {1.15, 1.41, 1.41, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47, 0.47, 0.00},
            };
            HierarchicalClustering<Object> clustering = new HierarchicalClustering<>(
                new SummingUnionManager(weights)
            );
            List<int[]> clusters = clustering.clusterizeAndGetIndexes();
            assertNotNull(clusters);
        }
    }

    private double[][] generateSomeMatrix() {
        int n = 100;
        double[][] matrix = new double[n][n];
        for (int i = 0, matrixLength = matrix.length; i < matrixLength; i++) {
            double[] row = matrix[i];
            for (int j = 0, rowLength = row.length; j < rowLength; j++) {
                if (i == j) {
                    row[j] = 0;
                } else {
                    row[j] = (i * j) % 9 + (i + j) * 0.1;
                }
            }
        }
        return matrix;
    }

    @Test
    public void testInfiniteWeights(){
        double[][] weights = new double[][]{
            {0.0, -2.0, -1.0},
            {-2.0, 0.0, Double.POSITIVE_INFINITY},
            {-1.0, Double.POSITIVE_INFINITY, 0.0}
        };
        HierarchicalClustering<Object> clustering = new HierarchicalClustering<>(
            new SummingUnionManager(weights)
        );
        List<int[]> clusters = clustering.clusterizeAndGetIndexes();
        ClusterTestUtils.assertSameClusterSet(Arrays.asList(new int[]{0, 1}, new int[]{2}), clusters);
    }

    private static class SummingUnionManager extends WithMatrixUnionManager<Object> {

        public SummingUnionManager(double[][] initialWeights) {
            super(initialWeights, x -> null);
        }

        @Override
        protected Object buildUnionMeta(Node<Object> firstNode, Node<Object> secondNode) {
            return null;
        }

        @Override
        protected double calculateNewWeight(Node<Object> newNode, int firstChildIndex, int secondChildIndex, int targetIndex) {
            return currentWeights[firstChildIndex][targetIndex] + currentWeights[secondChildIndex][targetIndex];
        }
    }
}