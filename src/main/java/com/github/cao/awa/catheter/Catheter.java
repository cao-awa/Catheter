package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.Pair;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.function.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class Catheter<T> {
    private static final Random RANDOM = new Random();
    private T[] targets;

    public Catheter(T[] targets) {
        this.targets = targets;
    }

    @SafeVarargs
    public static <X> Catheter<X> make(X... targets) {
        return new Catheter<>(targets);
    }

    public static <X> Catheter<X> makeCapacity(int size) {
        return new Catheter<>(array(size));
    }

    public static <X> Catheter<X> of(X[] targets) {
        return new Catheter<>(targets);
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> of(Set<X> targets) {
        return new Catheter<>((X[]) targets.toArray(Object[]::new));
    }

    @SuppressWarnings("unchecked")
    public static <X> Catheter<X> of(List<X> targets) {
        return new Catheter<>((X[]) targets.toArray(Object[]::new));
    }

    public Catheter<T> each(final Consumer<T> action) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public Catheter<T> each(final Consumer<T> action, Runnable poster) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> Catheter<T> each(X initializer, final BiConsumer<X, T> action) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> Catheter<T> each(X initializer, final BiConsumer<X, T> action, Consumer<X> poster) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> Catheter<T> overall(X initializer, final TriConsumer<X, Integer, T> action) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> Catheter<T> overall(X initializer, final TriConsumer<X, Integer, T> action, Consumer<X> poster) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public Catheter<T> overall(final BiConsumer<Integer, T> action) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public Catheter<T> overall(final BiConsumer<Integer, T> action, Runnable poster) {
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public Catheter<T> insert(final TriFunction<Integer, T, T, T> maker) {
        final Map<Integer, Pair<Integer, T>> indexes = new HashMap<>();
        final Receptacle<T> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            T result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final T[] ts = this.targets;
        final T[] newDelegate = array(ts.length + indexes.size());
        final Receptacle<Integer> lastIndex = new Receptacle<>(0);
        final Receptacle<Integer> lastDest = new Receptacle<>(0);
        of(indexes.keySet())
                .sort()
                .each(index -> {
                    if (lastIndex.get().intValue() != index) {
                        final int maxCopyLength = Math.min(
                                newDelegate.length - lastDest.get() - 1,
                                index - lastIndex.get()
                        );
                        System.arraycopy(
                                ts,
                                lastIndex.get(),
                                newDelegate,
                                lastDest.get(),
                                maxCopyLength
                        );
                    }
                    final Pair<Integer, T> item = indexes.get(index);
                    newDelegate[index] = item.second();
                    lastIndex.set(item.first());
                    lastDest.set(index + 1);
                }, () -> {
                    System.arraycopy(
                            ts,
                            lastIndex.get(),
                            newDelegate,
                            lastDest.get(),
                            newDelegate.length - lastDest.get()
                    );
                });

        this.targets = newDelegate;

        return this;
    }

    public Catheter<T> pluck(final TriFunction<Integer, T, T, Boolean> maker) {
        final Receptacle<T> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public Catheter<T> filter(final Predicate<T> predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> overallFilter(final BiPredicate<Integer, T> predicate) {
        // 创建需要的变量和常量
        final T[] ts = this.targets;
        final int length = ts.length;
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            T target = ts[index];

            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的设为null，后面会去掉
            // 并且将新数组的容量减一
            ts[index++] = null;
            newDelegateSize--;
        }

        // 创建新数组
        final T[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            final T t = ts[index++];

            // 为null则为被筛选掉的，忽略
            if (t == null) {
                continue;
            }

            // 不为null则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> overallFilter(final X initializer, final TriFunction<Integer, T, X, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> filter(final X initializer, final BiPredicate<T, X> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param succeed   Direct done filter? When succeed true, cancel filter instantly
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public Catheter<T> orFilter(final boolean succeed, final Predicate<T> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param succeed     Direct done filter? When succeed true, cancel filter instantly
     * @param initializer A constant to passed to next parameter
     * @param predicate   The filter predicate
     * @param <X>         Initializer constant
     * @return This {@code Catheter<T>}
     * @author 草
     * @author 草二号机
     * @since 1.0.0
     */
    public <X> Catheter<T> orFilter(final boolean succeed, final X initializer, final BiPredicate<T, X> predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public Catheter<T> distinct() {
        final Map<T, Boolean> map = new HashMap<>();
        return filter(
                item -> {
                    if (map.getOrDefault(item, false)) {
                        return false;
                    }
                    map.put(item, true);
                    return true;
                }
        );
    }

    public Catheter<T> sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public Catheter<T> sort(Comparator<T> comparator) {
        Arrays.sort(this.targets, comparator);
        return this;
    }

    public Catheter<T> holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final T[] ts = this.targets;
        final T[] newDelegate = array(index);
        if (index > 0) {
            System.arraycopy(
                    ts,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public Catheter<T> holdTill(final Predicate<T> predicate) {
        final int index = findTill(predicate);

        final T[] ts = this.targets;
        final T[] newDelegate = array(index);
        if (index > 0) {
            System.arraycopy(
                    ts,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public Catheter<T> whenFlock(final T source, final BiFunction<T, T, T> maker, Consumer<T> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public Catheter<T> whenFlock(BiFunction<T, T, T> maker, Consumer<T> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public T flock(final T source, final BiFunction<T, T, T> maker) {
        T result = source;
        final T[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    @Nullable
    public T flock(final BiFunction<T, T, T> maker) {
        final T[] ts = this.targets;
        final int length = ts.length;
        T result = length > 0 ? ts[0] : null;
        if (result != null) {
            for (int i = 1; i < length; i++) {
                result = maker.apply(result, ts[i]);
            }
        }
        return result;
    }

    public Catheter<T> waiveTill(final int index) {
        final T[] ts = this.targets;
        final T[] newDelegate;
        if (index >= ts.length) {
            newDelegate = array(0);
        } else {
            newDelegate = array(ts.length - index + 1);
            System.arraycopy(
                    ts,
                    index - 1,
                    newDelegate,
                    0,
                    newDelegate.length
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public Catheter<T> waiveTill(final Predicate<T> predicate) {
        final int index = findTill(predicate);

        final T[] ts = this.targets;
        final T[] newDelegate;
        if (index >= ts.length) {
            newDelegate = array(0);
        } else {
            newDelegate = array(ts.length - index + 1);
            System.arraycopy(
                    ts,
                    index - 1,
                    newDelegate,
                    0,
                    newDelegate.length
            );
        }
        this.targets = newDelegate;

        return this;
    }

    public Catheter<T> till(final Predicate<T> predicate) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<T> predicate) {
        final T[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public Catheter<T> replace(final Function<T, T> handler) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<T, X> handler) {
        final T[] ts = this.targets;
        final X[] array = array(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public Catheter<T> whenAny(final Predicate<T> predicate, final Consumer<T> action) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final T t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public Catheter<T> whenAll(final Predicate<T> predicate, final Runnable action) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final T t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public Catheter<T> whenAll(final Predicate<T> predicate, final Consumer<T> action) {
        return whenAll(predicate, () -> each(action));
    }

    private Catheter<T> whenNone(final Predicate<T> predicate, final Runnable action) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final T t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<T> predicate) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<T> predicate) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<T> predicate) {
        final T[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public Catheter<T> any(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(select(this.targets, RANDOM));
        }
        return this;
    }

    public Catheter<T> first(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public Catheter<T> tail(final Consumer<T> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public Catheter<T> reverse() {
        final T[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        T temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public T max(final Comparator<T> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public T min(final Comparator<T> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public Optional<T> selectMax(final Comparator<T> comparator) {
        return Optional.ofNullable(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
    }

    public Optional<T> selectMin(final Comparator<T> comparator) {
        return Optional.ofNullable(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
    }

    public Catheter<T> whenMax(final Comparator<T> comparator, final Consumer<T> action) {
        final T t = flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
        if (t != null) {
            action.accept(t);
        }
        return this;
    }

    public Catheter<T> whenMin(final Comparator<T> comparator, final Consumer<T> action) {
        final T t = flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
        if (t != null) {
            action.accept(t);
        }
        return this;
    }

    public Catheter<T> exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public Catheter<T> count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public Catheter<T> count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public Catheter<T> count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final Catheter<T> append(final T... objects) {
        final T[] ts = this.targets;
        final T[] newDelegate = array(ts.length + objects.length);
        System.arraycopy(
                ts,
                0,
                newDelegate,
                0,
                ts.length
        );
        System.arraycopy(
                objects,
                0,
                newDelegate,
                ts.length,
                objects.length
        );
        this.targets = newDelegate;
        return this;
    }

    public Catheter<T> append(final Catheter<T> objects) {
        return append(objects.array());
    }

    public Catheter<T> repeat(final int count) {
        final T[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public T fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, T item) {
        this.targets[index] = item;
    }

    public Catheter<T> matrixEach(final int width, final BiConsumer<MatrixPos, T> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X, Y> Catheter<Y> matrixHomoVary(final int width, Catheter<X> input, final TriFunction<MatrixPos, T, X, Y> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final X inputX = input.fetch(indexValue);
                final Y result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public <X, Y> Catheter<Y> matrixMap(
            final int width,
            final int inputWidth,
            final Catheter<X> input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, T, X, Y> scanFlocked,
            final TriFunction<MatrixPos, Y, Y, Y> combineFlocked
    ) {
        final int inputHeight = input.count() / inputWidth;
        final int sourceHeight = count() / width;

        boolean homoMatrix = inputHeight == sourceHeight && width == inputWidth;

        // 矩阵计算时 A(h, w) B(h, w) 中的 A(w) 必须等于 B(h)
        // 其中 h 是高度而 w 是宽度，因此自身的 width 必须等于输入的 height
        if (width != inputHeight && !homoMatrix) {
            throw new IllegalArgumentException("The matrix cannot be constructed because input height does not match to source width");
        }

        // 创建矩阵，大小是 A(h)B(w)
        final Catheter<T> newMatrix = Catheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final Catheter<Y> flockingCatheter = Catheter.makeCapacity(width);

        return newMatrix.matrixVary(inputWidth, (pos, ignored) -> {
            final int posX = pos.x();
            final int posY = pos.y();

            int flockingIndex = 0;
            int inputY = 0;
            int sourceX = 0;
            while (sourceX < width) {

                // 这些 pos 和计算无关，用于让使用者自定义判断在矩阵中如何变换数据的
                final MatrixFlockPos flockPos = new MatrixFlockPos(
                        posX,
                        posY
                );
                final MatrixPos inputPos = new MatrixPos(
                        posX,
                        inputY
                );
                final MatrixPos sourcePos = new MatrixPos(
                        sourceX,
                        posY
                );

                // 获取自身的值
                final T fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final X fetchedInput = input.fetch(inputY * inputWidth + posX);

                // 追加到 flock 组中
                flockingCatheter.fetch(
                        flockingIndex++,
                        scanFlocked.apply(
                                flockPos,
                                sourcePos,
                                inputPos,
                                fetchedSource,
                                fetchedInput
                        )
                );

                inputY++;
                sourceX++;
            }

            // 对矩阵的每个参数累加对应列的结果
            return flockingCatheter.flock((current, next) -> combineFlocked.apply(pos, current, next));
        });
    }

    public Catheter<T> matrixTranspose(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int height = count() / width;

        final T[] newDelegate = array(this.targets.length);
        int newDelegateIndex = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                newDelegate[newDelegateIndex++] = fetch(y * width + x);
            }
        }

        this.targets = newDelegate;

        return this;
    }

    public <X, Y> Catheter<Y> matrixVary(final int width, X input, final TriFunction<MatrixPos, T, X, Y> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public Catheter<T> matrixReplace(final int width, final BiFunction<MatrixPos, T, T> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final Receptacle<Integer> w = new Receptacle<>(0);
        final Receptacle<Integer> h = new Receptacle<>(0);

        final int matrixEdge = width - 1;

        return replace(item -> {
            final int wValue = w.get();
            final int hValue = h.get();

            if (wValue == matrixEdge) {
                w.set(0);
                h.set(hValue + 1);
            } else {
                w.set(wValue + 1);
            }
            return action.apply(new MatrixPos(wValue, hValue), item);
        });
    }

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, T, X> action) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final Receptacle<Integer> w = new Receptacle<>(0);
        final Receptacle<Integer> h = new Receptacle<>(0);

        final int matrixEdge = width - 1;

        return vary(item -> {
            final int hValue = h.get();
            final int wValue = w.get();

            if (wValue == matrixEdge) {
                w.set(0);
                h.set(hValue + 1);
            } else {
                w.set(wValue + 1);
            }
            return action.apply(new MatrixPos(wValue, hValue), item);
        });
    }

    public Catheter<Catheter<T>> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<Catheter<T>> results = Catheter.makeCapacity(sourceHeight);
        Catheter<T> catheter = Catheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final T element = fetch(y * width + x);
                catheter.fetch(
                        x,
                        element
                );
            }
            results.fetch(
                    y,
                    catheter.dump()
            );
        }

        return results;
    }

    public Catheter<T> shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public Catheter<T> dump() {
        return new Catheter<>(array());
    }

    public Catheter<T> reset() {
        this.targets = array(0);
        return this;
    }

    public T[] array() {
        return this.targets.clone();
    }

    public List<T> list() {
        return List.of(this.targets);
    }

    public Set<T> set() {
        return Set.of(this.targets);
    }

    public static void main(String[] args) {
        LongCatheter source = LongCatheter.make(
                1, 2, 3, 4,
                5, 6, 7, 8
        );
        LongCatheter input = LongCatheter.make(
                1, 5,
                2, 6,
                3, 7,
                4, 8
        );

        System.out.println("------");

        System.out.println("---Source---");
        source.matrixLines(4)
                .each(line -> {
                    line.each(new StringBuilder(), (builder, longValue) -> {
                        builder.append(longValue).append(", ");
                    }, builder -> {
                        System.out.println(builder.toString());
                    });
                });

        System.out.println("---Input---");
        input.matrixLines(2)
                .each(line -> {
                    line.each(new StringBuilder(), (builder, longValue) -> {
                        builder.append(longValue).append(", ");
                    }, builder -> {
                        System.out.println(builder.toString());
                    });
                });

        System.out.println("---Result---");
        source.matrixMap(4, 4, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
                    return sourceX * inputX;
                }, (destPos, combine1, combine2) -> {
                    return combine1 + combine2;
                })
                .matrixLines(4)
                .each(line -> {
                    line.each(new StringBuilder(), (builder, longValue) -> {
                        builder.append(longValue).append(", ");
                    }, builder -> {
                        System.out.println(builder.toString());
                    });
                });
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] array(int size) {
        return (X[]) new Object[size];
    }

    public static <T> T select(T[] array, int index) {
        return array.length > index ? array[index] : array[array.length - 1];
    }

    public static <T> T select(T[] array, Random random) {
        return array[random.nextInt(array.length)];
    }
}
