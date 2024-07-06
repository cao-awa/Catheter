# Example
```Catheter``` is a stated streaming data solution.

```java
Catheter<Integer> catheter = Catheter.make(
    0, 1, 2, 3, 3, 3, 4, 5, 6
);

String concat = catheter.filter(element -> {
    return element > 0;
})
.distinct()
.filter(element -> {
    return element < 6;
})
.replace(element -> {
    return element + 2;
})
.vary(element -> {
    return String.valueOf(element);
})
.flock("", (result, element) -> {
    return result + element;
});
```

This example removed duplicate element by distinct and filtered elements bigger than 0 and smaller than 6, then got {1, 2, 3, 4 ,5}.

And it replace the numbers be increased 2 of itself, then got {3, 4, 5, 6, 7}.

And it vary to a new catheter that typed ```String``` got {"3", "4", "5", "6", "7"}.

Finally it flock to a ```String``` value is "34567", for details of ```flock``` and other actions please see the "Actions" chapter in this markdown file.

# Make catheter
## With delegate
Make a catheter with a delegate:

```java
List<X> theList = ......;
Catheter<X> catheter = Catheter.of(theList);
```

the delegate ```List<X>``` has supported to all ```Collection<X>``` and ```X[]```

## With present
Make a catheter with a present:
```java
Catheter<String> catheter = Catheter.make(
    "a", "b", "c", "d"
);
```

# Actions
## each
The action ```each``` with simplest calling is:

```java
catheter.each(element -> {
    // ......
});
```

It will consumes all elements in the catheter, includes null element.

In addition, ```each``` action also able to:

### With poster code

```java
catheter.each(element -> {
    // ......
}, () -> {
    // The poster code.
});
```

The poster code will be called when end of the elements consumes.

### With initializer

```java
catheter.each("xxx", (initializer, element) -> {
    // The "initializer" always is "xxx" (from args input)
});
```

Make a temporary variable be named initializer.

### With initializer and poster code

```java
catheter.each("xxx", (initializer, element) -> {
    // The "initializer" always is "xxx" (from args input)
}, (initializer) -> {
    // The poster code.
});
```

This ```each``` action just combined the previous two behaviors.

## overall
The action ```overall``` with simplest calling is:

```java
catheter.each((index, element) -> {
    // ......
});
```

It will consumes all elements in the catheter with index, includes null element.

It is all behavior same to ```each``` action, different only indexed.

In addition, ```overall``` action also able to:

### With poster code

```java
catheter.overall((index, element) -> {
    // ......
}, () -> {
    // The poster code.
});
```

The poster code will be called when end of the elements consumes.

### With initializer

```java
catheter.overall("xxx", (index, initializer, element) -> {
    // The "initializer" always is "xxx" (from args input)
});
```

Make a temporary variable be named initializer.

### With initializer and poster code

```java
catheter.overall("xxx", (index, initializer, element) -> {
    // The "initializer" always is "xxx" (from args input)
}, (initializer) -> {
    // The poster code.
});
```

This ```overall``` action just combined the previous two behaviors.

## flock

The action ```flock``` can input an initializer that type sames to the element type of the catheter.

Then this action will consumes all element and update the initializer input.

```java
int acc = catheter.flock(1, (result, element) -> {
    // The "result" will be changes every time calculating.
    return result + element;
});
```

In this example, if catheter have elements {1, 2, 3}, then ```acc``` will be 7.

Because initializer is 1, it sum to the first element 1 then changed to 2,

When it sum to second element, it changed to 2+2 that is 4,

Finally, it sum to third element, then it changed to 4+3 that is 7.

### Without initializer

The action ```flock``` also can missing the initializer, it will create the initializer after first calculate.

This usage may causes NullPointerException.

```java
int acc = catheter.flock((result, element) -> {
    // The "result" will be changes every time calculating.
    if (result == null) {
        return element;
    }
    return result + element;
});
```

In this example, if catheter have elements {1, 2, 3}, then ```acc``` will be 6.

Because this example doesn't has extra initializer 1, it just sums up all numbers in the catheter.

## alternate

The action ```alternate``` can input an initializer that type not sames to the element type of the catheter.

Then this action will consumes all element and update the initializer input.

```java
String concat = catheter.alternate("0", (result, element) -> {
    // The "result" will be changes every time calculating.
    return result + element;
});
```

In this example, if catheter have elements {1, 2, 3}, then ```concat``` will be "0123".

Because initializer is "0", it concat to the first element "1" then changed to "01",

When it concat to second element, it changed to "01"+"2" that is "012",

Finally, it concat to third element, then it changed to "012"+"3" that is "0123".

The action ```alternate``` must assign an initializer because it need inference the result type by method signature.

## replace

The action ```replace``` will consumes all elements in the catheter, calculate result and replaced old value.

```java
catheter.replace(element -> {
    return element + 1;
});
```

In this example, if catheter have elements {1, 2, 3}, then this catheter will be {2, 3, 4} after replace.

## vary

The action ```vary``` will consumes all elements in the catheter, calculate result and make a new catheter.


```java
catheter.replace(element -> {
    return String.valueOf(element);
});
```

In this example, if catheter have elements {1, 2, 3}, then got a new catheter as {"1", "2", "3"} after replace.

## shuffle

Random shuffle the catheter.

```java
catheter.shuffle();
```

## dump

Copy this catheter to a new catheter.

```java
Catheter<X> newCatheter = catheter.dump();
```

## reset

Clear this catheter.


```java
catheter.reset();
```

## exists

Removes null element in the catheter.

```java
catheter.exists();

// It equivalent to
catheter.filter(Objects::nonNull);
```

## reverse

Reverse the elements array in the catheter.

```java
catheter.reverse();
```

In this example, if catheter have elements {1, 2, 3}, then this catheter will be {3, 2, 1} after reverse.

## sort

Sort the elements in the catheter.

```java
catheter.sort();
```

It can also use Comparator<X> to sort elements.

```java
catheter.sort(Collections.reverseOrder());
```

## distinct

Removes duplicated elements in the catheter.

```java
catheter.distinct();
```

## Collect
Theres action can collect the catheter to ```Collection<X>``` and ```X[]```.

### list

```java
List<X> list = catheter.list();
```
### set

```java
Set<X> set = catheter.set();
```
### array

```java
X[] array = catheter.array();
```

### dump

Same to what previous just said, it copy the catheter to a new catheter.

```java
Catheter<X> catheter = catheter.dump();
```

## Count
Theres action can count the catheter eleme.

### Directly count

```java
int count = catheter.count();
```

### Count to AtomicInteger

```java
AtomicInteger i = new AtomicInteger();
catheter.count(i);
```

### Count to Consumer

```java
Consumer<Integer> consumer = count -> {
    // The consumer codes.
};
catheter.count(consumer);

// 或者
catheter.count(count -> {
    // The consumer codes.
});
```

### Count to Receptacle

```java
Receptacle<Integer> r = new Receptacle<>();
catheter.count(r);
```
