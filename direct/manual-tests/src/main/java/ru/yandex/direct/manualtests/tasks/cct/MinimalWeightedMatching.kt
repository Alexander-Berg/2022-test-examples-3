package ru.yandex.direct.manualtests.tasks.cct

import kotlin.math.max
import kotlin.math.min

object MinimalWeightedMatching {
    /**
     * Константа, означающая отсутствие ребра
     */
    const val INF: Int = Int.MAX_VALUE

    fun minimalMatching(left: Int, right: Int, weights: Array<IntArray>): List<Pair<Int, Int>> {
        val swap: Boolean = left > right

        val n = min(left, right)
        val m = max(left, right)

        val a: Array<IntArray> = Array(n + 1) { i ->
            IntArray(m + 1) { j ->
                if (i >= 1 && j >= 1) {
                    if (!swap) {
                        weights[i - 1][j - 1]
                    } else {
                        weights[j - 1][i - 1]
                    }
                } else {
                    0
                }
            }
        }

        val p: IntArray = hungarianAlgorithm(n, m, a)

        val result: MutableList<Pair<Int, Int>> = mutableListOf()

        for (j in 1..m) {
            if (p[j] == 0) {
                continue
            }

            result += if (!swap) {
                p[j] - 1 to j - 1
            } else {
                j - 1 to p[j] - 1
            }
        }

        return result
    }

    /**
     * Нагло переписано с e-maxx: [e-maxx.ru/algo/assignment_hungary](https://e-maxx.ru/algo/assignment_hungary)
     * @param n количество строк
     * @param m количество столбцов, не меньше [n]
     * @param a матрица весов, индексация с единицы
     * @return массив `p[i]` - индекс строки, соответствующей столбцу `i`
     */
    private fun hungarianAlgorithm(n: Int, m: Int, a: Array<IntArray>): IntArray {
        val u = IntArray(n + 1)
        val v = IntArray(m + 1)
        val p = IntArray(m + 1)
        val way = IntArray(m + 1)

        for (i in 1..n) {
            p[0] = i
            var j0 = 0
            val minv = IntArray(m + 1) { INF }
            val used = BooleanArray(m + 1) { false }

            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = INF
                var j1 = 0

                for (j in 1..m) {
                    if (!used[j]) {
                        val current = a[i0][j] - u[i0] - v[j]
                        if (current < minv[j]) {
                            minv[j] = current
                            way[j] = j0
                        }
                        if (minv[j] < delta) {
                            delta = minv[j]
                            j1 = j
                        }
                    }
                }

                for (j in 0..m) {
                    if (used[j]) {
                        u[p[j]] += delta
                        v[j] -= delta
                    } else {
                        minv[j] -= delta
                    }
                }
                j0 = j1
            } while (p[j0] != 0)

            do {
                val j1 = way[j0]
                p[j0] = p[j1]
                j0 = j1
            } while (j0 != 0)
        }

        return p
    }

}
