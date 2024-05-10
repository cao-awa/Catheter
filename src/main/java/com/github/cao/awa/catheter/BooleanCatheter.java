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

public class BooleanCatheter {
    private static final Random RANDOM = new Random();
    private boolean[] targets;

    public BooleanCatheter(boolean[] targets) {
        this.targets = targets;
    }

    public static BooleanCatheter make(boolean... targets) {
        return new BooleanCatheter(targets);
    }

    public static BooleanCatheter makeCapacity(int size) {
        return new BooleanCatheter(array(size));
    }

    public static <X> BooleanCatheter of(boolean[] targets) {
        return new BooleanCatheter(targets);
    }

    public static BooleanCatheter of(Set<Boolean> targets) {
        boolean[] delegate = new boolean[targets.size()];
        int index = 0;
        for (boolean target : targets) {
            delegate[index++] = target;
        }
        return new BooleanCatheter(delegate);
    }

    public static BooleanCatheter of(List<Boolean> targets) {
        boolean[] delegate = new boolean[targets.size()];
        int index = 0;
        for (boolean target : targets) {
            delegate[index++] = target;
        }
        return new BooleanCatheter(delegate);
    }

    public BooleanCatheter each(final Consumer<Boolean> action) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        return this;
    }

    public BooleanCatheter each(final Consumer<Boolean> action, Runnable poster) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(ts[index++]);
        }
        poster.run();
        return this;
    }

    public <X> BooleanCatheter each(X initializer, final BiConsumer<X, Boolean> action) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        return this;
    }

    public <X> BooleanCatheter each(X initializer, final BiConsumer<X, Boolean> action, Consumer<X> poster) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public <X> BooleanCatheter overall(X initializer, final TriConsumer<X, Integer, Boolean> action) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        return this;
    }

    public <X> BooleanCatheter overall(X initializer, final TriConsumer<X, Integer, Boolean> action, Consumer<X> poster) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(initializer, index, ts[index++]);
        }
        poster.accept(initializer);
        return this;
    }

    public BooleanCatheter overall(final BiConsumer<Integer, Boolean> action) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        return this;
    }

    public BooleanCatheter overall(final BiConsumer<Integer, Boolean> action, Runnable poster) {
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            action.accept(index, ts[index++]);
        }
        poster.run();
        return this;
    }

    public BooleanCatheter insert(final TriFunction<Integer, Boolean, Boolean, Boolean> maker) {
        final Map<Integer, Pair<Integer, Boolean>> indexes = new HashMap<>();
        final Receptacle<Boolean> lastItem = new Receptacle<>(null);
        overall((index, item) -> {
            Boolean result = maker.apply(index, item, lastItem.get());
            if (result != null) {
                indexes.put(index + indexes.size(), new Pair<>(index, result));
            }
            lastItem.set(item);
        });

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(ts.length + indexes.size());
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
                    final Pair<Integer, Boolean> item = indexes.get(index);
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

    public BooleanCatheter pluck(final TriFunction<Integer, Boolean, Boolean, Boolean> maker) {
        final Receptacle<Boolean> lastItem = new Receptacle<>(null);
        return overallFilter((index, item) -> {
            final Boolean pluck = maker.apply(index, item, lastItem.get());
            if (pluck != null && pluck) {
                return false;
            }
            lastItem.set(item);
            return true;
        });
    }

    public BooleanCatheter filter(final Predicate<Boolean> predicate) {
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
    public BooleanCatheter overallFilter(final BiPredicate<Integer, Boolean> predicate) {
        // 创建需要的变量和常量
        final boolean[] ts = this.targets;
        final int length = ts.length;
        final boolean[] deleting = array(length);
        int newDelegateSize = length;
        int index = 0;

        // 遍历所有元素
        while (index < length) {
            boolean target = ts[index];

            // 符合条件的保留
            if (predicate.test(index, target)) {
                index++;
                continue;
            }

            // 不符合条件的设为null，后面会去掉
            // 并且将新数组的容量减一
            deleting[index++] = true;
            newDelegateSize--;
        }

        // 创建新数组
        final boolean[] newDelegate = array(newDelegateSize);
        int newDelegateIndex = 0;
        index = 0;

        // 遍历所有元素
        while (index < length) {
            // deleting 值为 true 则为被筛选掉的，忽略
            if (deleting[index]) {
                continue;
            }

            final boolean t = ts[index++];

            // 不为 true 则加入新数组
            newDelegate[newDelegateIndex++] = t;
        }

        // 替换当前数组，不要创建新Catheter对象以节省性能
        this.targets = newDelegate;

        return this;
    }

    public BooleanCatheter overallFilter(final boolean initializer, final TriFunction<Integer, Boolean, Boolean, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.apply(index, item, initializer));
    }

    public BooleanCatheter filter(final boolean initializer, final BiPredicate<Boolean, Boolean> predicate) {
        return overallFilter((index, item) -> predicate.test(item, initializer));
    }

    public BooleanCatheter orFilter(final boolean succeed, final Predicate<Boolean> predicate) {
        if (succeed) {
            return this;
        }
        return filter(predicate);
    }

    public BooleanCatheter orFilter(final boolean succeed, final boolean initializer, final BiPredicate<Boolean, Boolean> predicate) {
        if (succeed) {
            return this;
        }
        return filter(initializer, predicate);
    }

    public BooleanCatheter distinct() {
        final Map<Boolean, Boolean> map = new HashMap<>();
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

    public BooleanCatheter sort(Comparator<Boolean> comparator) {
        Boolean[] array = new Boolean[this.targets.length];
        int index = 0;
        for (boolean target : this.targets) {
            array[index++] = target;
        }
        Arrays.sort(array, comparator);
        index = 0;
        for (boolean target : array) {
            this.targets[index++] = target;
        }
        return this;
    }

    public BooleanCatheter holdTill(int index) {
        index = Math.min(index, this.targets.length);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(index);
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

    public BooleanCatheter holdTill(final Predicate<Boolean> predicate) {
        final int index = findTill(predicate);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(index);
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

    public BooleanCatheter whenFlock(final Boolean source, final BiFunction<Boolean, Boolean, Boolean> maker, Consumer<Boolean> consumer) {
        consumer.accept(flock(source, maker));
        return this;
    }

    public BooleanCatheter whenFlock(BiFunction<Boolean, Boolean, Boolean> maker, Consumer<Boolean> consumer) {
        consumer.accept(flock(maker));
        return this;
    }

    public boolean flock(final boolean source, final BiFunction<Boolean, Boolean, Boolean> maker) {
        boolean result = source;
        final boolean[] ts = this.targets;
        int index = 0;
        final int length = ts.length;
        while (index < length) {
            result = maker.apply(result, ts[index++]);
        }
        return result;
    }

    public boolean flock(final BiFunction<Boolean, Boolean, Boolean> maker) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        boolean result = length > 0 && ts[0];
        for (int i = 1; i < length; i++) {
            result = maker.apply(result, ts[i]);
        }
        return result;
    }

    public BooleanCatheter waiveTill(final int index) {
        final boolean[] ts = this.targets;
        final boolean[] newDelegate;
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

    public BooleanCatheter waiveTill(final Predicate<Boolean> predicate) {
        final int index = findTill(predicate);

        final boolean[] ts = this.targets;
        final boolean[] newDelegate;
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

    public BooleanCatheter till(final Predicate<Boolean> predicate) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return this;
    }

    public int findTill(final Predicate<Boolean> predicate) {
        final boolean[] ts = this.targets;
        int index = 0, length = ts.length;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                break;
            }
        }

        return index;
    }

    public BooleanCatheter replace(final Function<Boolean, Boolean> handler) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            ts[index] = handler.apply(ts[index++]);
        }
        return this;
    }

    public <X> Catheter<X> vary(final Function<Boolean, X> handler) {
        final boolean[] ts = this.targets;
        final X[] array = xArray(ts.length);
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            array[index] = handler.apply(ts[index++]);
        }
        return new Catheter<>(array);
    }

    public BooleanCatheter whenAny(final Predicate<Boolean> predicate, final Consumer<Boolean> action) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final boolean t = ts[index++];
            if (predicate.test(t)) {
                action.accept(t);
                break;
            }
        }
        return this;
    }

    public BooleanCatheter whenAll(final Predicate<Boolean> predicate, final Runnable action) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final boolean t = ts[index++];
            if (!predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public BooleanCatheter whenAll(final Predicate<Boolean> predicate, final Consumer<Boolean> action) {
        return whenAll(predicate, () -> each(action));
    }

    private BooleanCatheter whenNone(final Predicate<Boolean> predicate, final Runnable action) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            final boolean t = ts[index++];
            if (predicate.test(t)) {
                return this;
            }
        }
        action.run();
        return this;
    }

    public boolean hasAny(final Predicate<Boolean> predicate) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAll(final Predicate<Boolean> predicate) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (!predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNone(final Predicate<Boolean> predicate) {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        int index = 0;
        while (index < length) {
            if (predicate.test(ts[index++])) {
                return false;
            }
        }
        return true;
    }

    public BooleanCatheter any(final Consumer<Boolean> consumer) {
        if (this.targets.length > 0) {
            boolean[] ls = this.targets;
            int index = RANDOM.nextInt(ls.length);
            consumer.accept(ls.length > index ? ls[index] : ls[ls.length - 1]);
        }
        return this;
    }

    public BooleanCatheter first(final Consumer<Boolean> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[0]);
        }
        return this;
    }

    public BooleanCatheter tail(final Consumer<Boolean> consumer) {
        if (this.targets.length > 0) {
            consumer.accept(this.targets[this.targets.length - 1]);
        }
        return this;
    }

    public BooleanCatheter reverse() {
        final boolean[] ts = this.targets;
        final int length = ts.length;
        final int split = length / 2;
        int index = 0;
        boolean temp;
        for (; index < split; index++) {
            final int swapIndex = length - index - 1;
            temp = ts[index];
            ts[index] = ts[swapIndex];
            ts[swapIndex] = temp;
        }
        return this;
    }

    public boolean max(final Comparator<Boolean> comparator) {
        return flock((result, element) -> comparator.compare(result, element) < 0 ? element : result);
    }

    public boolean min(final Comparator<Boolean> comparator) {
        return flock((result, element) -> comparator.compare(result, element) > 0 ? element : result);
    }

    public BooleanCatheter whenMax(final Comparator<Boolean> comparator, final Consumer<Boolean> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) < 0 ? element : result));
        return this;
    }

    public BooleanCatheter whenMin(final Comparator<Boolean> comparator, final Consumer<Boolean> action) {
        action.accept(flock((result, element) -> comparator.compare(result, element) > 0 ? element : result));
        return this;
    }

    private BooleanCatheter exists() {
        return filter(Objects::nonNull);
    }

    public int count() {
        return this.targets.length;
    }

    public BooleanCatheter count(final AtomicInteger target) {
        target.set(count());
        return this;
    }

    public BooleanCatheter count(final Receptacle<Integer> target) {
        target.set(count());
        return this;
    }

    public BooleanCatheter count(final Consumer<Integer> consumer) {
        consumer.accept(count());
        return this;
    }

    @SafeVarargs
    public final BooleanCatheter append(final boolean... objects) {
        final boolean[] ts = this.targets;
        final boolean[] newDelegate = array(ts.length + objects.length);
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

    public BooleanCatheter append(final BooleanCatheter objects) {
        return append(objects.array());
    }

    public BooleanCatheter repeat(final int count) {
        final boolean[] ts = array();
        for (int i = 0; i < count; i++) {
            append(ts);
        }
        return this;
    }

    public boolean fetch(int index) {
        return this.targets[Math.min(index, this.targets.length - 1)];
    }

    public void fetch(int index, boolean item) {
        this.targets[index] = item;
    }

    public BooleanCatheter matrixEach(final int width, final BiConsumer<MatrixPos, Boolean> action) {
        return matrixReplace(width, (pos, item) -> {
            action.accept(pos, item);
            return item;
        });
    }

    public <X> Catheter<X> matrixHomoVary(final int width, BooleanCatheter input, final TriFunction<MatrixPos, Boolean, Boolean, X> action) {
        if (input.count() == count()) {
            final Receptacle<Integer> index = new Receptacle<>(0);
            return matrixVary(width, (pos, item) -> {
                final int indexValue = index.get();

                final boolean inputX = input.fetch(indexValue);
                final X result = action.apply(pos, item, inputX);

                index.set(indexValue + 1);

                return result;
            });
        }

        throw new IllegalArgumentException("The matrix is not homogeneous matrix");
    }

    public BooleanCatheter matrixMap(
            final int width,
            final int inputWidth,
            final BooleanCatheter input,
            final QuinFunction<MatrixFlockPos, MatrixPos, MatrixPos, Boolean, Boolean, Boolean> scanFlocked,
            final TriFunction<MatrixPos, Boolean, Boolean, Boolean> combineFlocked
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
        final BooleanCatheter newMatrix = BooleanCatheter.makeCapacity(homoMatrix ? sourceHeight * width : sourceHeight * inputWidth);

        // 后续需要使用 flock 累加 flocks 的数据
        final BooleanCatheter flockingCatheter = BooleanCatheter.makeCapacity(width);

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
                final boolean fetchedSource = fetch(posY * width + sourceX);

                // 获取输入的值
                final boolean fetchedInput = input.fetch(inputY * inputWidth + posX);

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

    public <X> Catheter<X> matrixVary(final int width, boolean input, final TriFunction<MatrixPos, Boolean, Boolean, X> action) {
        return matrixVary(width, (pos, item) -> action.apply(pos, item, input));
    }

    public BooleanCatheter matrixReplace(final int width, final BiFunction<MatrixPos, Boolean, Boolean> action) {
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

    public <X> Catheter<X> matrixVary(final int width, final BiFunction<MatrixPos, Boolean, X> action) {
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

    public Catheter<BooleanCatheter> matrixLines(final int width) {
        if (!(count() > 0 && count() % width == 0)) {
            throw new IllegalArgumentException("The elements does not is a matrix");
        }

        final int sourceHeight = count() / width;
        Catheter<BooleanCatheter> results = Catheter.makeCapacity(sourceHeight);
        BooleanCatheter catheter = BooleanCatheter.makeCapacity(width);
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < width; x++) {
                final boolean element = fetch(y * width + x);
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

    public BooleanCatheter shuffle() {
        sort((t1, t2) -> RANDOM.nextInt());
        return this;
    }

    public BooleanCatheter dump() {
        return new BooleanCatheter(array());
    }

    public BooleanCatheter reset() {
        this.targets = array(0);
        return this;
    }

    public boolean[] array() {
        return this.targets.clone();
    }

    public List<Boolean> list() {
        List<Boolean> list = new ArrayList<>();
        for (boolean l : array()) {
            list.add(l);
        }
        return list;
    }

    public Set<Boolean> set() {
        Set<Boolean> set = new HashSet<>();
        for (boolean l : array()) {
            set.add(l);
        }
        return set;
    }

    public static void main(String[] args) {
        BooleanCatheter source = BooleanCatheter.make(
                true, false, true,
                false, true, false,
                true, false, true
        );
        BooleanCatheter input = BooleanCatheter.make(
                true, true, true,
                false, false, false,
                true, true, true
        );

        source.dump()
                .matrixHomoVary(3, input, (pos, sourceX, inputX) -> {
                    return (sourceX ? 10 : 5) + (inputX ? 20 : 15);
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });

        System.out.println("------");

        source.matrixMap(3, 3, input, (flockPos, sourcePos, inputPos, sourceX, inputX) -> {
                    return sourceX || inputX;
                }, (destPos, combine1, combine2) -> {
                    return combine1 && combine2;
                })
                .matrixEach(3, (pos, item) -> {
                    System.out.println(pos);
                    System.out.println(item);
                });


    }

    private static boolean[] array(int size) {
        return new boolean[size];
    }

    @SuppressWarnings("unchecked")
    private static <X> X[] xArray(int size) {
        return (X[]) new Object[size];
    }
}
