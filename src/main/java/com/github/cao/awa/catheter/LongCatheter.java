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

public class LongCatheter {
    private static final Random RANDOM = new Random();
    private long[] targets;

    public LongCatheter(long[] targets) {
        this.targets = targets;
    }

    public static LongCatheter make(long... targets) {
        return new LongCatheter(targets);
    }

    public static LongCatheter makeCapacity(int size) {
        return new LongCatheter(array(size));
    }

    public static <X> LongCatheter of(long[] targets) {
        return new LongCatheter(targets);
    }

    public static LongCatheter of(Collection<Long> targets) {
        if (targets == null) {
            return new LongCatheter(array(0));
        }
        long[] delegate = new long[targets.size()];
        int index = 0;
        for (long target : targets) {
            delegate[index++] = target;
        }
        return new LongCatheter(delegate);
    }

    public LongCatheter each(final Consumer<Long> action) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public LongCatheter each(final Consumer<Long> action, Runnable poster) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> LongCatheter each(X initializer, final BiConsumer<X, Long> action) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> LongCatheter each(X initializer, final BiConsumer<X, Long> action, Consumer<X> poster) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> LongCatheter overall(X initializer, final TriConsumer<X, Integer, Long> action) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> LongCatheter overall(X initializer, final TriConsumer<X, Integer, Long> action, Consumer<X> poster) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public LongCatheter overall(final BiConsumer<Integer, Long> action) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public LongCatheter overall(final BiConsumer<Integer, Long> action, Runnable poster) {
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public LongCatheter insert(final TriFunction<Integer, Long, Long, Long> maker) {
        final Map<Integer, Pair<Integer, Long>> indexes = new HashMap<>();
        final Receptacle<Long> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            Long result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final long[] ts = this.targets;
        final long[] newDelegate = array(ts.length + indexes.size());
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
                    final Pair<Integer, Long> item = indexes.get(index);
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

    public LongCatheter pluck(final TriFunction<Integer, Long, Long, Boolean> maker) {
        final Receptacle<Long> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public LongCatheter discard(final Predicate<Long> predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public LongCatheter discard(final long initializer, final BiPredicate<Long, Long> predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public LongCatheter orDiscard(final boolean succeed, final Predicate<Long> predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public LongCatheter orDiscard(final boolean succeed, final long initializer, final BiPredicate<Long, Long> predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public LongCatheter filter(final Predicate<Long> predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public LongCatheter filter(final long initializer, final BiPredicate<Long, Long> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public LongCatheter orFilter(final boolean succeed, final Predicate<Long> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public LongCatheter orFilter(final boolean succeed, final long initializer, final BiPredicate<Long, Long> predicate) {
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
    public LongCatheter overallFilter(final BiPredicate<Integer, Long> predicate) {
        // 创建需要的变量和常量
        final long[] ts = this.targets;
        final int length = ts.length;
        final long[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            long target = ts[index];

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
        final long[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            // deleting 值为1则为被筛选掉的，忽略
            if (deleting[index] == 1) {
                index++;
                continue;
            }

            final long t = ts[index++];

            // 不为1则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public LongCatheter overallFilter(final long initializer, final TriFunction<Integer, Long, Long, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public LongCatheter ifPresent(Consumer<LongCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public LongCatheter ifEmpty(Consumer<LongCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public LongCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        long[] newDelegate = array(count() - 1);
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

    public LongCatheter distinct() {
        final Map<Long, Boolean> map = new HashMap<>();
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

    public LongCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public LongCatheter sort(Comparator<Long> comparator) {
        Long[] array = new Long[this.targets.length];
        int index = 0;
        for (long target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (long target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public LongCatheter holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final long[] ts = this.targets;
        final long[] newDelegate = array(index);
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

    public LongCatheter holdTill(final Predicate<Long> predicate) {
        final int index = findTill(predicate);

        final long[] ts = this.targets;
        final long[] newDelegate = array(index);
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

    public LongCatheter whenFlock(final Long source, final BiFunction<Long, Long, Long> maker, Consumer<Long> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public LongCatheter whenFlock(BiFunction<Long, Long, Long> maker, Consumer<Long> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public <X> X alternate(final X source, final BiFunction<X, Long, X> maker) {
        X result = source;
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public <X> LongCatheter whenAlternate(final X source, final BiFunction<X, Long, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> LongCatheter whenAlternate(BiFunction<X, Long, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public long flock(final long source, final BiFunction<Long, Long, Long> maker) {
        long result = source;
        final long[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public long flock(final BiFunction<Long, Long, Long> maker) {
        final long[] ts = this.targets;
        final int length = ts.length;
        long result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public LongCatheter waiveTill(final int index) {
        final long[] ts = this.targets;
        final long[] newDelegate;
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

    public LongCatheter waiveTill(final Predicate<Long> predicate) {
        final int index = findTill(predicate);

        final long[] ts = this.targets;
        final long[] newDelegate;
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

    public LongCatheter till(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public LongCatheter replace(final Function<Long, Long> handler) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<Long, X> handler) {
        final long[] ts = this.targets;
        final X[] array = xArray(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public LongCatheter whenAny(final Predicate<Long> predicate, final Consumer<Long> action) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final long t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public LongCatheter whenAll(final Predicate<Long> predicate, final Runnable action) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final long t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public LongCatheter whenAll(final Predicate<Long> predicate, final Consumer<Long> action) {
        return whenAll(predicate, () -> each(action));
    }

    private LongCatheter whenNone(final Predicate<Long> predicate, final Runnable action) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final long t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public long findFirst(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final long t = ts[index++];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public long findLast(final Predicate<Long> predicate) {
        final long[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final long t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final Predicate<Long> predicate, Function<Long, X> function) {
        final long[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final long t = ts[index++];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final Predicate<Long> predicate, Function<Long, X> function) {
        final long[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final long t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public LongCatheter any(final Consumer<Long> consumer) {
        if (this.targets.length > 0) {
            long[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public LongCatheter first(final Consumer<Long> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public LongCatheter tail(final Consumer<Long> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public LongCatheter reverse() {
        final long[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        long temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public long max(final Comparator<Long> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public long min(final Comparator<Long> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public LongCatheter whenMax(final Comparator<Long> comparator, final Consumer<Long> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public LongCatheter whenMin(final Comparator<Long> comparator, final Consumer<Long> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    private LongCatheter exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public LongCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public LongCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public LongCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final LongCatheter append(final long... objects) {
        final long[] ts = this.targets;
        final long[] newDelegate = array(ts.length + objects.length);
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

    public LongCatheter append(final LongCatheter objects) {
        return append(objects.array());
    }

    public LongCatheter repeat(final int count) {
        final long[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public long fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, long item) {
        this.targets[index] = item;
    }

    public LongCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Long> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, LongCatheter input, final TriFunction<MatrixPos, Long, Long, X> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final long inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public LongCatheter matrixMap(
            final int width,
            final int inputWidth,
            final LongCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Long, Long, Long> scanFlocked,
            final TriFunction<MatrixPos, Long, Long, Long> combineFlocked
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
        final LongCatheter newMatrix = LongCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final LongCatheter flockingCatheter = LongCatheter.makeCapacity(width);

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
                final long fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final long fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, long input, final TriFunction<MatrixPos, Long, Long, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public LongCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Long, Long> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Long, X> action) {
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

    public Catheter<LongCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<LongCatheter> results = Catheter.makeCapacity(sourceHeight);
        LongCatheter catheter = LongCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final long element = fetch(y * width + x);
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

    public LongCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public LongCatheter dump() {
        return new LongCatheter(array());
    }

    public LongCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public long[] array() {
        return this.targets.clone();
    }

    public long[] dArray() {
        return this.targets;
    }

    public List<Long> list() {
        List<Long> list = new ArrayList<>();
        for (long l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Long> set() {
        Set<Long> set = new HashSet<>();
        for (long l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        LongCatheter source = LongCatheter.make(
                1, 2, 3, 4, 5, 6, 7, 8
        );

        System.out.println("???");

        System.out.println(source.removeWithIndex(4).list());

        System.out.println("???");
    }

    private static long[] array(int size) {
        return new long[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
