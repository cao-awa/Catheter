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

public class DoubleCatheter {
    private static final Random RANDOM = new Random();
    private double[] targets;

    public DoubleCatheter(double[] targets) {
        this.targets = targets;
    }

    public static DoubleCatheter make(double... targets) {
        return new DoubleCatheter(targets);
    }

    public static DoubleCatheter makeCapacity(int size) {
        return new DoubleCatheter(array(size));
    }

    public static <X> DoubleCatheter of(double[] targets) {
        return new DoubleCatheter(targets);
    }

    public static DoubleCatheter of(Collection<Double> targets) {
        if (targets == null) {
            return new DoubleCatheter(array(0));
        }
        double[] delegate = new double[targets.size()];
        int index = 0;
        for (double target : targets) {
            delegate[index++] = target;
        }
        return new DoubleCatheter(delegate);
    }

    public DoubleCatheter each(final Consumer<Double> action) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public DoubleCatheter each(final Consumer<Double> action, Runnable poster) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> DoubleCatheter each(X initializer, final BiConsumer<X, Double> action) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> DoubleCatheter each(X initializer, final BiConsumer<X, Double> action, Consumer<X> poster) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> DoubleCatheter overall(X initializer, final TriConsumer<X, Integer, Double> action) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> DoubleCatheter overall(X initializer, final TriConsumer<X, Integer, Double> action, Consumer<X> poster) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public DoubleCatheter overall(final BiConsumer<Integer, Double> action) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public DoubleCatheter overall(final BiConsumer<Integer, Double> action, Runnable poster) {
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public DoubleCatheter insert(final TriFunction<Integer, Double, Double, Double> maker) {
        final Map<Integer, Pair<Integer, Double>> indexes = new HashMap<>();
        final Receptacle<Double> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            Double result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final double[] ts = this.targets;
        final double[] newDelegate = array(ts.length + indexes.size());
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
                    final Pair<Integer, Double> item = indexes.get(index);
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

    public DoubleCatheter pluck(final TriFunction<Integer, Double, Double, Boolean> maker) {
        final Receptacle<Double> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public DoubleCatheter discard(final Predicate<Double> predicate) {
        return overallFilter((index, item) -> !predicate.test(item));
    }

    public DoubleCatheter discard(final double initializer, final BiPredicate<Double, Double> predicate) {
        return overallFilter((index, item) -> !predicate.test(item, initializer));
    }

    public DoubleCatheter orDiscard(final boolean succeed, final Predicate<Double> predicate) {
        if (succeed) {
            return this;
        }
        return discard(predicate);
    }

    public DoubleCatheter orDiscard(final boolean succeed, final double initializer, final BiPredicate<Double, Double> predicate) {
        if (succeed) {
            return this;
        }
        return discard(initializer, predicate);
    }

    public DoubleCatheter filter(final Predicate<Double> predicate) {
        return overallFilter((index, item) -> predicate.test(item));
    }

    public DoubleCatheter filter(final double initializer, final BiPredicate<Double, Double> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public DoubleCatheter orFilter(final boolean succeed, final Predicate<Double> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public DoubleCatheter orFilter(final boolean succeed, final double initializer, final BiPredicate<Double, Double> predicate) {
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
    public DoubleCatheter overallFilter(final BiPredicate<Integer, Double> predicate) {
        // 创建需要的变量和常量
        final double[] ts = this.targets;
        final int length = ts.length;
        final double[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            double target = ts[index];

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
        final double[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            // deleting 值为1则为被筛选掉的，忽略
            if (deleting[index] == 1) {
                index++;
                continue;
            }

            final double t = ts[index++];

            // 不为1则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public DoubleCatheter overallFilter(final double initializer, final TriFunction<Integer, Double, Double, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    public DoubleCatheter distinct() {
        final Map<Double, Boolean> map = new HashMap<>();
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

    public DoubleCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public DoubleCatheter sort(Comparator<Double> comparator) {
        Double[] array = new Double[this.targets.length];
        int index = 0;
        for (double target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (double target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public DoubleCatheter holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final double[] ts = this.targets;
        final double[] newDelegate = array(index);
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

    public DoubleCatheter holdTill(final Predicate<Double> predicate) {
        final int index = findTill(predicate);

        final double[] ts = this.targets;
        final double[] newDelegate = array(index);
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

    public DoubleCatheter whenFlock(final Double source, final BiFunction<Double, Double, Double> maker, Consumer<Double> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public DoubleCatheter whenFlock(BiFunction<Double, Double, Double> maker, Consumer<Double> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public <X> X alternate(final X source, final BiFunction<X, Double, X> maker) {
        X result = source;
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public double flock(final double source, final BiFunction<Double, Double, Double> maker) {
        double result = source;
        final double[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public double flock(final BiFunction<Double, Double, Double> maker) {
        final double[] ts = this.targets;
        final int length = ts.length;
        double result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public DoubleCatheter waiveTill(final int index) {
        final double[] ts = this.targets;
        final double[] newDelegate;
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

    public DoubleCatheter waiveTill(final Predicate<Double> predicate) {
        final int index = findTill(predicate);

        final double[] ts = this.targets;
        final double[] newDelegate;
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

    public DoubleCatheter till(final Predicate<Double> predicate) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<Double> predicate) {
        final double[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public DoubleCatheter replace(final Function<Double, Double> handler) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<Double, X> handler) {
        final double[] ts = this.targets;
        final X[] array = xArray(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public DoubleCatheter whenAny(final Predicate<Double> predicate, final Consumer<Double> action) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final double t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public DoubleCatheter whenAll(final Predicate<Double> predicate, final Runnable action) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final double t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public DoubleCatheter whenAll(final Predicate<Double> predicate, final Consumer<Double> action) {
        return whenAll(predicate, () -> each(action));
    }

    private DoubleCatheter whenNone(final Predicate<Double> predicate, final Runnable action) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final double t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<Double> predicate) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<Double> predicate) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<Double> predicate) {
        final double[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public DoubleCatheter any(final Consumer<Double> consumer) {
        if (this.targets.length > 0) {
            double[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public DoubleCatheter first(final Consumer<Double> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public DoubleCatheter tail(final Consumer<Double> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public DoubleCatheter reverse() {
        final double[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        double temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public double max(final Comparator<Double> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public double min(final Comparator<Double> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public DoubleCatheter whenMax(final Comparator<Double> comparator, final Consumer<Double> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public DoubleCatheter whenMin(final Comparator<Double> comparator, final Consumer<Double> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    private DoubleCatheter exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public DoubleCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public DoubleCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public DoubleCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final DoubleCatheter append(final double... objects) {
        final double[] ts = this.targets;
        final double[] newDelegate = array(ts.length + objects.length);
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

    public DoubleCatheter append(final DoubleCatheter objects) {
        return append(objects.array());
    }

    public DoubleCatheter repeat(final int count) {
        final double[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public double fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, double item) {
        this.targets[index] = item;
    }

    public DoubleCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Double> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, DoubleCatheter input, final TriFunction<MatrixPos, Double, Double, X> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final double inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public DoubleCatheter matrixMap(
            final int width,
            final int inputWidth,
            final DoubleCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Double, Double, Double> scanFlocked,
            final TriFunction<MatrixPos, Double, Double, Double> combineFlocked
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
        final DoubleCatheter newMatrix = DoubleCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final DoubleCatheter flockingCatheter = DoubleCatheter.makeCapacity(width);

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
                final double fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final double fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, double input, final TriFunction<MatrixPos, Double, Double, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public DoubleCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Double, Double> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Double, X> action) {
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

    public Catheter<DoubleCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<DoubleCatheter> results = Catheter.makeCapacity(sourceHeight);
        DoubleCatheter catheter = DoubleCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final double element = fetch(y * width + x);
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

    public DoubleCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        double[] newDelegate = array(count() - 1);
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

    public DoubleCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public boolean isPresent() {
        return count() > 0;
    }

    public DoubleCatheter ifPresent(Consumer<DoubleCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public DoubleCatheter ifEmpty(Consumer<DoubleCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public DoubleCatheter dump() {
        return new DoubleCatheter(array());
    }

    public DoubleCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public double[] array() {
        return this.targets.clone();
    }

    public List<Double> list() {
        List<Double> list = new ArrayList<>();
        for (double l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Double> set() {
        Set<Double> set = new HashSet<>();
        for (double l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        DoubleCatheter source = DoubleCatheter.make(
                3, 3, 3,
                4, 1, 1,
                5, 9, 9
        );
        DoubleCatheter input = DoubleCatheter.make(
                1, 0, 0,
                0, 1, 0,
                0, 0, 1
        );

        source.dump()
                .matrixHomoVary(3, input, (pos, sourceX, inputX) -> {
                    return sourceX - inputX;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });

        System.out.println("------");

        source.matrixMap(3, 3, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
                    return sourceX * inputX;
                }, (destPos, combine1, combine2) -> {
                    return combine1 + combine2;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });


    }

    private static double[] array(int size) {
        return new double[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
