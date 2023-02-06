package ru.yandex.autotests.innerpochta.ignores;

/**
 * Данный интерфейс реализуется классами, которые наследуются от {@link org.junit.runners.model.Statement} и
 * представляют из себя "игнорирующие" или "фильтрующие" рулы.
 * Нужна для того, чтобы различного рода игнорирующие/фильтрующие рулы не обрабатывали уже ненужный тест.
 *
 * @author pavponn
 */
public interface IgnoreStatement {
}
