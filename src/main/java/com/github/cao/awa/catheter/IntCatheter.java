package com.github.cao.awa.catheter;

import com.github.cao.awa.catheter.matrix.MatrixFlockPos;
import com.github.cao.awa.catheter.matrix.MatrixPos;
import com.github.cao.awa.catheter.pair.Pair;
import com.github.cao.awa.catheter.receptacle.Receptacle;
import com.github.cao.awa.sinuatum.function.consumer.TriConsumer;
import com.github.cao.awa.sinuatum.function.function.QuinFunction;
import com.github.cao.awa.sinuatum.function.function.TriFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

public class IntCatheter {
    private static final Random RANDOM = new Random();
    private int[] targets;

    public IntCatheter(int[] targets) {
        this.targets = targets;
    }

    public static IntCatheter make(int... targets) {
        return new IntCatheter(targets);
    }

    public static IntCatheter makeCapacity(int size) {
        return new IntCatheter(array(size));
    }

    public static <X> IntCatheter of(int[] targets) {
        return new IntCatheter(targets);
    }

    public static IntCatheter of(Collection<Integer> targets) {
        if (targets == null) {
            return new IntCatheter(array(0));
        }
        int[] delegate = new int[targets.size()];
        int index = 0;
        for (int target : targets) {
            delegate[index++] = target;
        }
        return new IntCatheter(delegate);
    }

    public IntCatheter each(final Consumer<Integer> action) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public IntCatheter each(final Consumer<Integer> action, Runnable poster) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> IntCatheter each(X initializer, final BiConsumer<X, Integer> action) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> IntCatheter each(X initializer, final BiConsumer<X, Integer> action, Consumer<X> poster) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> IntCatheter overall(X initializer, final TriConsumer<X, Integer, Integer> action) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> IntCatheter overall(X initializer, final TriConsumer<X, Integer, Integer> action, Consumer<X> poster) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public IntCatheter overall(final BiConsumer<Integer, Integer> action) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public IntCatheter overall(final BiConsumer<Integer, Integer> action, Runnable poster) {
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public IntCatheter insert(final TriFunction<Integer, Integer, Integer, Integer> maker) {
        final Map<Integer, Pair<Integer, Integer>> indexes = new HashMap<>();
        final Receptacle<Integer> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            Integer result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final int[] ts = this.targets;
        final int[] newDelegate = array(ts.length + indexes.size());
        final Receptacle<Integer> lastIndex = new Receptacle<>(0);
        final Receptacle<Integer> lastDest = new Receptacle<>(0);
        Catheter.of(indexes.keySet())
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
                    final Pair<Integer, Integer> item = indexes.get(index);
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

    public IntCatheter pluck(final TriFunction<Integer, Integer, Integer, Boolean> maker) {
        final Receptacle<Integer> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public IntCatheter discard(final Predicate<Integer> predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public IntCatheter discard(final int initializer, final BiPredicate<Integer, Integer> predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public IntCatheter orDiscard(final boolean succeed, final Predicate<Integer> predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public IntCatheter orDiscard(final boolean succeed, final int initializer, final BiPredicate<Integer, Integer> predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public IntCatheter filter(final Predicate<Integer> predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public IntCatheter filter(final int initializer, final BiPredicate<Integer, Integer> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public IntCatheter orFilter(final boolean succeed, final Predicate<Integer> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public IntCatheter orFilter(final boolean succeed, final int initializer, final BiPredicate<Integer, Integer> predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    /**
     * Holding items that matched given predicate.
     *
     * @param predicate The filter predicate
     * @return This {@code Catheter<T>}
     * @author 草
     * @since 1.0.0
     */
    public IntCatheter overallFilter(final BiPredicate<Integer, Integer> predicate) {
        // 创建需要的变量和常量
        final int[] ts = this.targets;
        final int length = ts.length;
        final int[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            int target = ts[index];

            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的设为null，后面会去掉
            // 并且将新数组的容量减一
            deleting[index++] = 1;
            newDelegateSize--;
        }

        // 创建新数组
        final int[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            // deleting 值为1则为被筛选掉的，忽略
            if (deleting[index] == 1) {
                index++;
                continue;
            }

            final int t = ts[index++];

            // 不为1则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public IntCatheter overallFilter(final int initializer, final TriFunction<Integer, Integer, Integer, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public IntCatheter ifPresent(Consumer<IntCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public IntCatheter ifEmpty(Consumer<IntCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public IntCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        int[] newDelegate = array(count() - 1);
        if (index > 0) {
            System.arraycopy(
                    this.targets,
                    0,
                    newDelegate,
                    0,
                    index
            );
        }

        System.arraycopy(
                this.targets,
                index + 1,
                newDelegate,
                index,
                count() - 1 - index
        );

        this.targets = newDelegate;

        return this;
    }

    public IntCatheter distinct() {
        final Map<Integer, Boolean> map = new HashMap<>();
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

    public IntCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public IntCatheter sort(Comparator<Integer> comparator) {
        Integer[] array = new Integer[this.targets.length];
        int index = 0;
        for (int target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (int target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public IntCatheter holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final int[] ts = this.targets;
        final int[] newDelegate = array(index);
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

    public IntCatheter holdTill(final Predicate<Integer> predicate) {
        final int index = findTill(predicate);

        final int[] ts = this.targets;
        final int[] newDelegate = array(index);
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

    public IntCatheter whenFlock(final Integer source, final BiFunction<Integer, Integer, Integer> maker, Consumer<Integer> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public IntCatheter whenFlock(BiFunction<Integer, Integer, Integer> maker, Consumer<Integer> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public <X> X alternate(final X source, final BiFunction<X, Integer, X> maker) {
        X result = source;
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public <X> IntCatheter whenAlternate(final X source, final BiFunction<X, Integer, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> IntCatheter whenAlternate(BiFunction<X, Integer, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public int flock(final int source, final BiFunction<Integer, Integer, Integer> maker) {
        int result = source;
        final int[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public int flock(final BiFunction<Integer, Integer, Integer> maker) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public IntCatheter waiveTill(final int index) {
        final int[] ts = this.targets;
        final int[] newDelegate;
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

    public IntCatheter waiveTill(final Predicate<Integer> predicate) {
        final int index = findTill(predicate);

        final int[] ts = this.targets;
        final int[] newDelegate;
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

    public IntCatheter till(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public IntCatheter replace(final Function<Integer, Integer> handler) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<Integer, X> handler) {
        final int[] ts = this.targets;
        final X[] array = xArray(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public IntCatheter whenAny(final Predicate<Integer> predicate, final Consumer<Integer> action) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final int t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public IntCatheter whenAll(final Predicate<Integer> predicate, final Runnable action) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final int t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public IntCatheter whenAll(final Predicate<Integer> predicate, final Consumer<Integer> action) {
        return whenAll(predicate, () -> each(action));
    }

    private IntCatheter whenNone(final Predicate<Integer> predicate, final Runnable action) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final int t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public int findFirst(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final int t = ts[index++];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public int findLast(final Predicate<Integer> predicate) {
        final int[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final int t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final Predicate<Integer> predicate, Function<Integer, X> function) {
        final int[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final int t = ts[index++];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final Predicate<Integer> predicate, Function<Integer, X> function) {
        final int[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final int t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public IntCatheter any(final Consumer<Integer> consumer) {
        if (this.targets.length > 0) {
            int[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public IntCatheter first(final Consumer<Integer> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public IntCatheter tail(final Consumer<Integer> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public IntCatheter reverse() {
        final int[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        int temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public int max(final Comparator<Integer> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public int min(final Comparator<Integer> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public IntCatheter whenMax(final Comparator<Integer> comparator, final Consumer<Integer> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public IntCatheter whenMin(final Comparator<Integer> comparator, final Consumer<Integer> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    private IntCatheter exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public IntCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public IntCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public IntCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final IntCatheter append(final int... objects) {
        final int[] ts = this.targets;
        final int[] newDelegate = array(ts.length + objects.length);
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

    public IntCatheter append(final IntCatheter objects) {
        return append(objects.array());
    }

    public IntCatheter repeat(final int count) {
        final int[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public int fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, int item) {
        this.targets[index] = item;
    }

    public IntCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Integer> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, IntCatheter input, final TriFunction<MatrixPos, Integer, Integer, X> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final int inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public IntCatheter matrixMap(
            final int width,
            final int inputWidth,
            final IntCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Integer, Integer, Integer> scanFlocked,
            final TriFunction<MatrixPos, Integer, Integer, Integer> combineFlocked
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
        final IntCatheter newMatrix = IntCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final IntCatheter flockingCatheter = IntCatheter.makeCapacity(width);

        return newMatrix.matrixReplace(inputWidth, (pos, ignored) -> {
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
                final int fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final int fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, int input, final TriFunction<MatrixPos, Integer, Integer, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public IntCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Integer, Integer> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Integer, X> action) {
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

    public Catheter<IntCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<IntCatheter> results = Catheter.makeCapacity(sourceHeight);
        IntCatheter catheter = IntCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final int element = fetch(y * width + x);
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

    public IntCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public IntCatheter dump() {
        return new IntCatheter(array());
    }

    public IntCatheter flat(Function<Integer, IntCatheter> function) {
        Catheter<IntCatheter> catheter = Catheter.makeCapacity(count());
        Receptacle<Integer> totalSize = new Receptacle<>(0);
        alternate(0, (index, element) -> {
            IntCatheter flatting = function.apply(element);
            catheter.fetch(index, flatting);
            totalSize.set(totalSize.get() + flatting.count());
            return index + 1;
        });

        this.targets = array(totalSize.get());
        catheter.alternate(0, (currentIndex, inner) -> {
            return inner.alternate(currentIndex, (index, element) -> {
                fetch(index, element);
                return index + 1;
            });
        });
        return this;
    }

    public <X> Catheter<X> flatTo(Function<Integer, Catheter<X>> function) {
        Catheter<Catheter<X>> catheter = Catheter.makeCapacity(count());
        Receptacle<Integer> totalSize = new Receptacle<>(0);
        alternate(0, (index, element) -> {
            Catheter<X> flatting = function.apply(element);
            catheter.fetch(index, flatting);
            totalSize.set(totalSize.get() + flatting.count());
            return index + 1;
        });

        Catheter<X> result = Catheter.makeCapacity(totalSize.get());

        catheter.alternate(0, (currentIndex, inner) -> {
            return inner.alternate(currentIndex, (index, element) -> {
                result.fetch(index, element);
                return index + 1;
            });
        });
        return result;
    }

    public IntCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public int[] array() {
        return this.targets.clone();
    }

    public int[] dArray() {
        return this.targets;
    }

    public List<Integer> list() {
        List<Integer> list = new ArrayList<>();
        for (int l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Integer> set() {
        Set<Integer> set = new HashSet<>();
        for (int l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        IntCatheter source = IntCatheter.make(
                1, 2, 3, 4, 5, 6, 7, 8
        );

        System.out.println("???");

        System.out.println(source.removeWithIndex(4).list());

        System.out.println("???");
    }

    private static int[] array(int size) {
        return new int[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
