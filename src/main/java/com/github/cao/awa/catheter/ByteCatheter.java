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

public class ByteCatheter {
    private static final Random RANDOM = new Random();
    private byte[] targets;

    public ByteCatheter(byte[] targets) {
        this.targets = targets;
    }

    public static ByteCatheter make(byte... targets) {
        return new ByteCatheter(targets);
    }

    public static ByteCatheter makeCapacity(int size) {
        return new ByteCatheter(array(size));
    }

    public static <X> ByteCatheter of(byte[] targets) {
        return new ByteCatheter(targets);
    }

    public static ByteCatheter of(Collection<Byte> targets) {
        if (targets == null) {
            return new ByteCatheter(array(0));
        }
        byte[] delegate = new byte[targets.size()];
        int index = 0;
        for (byte target : targets) {
            delegate[index++] = target;
        }
        return new ByteCatheter(delegate);
    }

    public ByteCatheter each(final Consumer<Byte> action) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public ByteCatheter each(final Consumer<Byte> action, Runnable poster) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> ByteCatheter each(X initializer, final BiConsumer<X, Byte> action) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> ByteCatheter each(X initializer, final BiConsumer<X, Byte> action, Consumer<X> poster) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> ByteCatheter overall(X initializer, final TriConsumer<X, Integer, Byte> action) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> ByteCatheter overall(X initializer, final TriConsumer<X, Integer, Byte> action, Consumer<X> poster) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public ByteCatheter overall(final BiConsumer<Integer, Byte> action) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public ByteCatheter overall(final BiConsumer<Integer, Byte> action, Runnable poster) {
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public ByteCatheter insert(final TriFunction<Integer, Byte, Byte, Byte> maker) {
        final Map<Integer, Pair<Integer, Byte>> indexes = new HashMap<>();
        final Receptacle<Byte> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            Byte result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(ts.length + indexes.size());
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
                    final Pair<Integer, Byte> item = indexes.get(index);
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

    public ByteCatheter pluck(final TriFunction<Integer, Byte, Byte, Boolean> maker) {
        final Receptacle<Byte> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public ByteCatheter filter(final Predicate<Byte> predicate) {
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
    public ByteCatheter overallFilter(final BiPredicate<Integer, Byte> predicate) {
        // 创建需要的变量和常量
        final byte[] ts = this.targets;
        final int length = ts.length;
        final byte[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            byte target = ts[index];

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
        final byte[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            // deleting 值为1则为被筛选掉的，忽略
            if (deleting[index] == 1) {
                index++;
                continue;
            }

            final byte t = ts[index++];

            // 不为1则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public ByteCatheter overallFilter(final byte initializer, final TriFunction<Integer, Byte, Byte, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    public ByteCatheter filter(final byte initializer, final BiPredicate<Byte, Byte> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public ByteCatheter orFilter(final boolean succeed, final Predicate<Byte> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public ByteCatheter orFilter(final boolean succeed, final byte initializer, final BiPredicate<Byte, Byte> predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public ByteCatheter distinct() {
        final Map<Byte, Boolean> map = new HashMap<>();
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

    public ByteCatheter removeWithIndex(int index) {
        if (isEmpty() || index >= count() || index < 0) {
            return this;
        }

        byte[] newDelegate = array(count() - 1);
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


    public boolean isPresent() {
        return count() > 0;
    }

    public ByteCatheter ifPresent(Consumer<ByteCatheter> action) {
        if (count() > 0) {
            action.accept(this);
        }
        return this;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    public ByteCatheter ifEmpty(Consumer<ByteCatheter> action) {
        if (count() == 0) {
            action.accept(this);
        }
        return this;
    }

    public ByteCatheter sort() {
        Arrays.sort(this.targets);
        return this;
    }

    public ByteCatheter sort(Comparator<Byte> comparator) {
        Byte[] array = new Byte[this.targets.length];
        int index = 0;
        for (byte target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (byte target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public ByteCatheter holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(index);
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

    public ByteCatheter holdTill(final Predicate<Byte> predicate) {
        final int index = findTill(predicate);

        final byte[] ts = this.targets;
        final byte[] newDelegate = array(index);
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

    public ByteCatheter whenFlock(final Byte source, final BiFunction<Byte, Byte, Byte> maker, Consumer<Byte> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public ByteCatheter whenFlock(BiFunction<Byte, Byte, Byte> maker, Consumer<Byte> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public <X> X alternate(final X source, final BiFunction<X, Byte, X> maker) {
        X result = source;
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public <X> ByteCatheter whenAlternate(final X source, final BiFunction<X, Byte, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(source, maker));
        return this;
    }

    public <X> ByteCatheter whenAlternate(BiFunction<X, Byte, X> maker, Consumer<X> consumer) {
        consumer.accept(alternate(null, maker));
        return this;
    }

    public byte flock(final byte source, final BiFunction<Byte, Byte, Byte> maker) {
        byte result = source;
        final byte[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public byte flock(final BiFunction<Byte, Byte, Byte> maker) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        byte result = length > 0 ? ts[0] : 0;
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public ByteCatheter waiveTill(final int index) {
        final byte[] ts = this.targets;
        final byte[] newDelegate;
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

    public ByteCatheter waiveTill(final Predicate<Byte> predicate) {
        final int index = findTill(predicate);

        final byte[] ts = this.targets;
        final byte[] newDelegate;
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

    public ByteCatheter till(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public ByteCatheter replace(final Function<Byte, Byte> handler) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<Byte, X> handler) {
        final byte[] ts = this.targets;
        final X[] array = xArray(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public ByteCatheter whenAny(final Predicate<Byte> predicate, final Consumer<Byte> action) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final byte t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public ByteCatheter whenAll(final Predicate<Byte> predicate, final Runnable action) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final byte t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public ByteCatheter whenAll(final Predicate<Byte> predicate, final Consumer<Byte> action) {
        return whenAll(predicate, () -> each(action));
    }

    private ByteCatheter whenNone(final Predicate<Byte> predicate, final Runnable action) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final byte t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public byte findFirst(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final byte t = ts[index++];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public byte findLast(final Predicate<Byte> predicate) {
        final byte[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final byte t = ts[index--];
            if (predicate.test(t)) {
                return t;
            }
        }
        return 0;
    }

    public <X> X whenFoundFirst(final Predicate<Byte> predicate, Function<Byte, X> function) {
        final byte[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final byte t = ts[index++];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public <X> X whenFoundLast(final Predicate<Byte> predicate, Function<Byte, X> function) {
        final byte[] ts = this.targets;
        int index = ts.length - 1;
        while (index > -1) {
            final byte t = ts[index--];
            if (predicate.test(t)) {
                return function.apply(t);
            }
        }
        return null;
    }

    public ByteCatheter any(final Consumer<Byte> consumer) {
        if (this.targets.length > 0) {
            byte[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public ByteCatheter first(final Consumer<Byte> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public ByteCatheter tail(final Consumer<Byte> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public ByteCatheter reverse() {
        final byte[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        byte temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public byte max(final Comparator<Byte> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public byte min(final Comparator<Byte> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public ByteCatheter whenMax(final Comparator<Byte> comparator, final Consumer<Byte> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public ByteCatheter whenMin(final Comparator<Byte> comparator, final Consumer<Byte> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    private ByteCatheter exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public ByteCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public ByteCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public ByteCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final ByteCatheter append(final byte... objects) {
        final byte[] ts = this.targets;
        final byte[] newDelegate = array(ts.length + objects.length);
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

    public ByteCatheter append(final ByteCatheter objects) {
        return append(objects.array());
    }

    public ByteCatheter repeat(final int count) {
        final byte[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public byte fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, byte item) {
        this.targets[index] = item;
    }

    public ByteCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Byte> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, ByteCatheter input, final TriFunction<MatrixPos, Byte, Byte, X> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final byte inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public ByteCatheter matrixMap(
            final int width,
            final int inputWidth,
            final ByteCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Byte, Byte, Byte> scanFlocked,
            final TriFunction<MatrixPos, Byte, Byte, Byte> combineFlocked
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
        final ByteCatheter newMatrix = ByteCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final ByteCatheter flockingCatheter = ByteCatheter.makeCapacity(width);

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
                final byte fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final byte fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, byte input, final TriFunction<MatrixPos, Byte, Byte, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public ByteCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Byte, Byte> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Byte, X> action) {
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

    public Catheter<ByteCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<ByteCatheter> results = Catheter.makeCapacity(sourceHeight);
        ByteCatheter catheter = ByteCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final byte element = fetch(y * width + x);
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

    public ByteCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public ByteCatheter dump() {
        return new ByteCatheter(array());
    }

    public ByteCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public byte[] array() {
        return this.targets.clone();
    }

    public byte[] dArray() {
        return this.targets;
    }

    public List<Byte> list() {
        List<Byte> list = new ArrayList<>();
        for (byte l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Byte> set() {
        Set<Byte> set = new HashSet<>();
        for (byte l : array()) {
            set.add(l);
        }
        return set;
    }

    private static byte[] array(int size) {
        return new byte[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
